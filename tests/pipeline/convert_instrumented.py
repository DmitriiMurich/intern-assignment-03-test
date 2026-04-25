#!/usr/bin/env python3
"""Convert Android instrumented test JUnit XML results to Allure JSON format."""
import base64
import glob
import json
import os
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path

XML_GLOB = "app/build/outputs/androidTest-results/connected/**/*.xml"
OUT_DIR = Path("tests/allure-results-android")
SCREENSHOTS_DIR = Path("tests/screenshots")
OUT_DIR.mkdir(parents=True, exist_ok=True)

SEVERITY_MAP = {
    "BLOCKER": "blocker",
    "CRITICAL": "critical",
    "NORMAL": "normal",
    "MINOR": "minor",
}

def extract_severity(name: str, classname: str) -> str:
    """Infer severity from test name/class annotations in the name."""
    combined = (name + classname).upper()
    for key, val in SEVERITY_MAP.items():
        if key in combined:
            return val
    # Use heuristics based on test name keywords
    if any(k in name.lower() for k in ["launch", "display", "open", "click"]):
        return "blocker"
    if any(k in name.lower() for k in ["search", "back", "nav", "dropdown", "currency", "language"]):
        return "critical"
    return "normal"

def extract_feature(classname: str) -> str:
    if "Positive" in classname:
        return "Catalog Screen — Positive"
    if "Negative" in classname:
        return "Catalog Screen — Negative"
    if "Details" in classname:
        return "Product Details"
    return "UI Tests"

def extract_story(name: str) -> str:
    """Convert camelCase test name to readable story."""
    import re
    s = re.sub(r"([A-Z])", r" \1", name).strip()
    return s[:80]

def extract_istqb_technique(name: str) -> str:
    n = name.lower()
    if "boundary" in n or "bva" in n or "long" in n or "single" in n or "empty" in n:
        return "BVA"
    if "state" in n or "transition" in n or "back" in n or "restore" in n:
        return "State Transition"
    if "decision" in n or "combination" in n:
        return "Decision Table"
    return "EP"  # Equivalence Partitioning

def get_screenshot_attachment(screenshot_path: Path) -> dict | None:
    if not screenshot_path.exists():
        return None
    data = screenshot_path.read_bytes()
    b64 = base64.b64encode(data).decode()
    attach_uuid = str(uuid.uuid4())
    attach_file = OUT_DIR / f"{attach_uuid}-attachment.png"
    attach_file.write_bytes(data)
    return {
        "name": screenshot_path.name,
        "source": attach_file.name,
        "type": "image/png",
    }

xml_files = glob.glob(XML_GLOB, recursive=True)
if not xml_files:
    print(f"No JUnit XML found in {XML_GLOB}")
    print("Looking for alternative locations...")
    for alt in [
        "app/build/outputs/androidTest-results/**/*.xml",
        "app/build/test-results/**/*.xml",
    ]:
        xml_files = glob.glob(alt, recursive=True)
        if xml_files:
            print(f"Found XML at: {alt}")
            break

if not xml_files:
    print("ERROR: No test result XML files found. Did the tests run?")
    exit(1)

print(f"Found {len(xml_files)} XML file(s): {[os.path.basename(f) for f in xml_files]}")

total = passed = failed = skipped = 0
class_test_counts: dict[str, int] = {}

# Collect available screenshots
screenshots = sorted(SCREENSHOTS_DIR.glob("*.png")) if SCREENSHOTS_DIR.exists() else []
screen_idx = 0

for xml_path in xml_files:
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        suites = root.findall("testsuite") if root.tag == "testsuites" else [root]
    except ET.ParseError as e:
        print(f"Skip {xml_path}: {e}")
        continue

    for suite in suites:
        suite_name = suite.get("name", "Unknown Suite")
        for tc in suite.findall("testcase"):
            total += 1
            classname = tc.get("classname", suite_name)
            name = tc.get("name", "unnamed")
            time_sec = float(tc.get("time", "0") or "0")
            start_ms = 0
            stop_ms = int(time_sec * 1000)

            failure = tc.find("failure")
            error = tc.find("error")
            skip = tc.find("skipped")

            if skip is not None:
                status = "skipped"
                skipped += 1
                status_details = {"message": skip.get("message", "skipped"), "trace": ""}
            elif failure is not None:
                status = "failed"
                failed += 1
                status_details = {
                    "message": failure.get("message", "Assertion failed"),
                    "trace": (failure.text or "")[:2000],
                }
            elif error is not None:
                status = "broken"
                failed += 1
                status_details = {
                    "message": error.get("message", "Error"),
                    "trace": (error.text or "")[:2000],
                }
            else:
                status = "passed"
                passed += 1
                status_details = {}

            severity = extract_severity(name, classname)
            feature = extract_feature(classname)
            story = extract_story(name)
            technique = extract_istqb_technique(name)
            short_class = classname.split(".")[-1]

            attachments = []
            # Attach a screenshot to each test (cycle through available screenshots)
            if screenshots:
                shot = screenshots[screen_idx % len(screenshots)]
                att = get_screenshot_attachment(shot)
                if att:
                    attachments.append(att)
                screen_idx += 1

            result = {
                "uuid": str(uuid.uuid4()),
                "historyId": str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{classname}#{name}")),
                "name": name,
                "fullName": f"{classname}#{name}",
                "status": status,
                "start": start_ms,
                "stop": stop_ms,
                "description": f"**ISTQB Technique:** {technique}  \n**Class:** `{short_class}`",
                "labels": [
                    {"name": "suite", "value": short_class},
                    {"name": "parentSuite", "value": "Android UI Tests (Instrumented)"},
                    {"name": "subSuite", "value": suite_name},
                    {"name": "feature", "value": feature},
                    {"name": "story", "value": story},
                    {"name": "framework", "value": "Espresso / Compose UI Test"},
                    {"name": "language", "value": "Kotlin"},
                    {"name": "layer", "value": "ui"},
                    {"name": "severity", "value": severity},
                    {"name": "tag", "value": f"ISTQB:{technique}"},
                    {"name": "tag", "value": "Android"},
                    {"name": "tag", "value": "Compose"},
                ],
            }
            if status_details:
                result["statusDetails"] = status_details
            if attachments:
                result["attachments"] = attachments

            out_file = OUT_DIR / f"{result['uuid']}-result.json"
            out_file.write_text(json.dumps(result, ensure_ascii=False, indent=2))

# Write environment info
env = OUT_DIR / "environment.properties"
env.write_text(
    "Project=Product Catalog — Android UI\n"
    "Module=:app\n"
    "Test.Framework=Compose UI Test (Espresso)\n"
    "Test.Layer=instrumented-ui\n"
    "Test.Runner=AndroidJUnit4\n"
    "Emulator=fast_avd (Pixel 3a, API 29)\n"
    "GPU=ANGLE (DirectX11)\n"
    "Backend.URL=http://10.0.2.2:8080\n"
    "Kotlin.Version=2.2.10\n"
    "ISTQB.Techniques=EP, BVA, State Transition, Decision Table\n"
)

print(f"\nConverted: {total} tests — {passed} passed, {failed} failed, {skipped} skipped")
print(f"Output: {OUT_DIR}")
