# PipelinePilot Companion (Jenkins plugin)

IDE-facing API plugin backing the **PipelinePilot for Jenkins** IntelliJ plugin.
Exposes a root action at `/ide/...` so the IDE can validate, run, replay, and
stream pipeline executions on a **real** Jenkins instance — no local emulation.

## Endpoints

| Method | Path | Body / Query | Returns |
|--------|------|--------------|---------|
| POST | `/ide/validate` | form `jenkinsfile` | `{valid, diagnostics[]}` |
| POST | `/ide/runSandbox` | JSON `RunSandboxRequest` | `{sessionId, accepted, message?}` |
| POST | `/ide/replay` | JSON `ReplayRequest` | `{sessionId, accepted, message?}` |
| GET | `/ide/logs` | `?sessionId=&start=N` | `{events[], nextStart, more}` (progressive) |
| POST | `/ide/session` | `?sessionId=` | 200 (destroy temp execution) |

JSON shapes mirror `common-api/Models.kt`.

## Security

- Mutating endpoints are `@RequirePOST` → CSRF crumb required (IDE fetches it via `/crumbIssuer`).
- Permission checks per endpoint: `Jenkins.READ` (validate/logs), `Item.CREATE` (run/replay), `Item.DELETE` (cleanup).
- Submitted scripts run under the **Groovy sandbox** (`CpsFlowDefinition(script, /*sandbox*/ true)`).
- Ephemeral jobs live under hidden folder `IDE-SANDBOX/<user>/` and are deleted on cleanup; the original pipeline config is never mutated.

## Build

Requires Maven + JDK 17.

```bash
cd jenkins-plugin
mvn -DskipTests package        # produces target/pipelinepilot.hpi
mvn hpi:run                    # run a dev Jenkins with the plugin at :8080/jenkins
```

## Integration points (skeleton TODOs)

- `doValidate` — delegate to the declarative linter (`pipeline-model-definition`) instead of the brace-balance placeholder.
- `doReplay` stage-scoped rerun — wire to `RestartDeclarativePipeline` / `replay` of `workflow-cps`.
- `doLogs` — emit `StageInfo` events from the flow graph (`FlowGraphTable`) for the IDE stage view, not just console lines.
