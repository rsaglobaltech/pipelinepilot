# PipelinePilot for Jenkins — Project Plan & Phase Tracker

> Living document. Tracks where we are across phases. Update the **Status** column as work progresses.

---

## 1. Vision

Professional IntelliJ IDEA plugin: **PipelinePilot for Jenkins**.

Goal: dramatically improve developer experience of creating, validating, testing, and debugging Jenkins Pipelines (`Jenkinsfile`, `.jenkinsfile`, Groovy shared libraries).

**Key principle:** Do NOT emulate Jenkins locally. Use a **remote-assisted development architecture**.

| IntelliJ IDEA provides | Jenkins server provides |
|------------------------|-------------------------|
| editing | real validation |
| validation UX | sandbox execution |
| inline diagnostics | plugin/runtime compatibility |
| execution controls | credentials |
| stage visualization | shared libraries |
| logs streaming | agents + actual execution env |

**Target UX loop:**

```
Edit Jenkinsfile → Ctrl+Enter → Run remotely in sandbox → See logs & stages instantly
```

Replaces the slow loop: `commit → push → wait → fail → repeat`.

---

## 2. Architecture

### 2.1 IntelliJ Plugin
- **Language:** Kotlin
- **Platform:** IntelliJ Platform Plugin SDK
- **Responsibilities:** editor integration, syntax highlighting, diagnostics, linter integration, execution UI, logs streaming, stage graph, sandbox execution controls

### 2.2 Jenkins Companion Plugin
- **Language:** Java/Kotlin
- **Responsibilities:** sandbox execution, temporary pipeline runs, validation APIs, replay orchestration, secure execution isolation, cleanup
- Exposes dedicated APIs for IDE integration

---

## 3. Core Features

| # | Feature | Summary |
|---|---------|---------|
| 1 | **Jenkinsfile Validation** | Validate syntax from IntelliJ. On save/Validate: detect Jenkinsfile → send to Jenkins validation endpoint → parse → inline diagnostics (gutter markers, clickable, navigable). Endpoints: `POST /pipeline-model-converter/validate` + custom companion endpoint. |
| 2 | **Remote Sandbox Execution** ⭐ MOST IMPORTANT | Create ephemeral remote executions. Flow: send Jenkinsfile → create temp execution → run on real Jenkins → stream logs → destroy temp execution. Supports declarative/scripted/shared libs/enterprise plugins/credentials/agents/K8s/Docker. Strategies: (A) temp folder `/IDE-SANDBOX/<user>/<session>`, (B) **Replay existing pipeline — preferred** (inject temp Jenkinsfile, never mutate original). |
| 3 | **Real-Time Logs Streaming** | Live console logs, stage transitions, timestamps, build status inside IntelliJ tool window. |
| 4 | **Stage Visualization** | Visual graph `[Checkout] → [Build] → [Test] → [Deploy]`. Supports parallel, nested, failed, skipped stages. |
| 5 | **Shared Library Awareness** | Support `@Library('shared-lib')`. Resolve symbols, navigate definitions, autocomplete `vars/`, understand library steps. |
| 6 | **Credentials Simulation Metadata** | NEVER expose credentials. Detect missing creds, display required credential IDs, optional mock local metadata. |
| 7 | **Pipeline Replay** | Rerun entire pipeline / failed stage / specific stage from IntelliJ UI. |
| 8 | **Intelligent Pipeline Parsing** | Groovy PSI + custom declarative parser. Support declarative/scripted/hybrid. |

---

## 4. IntelliJ UI Requirements

- **Tool Windows:** PipelinePilot, Logs, Stages, Sandbox Sessions
- **Editor Features:** syntax highlighting, inline diagnostics, quick fixes, autocomplete, navigation
- **Actions:** Validate Jenkinsfile, Run in Sandbox, Replay Pipeline, Stream Logs

---

## 5. Tech Stack

**IntelliJ Plugin (Kotlin):** IntelliJ PSI, ToolWindow APIs, Kotlin coroutines, OkHttp, Jackson
**Jenkins Plugin:** Jenkins Plugin SDK, Stapler, Jenkins Pipeline APIs

---

## 6. Backend API Contracts

```
POST   /ide/validate            { jenkinsfile, pipelineId, branch }
POST   /ide/runSandbox
GET    /ide/logs/{sessionId}
DELETE /ide/session/{sessionId}
```

