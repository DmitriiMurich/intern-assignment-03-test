---
description: Start the backend via Docker in WSL2, wait for health, then run the full test suite
---

Start the backend project and run all tests against it.

Steps:
1. Check if backend is already running: curl -sf http://localhost:8080/health
   - If yes: skip to step 4

2. Start Docker in WSL2 (sudo already configured passwordless):
   wsl -d Ubuntu -- bash -c "sudo dockerd > /tmp/dockerd.log 2>&1 & sleep 3 && sudo docker info > /dev/null 2>&1 && echo DAEMON_OK"
   
3. Start services:
   wsl -d Ubuntu -- bash -c "cd /mnt/c/Users/Dmitrii/projects/intern-assignment-03-test && test -f .env || cp .env.example .env && sudo docker compose up -d 2>&1 | tail -5"
   
   Then poll until healthy (max 60s):
   for i in 1..12: curl -sf http://localhost:8080/health && break || sleep 5

4. Install Python test dependencies if needed:
   pip show pytest > /dev/null || pip install -r tests/requirements-test.txt -q

5. Run full test suite:
   python -m pytest tests/ -v --tb=short

6. Write results to tests/pipeline/run-status.json

7. Show formatted pipeline report:
   - Backend status (running/offline)
   - Unit / API / Smoke results (passed/failed/skipped)
   - Total coverage %
