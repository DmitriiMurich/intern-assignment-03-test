#!/usr/bin/env python3
"""Convert JUnit XML test results to Allure JSON format."""
import glob, json, os, uuid, xml.etree.ElementTree as ET
from pathlib import Path

XML_GLOB = "feature/build/test-results/**/*.xml"
OUT_DIR = Path("tests/allure-results-android")
OUT_DIR.mkdir(parents=True, exist_ok=True)

xml_files = glob.glob(XML_GLOB, recursive=True)
if not xml_files:
    print("No JUnit XML found — check build output")
    exit(1)

total = passed = failed = skipped = 0

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
                    "trace": failure.text or "",
                }
            elif error is not None:
                status = "broken"
                failed += 1
                status_details = {
                    "message": error.get("message", "Error"),
                    "trace": error.text or "",
                }
            else:
                status = "passed"
                passed += 1
                status_details = {}

            result = {
                "uuid": str(uuid.uuid4()),
                "historyId": str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{classname}#{name}")),
                "name": name,
                "fullName": f"{classname}#{name}",
                "status": status,
                "start": start_ms,
                "stop": stop_ms,
                "labels": [
                    {"name": "suite", "value": suite_name},
                    {"name": "parentSuite", "value": "Android / KMP"},
                    {"name": "feature", "value": "Android"},
                    {"name": "story", "value": classname.split(".")[-1]},
                    {"name": "framework", "value": "kotlin-test"},
                    {"name": "language", "value": "Kotlin"},
                    {"name": "layer", "value": "unit"},
                    {"name": "severity", "value": "normal"},
                ],
            }
            if status_details:
                result["statusDetails"] = status_details

            out_file = OUT_DIR / f"{result['uuid']}-result.json"
            out_file.write_text(json.dumps(result, ensure_ascii=False, indent=2))

print(f"Converted: {total} tests — {passed} passed, {failed} failed, {skipped} skipped")