---

## 7. Security (CRITICAL)

- Never expose Jenkins credentials
- Token-based auth
- Support SSO
- CSRF protection
- RBAC
- Enforce sandbox isolation

---

## 8. Enterprise Compatibility

Large Jenkins installs, thousands of jobs, shared libraries, Kubernetes plugin, Docker plugin, Vault, Artifactory, custom enterprise plugins.

## 9. Performance Goals

- Validation latency: **< 500ms**
- Logs streaming: near real-time
- UI: non-blocking, async everywhere

---

## 10. Project Structure

```
pipelinepilot/
 ├── intellij-plugin/
 ├── jenkins-plugin/
 ├── common-api/
 ├── docs/
 └── examples/
```

---

## 11. Constraints

**DO NOT:** full local Jenkins emulation · clone enterprise Jenkins locally · replicate plugins locally · replicate credentials locally
**DO:** rely on remote-assisted execution · use real Jenkins runtime · optimize dev workflow · prioritize enterprise compatibility

---

## 12. Phase Roadmap & Status

> Update **Status**: ⬜ Not started · 🟨 In progress · ✅ Done

### Phase 1 — Foundation / Validation
| Task | Status | Notes |
|------|--------|-------|
| Gradle multi-module scaffold | ✅ | root + `common-api` + `intellij-plugin`, wrapper 9.1.0, Java 21 |
| `common-api` DTO contracts | ✅ | `Models.kt` — validate/sandbox/logs DTOs; compiles |
| Jenkinsfile detection | ✅ | `JenkinsfileDetector` + `JenkinsfileAssociationActivity` (assoc w/ Groovy) |
| Connection settings + secure token | ✅ | `PipelinePilotSettings` (PasswordSafe) + `PipelinePilotConfigurable` UI |
| Remote validation client | ✅ | `ValidationClient` — CSRF crumb, basic auth, `LinterOutputParser` |
| Inline diagnostics | ✅ | `JenkinsfileExternalAnnotator` (line/col → TextRange) |
| Validate action + notification | ✅ | `ValidateJenkinsfileAction` (Ctrl+Alt+V) |
| Parser unit tests | ✅ | `LinterOutputParserTest` — 3/3 green |
| `intellij-plugin` SDK compile verified | ✅ | compiles vs IntelliJ 2024.2 (IC) SDK, tests pass |

### Phase 2 — Execution
| Task | Status | Notes |
|------|--------|-------|
| Shared Jenkins HTTP helper | ✅ | `JenkinsHttp` — auth + CSRF crumb, reused by sandbox client |
| Sandbox client | ✅ | `SandboxClient` — `runSandbox` / `fetchLogs` (progressive) / `deleteSession` |
| Log-streaming DTO | ✅ | `LogChunk` (events + nextStart + more cursor) |
| Sandbox orchestration | ✅ | `SandboxService` — bg run, poll loop, session registry, auto-cleanup |
| Streaming log console | ✅ | `PipelinePilotLogConsole` — timestamped, stage/status coloring |
| Tool window | ✅ | `PipelinePilotToolWindowFactory` — Logs + Sessions tabs (Stop & Clean Up) |
| Run in Sandbox action | ✅ | `RunInSandboxAction` (Ctrl+Enter) |
| SDK compile + tests | ✅ | compiles vs 2024.2 SDK, 3/3 tests pass |

