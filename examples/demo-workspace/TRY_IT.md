# PipelinePilot — try it in the sandbox IDE

The `runIde` window is a clean IntelliJ with the plugin installed.

## 0. Open this folder
File ▸ Open ▸ `examples/demo-workspace` (this directory).

## 1. Configure the connection
Settings ▸ Tools ▸ **PipelinePilot for Jenkins**
- Jenkins URL: `http://localhost:8080`
- Username: `admin`
- API token: generate at http://localhost:8080/user/admin/security/ ▸ "Add new token"
- Shared library roots: full path to `examples/demo-workspace/shared-lib`
- (optional) Enable AI + paste an Anthropic key

## 2. Validation
Open `Jenkinsfile`. Delete a `}` → save. Red gutter marker + inline error appears.
`Alt+Enter` on it → quick-fixes ("Add 'agent any'", "Suggest fix (AI)").
Undo. `Ctrl+Alt+V` → "successfully validated" balloon.

## 3. Shared library
- `@Library('platform-lib')` on line 1 is annotated.
- In the Deploy step, type `dep` → completion lists `deploy` (shared library step).
- `Ctrl+Click` on `deploy` → jumps to `shared-lib/vars/deploy.groovy`.

## 4. Run in sandbox + logs + stages
`Ctrl+Enter` (or right-click ▸ Run in Sandbox).
- PipelinePilot tool window opens (bottom).
- **Logs** tab streams the real Jenkins console.
- **Stages** tab draws the graph (parallel Quality Gates branches).
- **Sessions** tab lists the run; "Stop & Clean Up" available.

## 5. Replay
Stages tab ▸ "Replay Pipeline", or right-click a stage box ▸ "Replay stage …".

## 6. AI (if enabled)
Right-click ▸ "Analyze Pipeline (AI)" → recommendations stream into the Logs console.

> Requires the Docker Jenkins running: `docker compose up -d` (admin/admin).