### Phase 3 — Visualization & Libraries
| Task | Status | Notes |
|------|--------|-------|
| Stage graph model | ✅ | `StageGraphModel` — tree from `StageInfo` events, parallel/nested |
| Stage graph panel | ✅ | `StageGraphPanel` — custom-drawn boxes+arrows, status colors, right-click replay |
| Replay DTO + client | ✅ | `ReplayRequest` + `SandboxClient.replay` (`/ide/replay`) |
| Replay orchestration | ✅ | `SandboxService` — replayPipeline / replayFailed / replayStage, last-run tracking |
| Replay action + UI | ✅ | `ReplayPipelineAction`, Stages-tab buttons, per-stage context menu |
| Shared lib scanner | ✅ | `SharedLibrarySupport` — vars/ steps + `@Library` parse, settings roots |
| Step autocomplete | ✅ | `SharedLibraryCompletionContributor` (vars/*.groovy → step()) |
| Step navigation | ✅ | `SharedLibraryGotoDeclarationHandler` (Ctrl+Click → vars file) |
| `@Library` annotator | ✅ | `SharedLibraryAnnotator` — resolved info / unresolved weak-warning |
| SDK compile + tests | ✅ | compiles vs 2024.2 SDK; 8/8 tests pass |

### Companion — Jenkins Plugin (`jenkins-plugin/`, Maven)
| Task | Status | Notes |
|------|--------|-------|
| Maven HPI module + pom | ✅ | parent `org.jenkins-ci.plugins:plugin:4.88`, BOM 2.426.x, JDK 17 |
| IDE API root action | ✅ | `IdeApiRootAction` — `/ide/validate,runSandbox,replay,logs,session` |
| Sandbox session store | ✅ | `SandboxSessionStore` — ephemeral `WorkflowJob` under `IDE-SANDBOX/<user>/`, sandbox=true, cleanup |
| Security | ✅ | `@RequirePOST` (CSRF) + per-endpoint permission checks + Groovy sandbox |
| Progressive log endpoint | ✅ | `doLogs` — `writeLogTo(start)` cursor → LogChunk JSON |
| Plugin descriptor | ✅ | `index.jelly` |
| Maven compile + package | ✅ | compiles vs Jenkins 2.426.3 + BOM; `mvn package` → `target/pipelinepilot.hpi` |
| Stage-graph events | ✅ | `StageScanner` — flow-graph → `StageInfo` in `doLogs`; **E2E verified** (sequential + parallel/nested, parent + status) |
| Integration TODOs | ⬜ | remaining: real linter delegation in `doValidate`, stage-scoped replay (documented in README) |

### Phase 4 — Intelligence
| Task | Status | Notes |
|------|--------|-------|
| AI settings (key in PasswordSafe) | ✅ | `aiEnabled` / `aiModel` / key; UI fields |
| Anthropic client + prompt caching | ✅ | `AiClient` — Messages API, `cache_control: ephemeral` system block |
| AI assistant service | ✅ | `AiAssistant` — `suggestFix` + `analyzePipeline`, code-block extraction |
| AI suggested-fix quick-fix | ✅ | `AiSuggestFixIntention` — explains + applies corrected file |
| Deterministic quick-fix | ✅ | `AddAgentAnyIntention` (no-AI fix for missing agent) |
| Quick-fixes wired to diagnostics | ✅ | `JenkinsfileExternalAnnotator.withFix(...)` |
| Pipeline optimization recs | ✅ | `AnalyzePipelineAction` → AI insights into Logs console |
| SDK compile + tests + package | ✅ | 11/11 tests; `buildPlugin` → `intellij-plugin-0.1.0.zip` |

---

## 13. Deliverables Checklist

- [x] Full architecture proposal (§1–11)
- [x] IntelliJ plugin skeleton (compiles + packages)
- [x] Jenkins plugin skeleton (compiles + packages → `pipelinepilot.hpi`)
- [x] API contracts (`common-api/Models.kt` + endpoints §6)
- [x] Kotlin implementation examples (all of `intellij-plugin/`)
- [x] PSI parsing examples (Groovy assoc, completion, goto, annotators)
- [x] ToolWindow examples (Logs / Sessions / Stages)
- [x] Logs streaming implementation (`SandboxClient.fetchLogs` + console)
- [x] Validation service implementation (`ValidationClient` + `LinterOutputParser`)
- [x] Sandbox execution orchestration (`SandboxService`)
- [x] Build scripts + Gradle configuration (multi-module, wrapper 9.1.0)
- [ ] Example screenshot mockups (ASCII) — partial (stage graph drawn live)
- [x] MVP roadmap (this doc, §12)
- [x] Testing strategy (11 unit tests across parser/lib/stages/AI)

---

## 14. Current Position

> **We are here:** ✅ **ALL 4 PHASES COMPLETE (IDE side).** Plugin compiles vs IntelliJ 2024.2 SDK; **11/11 tests pass**; `buildPlugin` produces `intellij-plugin/build/distributions/intellij-plugin-0.1.0.zip` (searchable-options pass = plugin.xml + all extensions valid). P1 validation, P2 sandbox+streaming, P3 stage graph+replay+shared-libs, P4 AI fixes+analysis (Anthropic Messages API w/ prompt caching, key in PasswordSafe).
> **Update:** ✅ **END-TO-END VERIFIED** against a live `mvn hpi:run` Jenkins (2.426.3) at :8080, driving `/ide/*` over HTTP exactly as the IDE does (crumb + cookie jar):
> - `POST /ide/validate` valid → `{valid:true}`; broken → `{valid:false, diagnostics:[…]}`
> - `POST /ide/runSandbox` → accepted + sessionId; pipeline executed **isolated** in `IDE-SANDBOX/<user>/session-<id>`
> - `GET /ide/logs` → progressive stream, echo output captured, terminal `buildStatus:SUCCESS finished:true more:false`
> - `POST /ide/session` cleanup → 200, job removed (404 after) ✅
> - **Bug found & fixed:** IDE↔companion contract mismatch (path-style + HTTP DELETE) → aligned `SandboxClient` to query params + POST delete (preserves CSRF). IDE plugin recompiled, tests green.
> **Update 2:** ✅ **Stage graph events done + E2E verified.** `StageScanner` walks the run's flow graph (`BlockStartNode`+`LabelAction`, `ThreadNameAction` for parallel) and emits `StageInfo` in `doLogs`. Live test of a scripted pipeline with `parallel` returned: Checkout/Build/Test (sequential) + Branch:unit / Branch:integ (`parallel:true, parent:Test`), all status-tagged. Dev Jenkins stopped, port 8080 free.
> **Update 3:** ✅ **Dockerized deployment + complex-pipeline E2E.** Companion bumped to **Jenkins 2.504.3 LTS** (jakarta Stapler2, `jakarta.servlet-api:6.1.0`, `Descriptor.FormException` handled). `docker-compose.yml` + `jenkins-plugin/docker/{Dockerfile,plugins.txt,casc.yaml}` build a demo Jenkins (admin/admin, JCasC, seeded `demo-complex-pipeline`) with the `.hpi` preinstalled. Live test of a complex declarative pipeline (parallel Quality Gates: Unit/Integration/Lint + Package + Deploy + post) → **all stages SUCCESS**, nested+parallel stage graph correct, cleanup 200. Also confirmed graceful failure surfacing (a `timestamps()` compile error streamed through verbatim). Container live at http://localhost:8080.
> **Remaining (optional):** companion TODOs — real declarative-linter delegation in `doValidate`; stage-scoped replay. Stage scanner emits both `Branch: X` and inner `X` for declarative parallel (accurate but slightly redundant — could collapse). IDE polish — icons, Insights tab, AI quick-fix on warnings. Manual IDE `runIde` GUI pass.

### Run the demo stack
```
docker compose up -d --build      # http://localhost:8080  (admin/admin)
# IDE API live at http://localhost:8080/ide/*  ; seeded job: demo-complex-pipeline
docker compose down -v            # tear down + wipe volume
```

### File map (Phase 1 + 2)
```
common-api/src/main/kotlin/com/pipelinepilot/api/Models.kt   (+ LogChunk)
intellij-plugin/src/main/kotlin/com/pipelinepilot/
  JenkinsfileDetector.kt
  JenkinsfileAssociationActivity.kt
  settings/PipelinePilotSettings.kt        (+ sandbox paths)
  settings/PipelinePilotConfigurable.kt
  api/ValidationClient.kt                  (+ LinterOutputParser)
  api/JenkinsHttp.kt                       [P2] auth + CSRF
  api/SandboxClient.kt                     [P2] run/logs/delete
  validation/JenkinsfileExternalAnnotator.kt
  sandbox/PipelinePilotLogConsole.kt       [P2] streaming console
  sandbox/SandboxService.kt                [P2/P3] orchestration + replay
  sandbox/StageGraphModel.kt               [P3] stage tree
  toolwindow/PipelinePilotToolWindowFactory.kt  [P2/P3] Logs + Sessions + Stages
  toolwindow/StageGraphPanel.kt            [P3] custom-drawn graph
  library/SharedLibrarySupport.kt          [P3] vars/ scanner + @Library parse
  library/SharedLibraryCompletionContributor.kt  [P3] step autocomplete
  library/SharedLibraryGotoDeclarationHandler.kt [P3] step navigation
  library/SharedLibraryAnnotator.kt        [P3] @Library annotation
  ai/AiClient.kt                           [P4] Anthropic Messages + caching
  ai/AiAssistant.kt                        [P4] suggestFix + analyzePipeline
  ai/AiSuggestFixIntention.kt              [P4] AI quick-fix
  validation/AddAgentAnyIntention.kt       [P4] deterministic quick-fix
  actions/ValidateJenkinsfileAction.kt
  actions/RunInSandboxAction.kt            [P2]
  actions/ReplayPipelineAction.kt          [P3]
  actions/AnalyzePipelineAction.kt         [P4] AI insights
intellij-plugin/src/main/resources/META-INF/plugin.xml
intellij-plugin/src/test/kotlin/com/pipelinepilot/api/LinterOutputParserTest.kt
examples/Jenkinsfile, examples/Jenkinsfile.broken
```

### Post-MVP fixes & changes (2026-06-04)
- **Critical bug fixed:** plugin.xml used `defaultExtensionPointName` (invalid) instead of `defaultExtensionNs` → IntelliJ silently dropped the entire `<extensions>` block (settings, tool window, annotators, completion all missing; only `<actions>` worked). Caught via a headless registration test (`ConfigurableRegistrationTest`, kept as regression guard). Verified `configurable=true toolwindow=true`.
- **IDE target bumped** to local build 261 (IntelliJ IDEA 2025.3/2026.1); Kotlin 2.0→2.3.10 (metadata 2.3); `-PlocalIdePath` build option added; `since-build=261`.
- **AI provider switched Anthropic → OpenAI-compatible Chat Completions** (`AiClient`): provider-agnostic via configurable base URL — OpenAI, Azure, OpenRouter, Ollama, LM Studio, vLLM, opencode. Settings: `aiBaseUrl`, `aiModel` (default `gpt-4o-mini`), key in PasswordSafe (optional for local providers).
- **Companion Jenkins plugin** moved to Jenkins 2.504.3 LTS (jakarta Stapler2, jakarta-servlet-api 6.1.0, `Descriptor.FormException`); `StageScanner` emits stage events.
- **Plugin icon** added (`META-INF/pluginIcon.svg` + dark, from `docs/6c7d46d4-…jpg`, center-cropped square).
- **README.md** authored (professional, English): why / architecture / features / API / quick start / AI / security.

### Finishing pass (2026-06-04)
- **AI review = inline popup** (`AiReviewDialog`) instead of console: structured per-line
  findings (`reviewPipeline` → JSON), severity colors, click-to-navigate, **Apply Fix (AI)**.
- **AI provider dropdown** + **Test Connection** (green/red) + provider-agnostic OpenAI client.
- **Marketplace-ready**: rich `plugin.xml` description + `change-notes` + homepage; plugin icon;
  `docs/MARKETPLACE_LISTING.md` checklist (✅ in-repo vs ⛏️ admin-panel items).
- **Repo hygiene**: `LICENSE` (Apache-2.0), `.gitignore`, `CHANGELOG.md`, professional `README.md`.
- **Real example**: `examples/products-api/` Spring Boot CRUD + Jenkinsfile, verified green E2E
  through the plugin against Dockerized Jenkins.

### Follow-up pass — items 1 & 2 (2026-06-04)
- ✅ **True per-stage replay** — companion `SandboxSessionStore.restartStage` uses
  `RestartDeclarativePipelineAction.run(stage)` to restart the source declarative run from a
  stage (new build on the same job). **Verified E2E**: replay "Test" → Build skipped, Test+Deploy
  run, SUCCESS. IDE no longer auto-deletes finished sessions (kept for replay); cleanup on project
  dispose, on "Stop & Clean Up", and via a 10-session retention cap.
- ✅ **Inline gutter markers for AI findings** — `AiFindingsHolder` + `AiFindingsAnnotator` render
  each AI review finding as a gutter marker + hover tooltip on its line; `Analyze Pipeline (AI)`
  caches findings and restarts the daemon. Plus an **Apply Fix (AI)** button in the review popup.

### Known remaining (intentional / needs owner)
- **Companion `/ide/validate`** stays a lightweight brace check; the IDE defaults to the official
  `/pipeline-model-converter/validate` linter (real validation already works) — not rewritten to
  avoid risky API churn.
- **Marketplace ⛏️ items** require the owner's accounts/assets: screenshots (≥1200×760), tags,
  license selection, and replacing placeholder URLs (`pipelinepilot.dev`, GitHub org).

_Last updated: 2026-06-04_
