# PipelinePilot — Visual / UI Mockups (ASCII)

How the plugin presents inside IntelliJ IDEA. ASCII approximations of each surface.

---

## 1. Editor — inline validation + quick-fix

A Jenkinsfile open. Remote linter ran on save; error highlighted with gutter marker.
`Alt+Enter` on the error offers fixes.

```
 Jenkinsfile ─────────────────────────────────────────────────────────────────
  1   pipeline {
  2 ⚠   stages {                         ◄ gutter marker (red)
  3       stage('Build') {
  4         steps { sh 'make' }
  5       }
  6     }
  7   }
        ╰── Missing required section "agent"  @ line 1
            ┌───────────────────────────────────────────────┐
            │  💡  PipelinePilot: Add 'agent any'            │  ◄ deterministic
            │  💡  PipelinePilot: Suggest fix (AI)          │  ◄ AI (if enabled)
            │      More actions…                            │
            └───────────────────────────────────────────────┘
```

After "Add 'agent any'":

```
  1   pipeline {
  2       agent any          ◄ inserted, error clears
  3       stages {
```

---

## 2. Editor — shared-library autocomplete + navigation

Typing a step name; completion lists global vars from configured `vars/` checkouts.
`@Library` line annotated.

```
  1 ⓘ @Library('platform-lib') _        ◄ "Shared library: platform-lib" (info)
  2   pipeline {
  3     agent any
  4     stages {
  5       stage('Deploy') {
  6         steps { dep▌ }
  7       }                ┌──────────────────────────────────────────────┐
  8     }                  │ ƒ deploy            shared library step       │
  9   }                    │ ƒ deployToK8s       shared library step       │
                          │ ƒ deployHelmChart   shared library step       │
                          └──────────────────────────────────────────────┘
   Ctrl+Click on 'deploy'  ──►  jumps to  vars/deploy.groovy
```

If no local checkout: `@Library('x')` gets a weak-warning instead of the info tag.

---

## 3. Tool window — PipelinePilot (bottom dock), 3 tabs

### Tab: Logs  (streaming console)

```
 PipelinePilot ▸ [ Logs ] [ Sessions ] [ Stages ]            ⟳  ⏹  ✕ ───────────
 ┌────────────────────────────────────────────────────────────────────────────┐
 │ === Sandbox session 6c9df7cc… started (TEMP_FOLDER) ===                      │
 │ [13:05:21] Started by remote host IDE — PipelinePilot sandbox               │
 │ [13:05:21] [Pipeline] Start of Pipeline                                     │
 │ [13:05:22]                                                                   │
 │ [13:05:22] === Stage: Checkout [RUNNING] ===                                │
 │ [13:05:22] checkout scm                                                      │
 │ [13:05:23] === Stage: Build [RUNNING] ===                                   │
 │ [13:05:24] + ./gradlew build                                                │
 │ [13:05:31] === Stage: Tests [RUNNING] ===                                   │
 │ [13:05:34] >>> Build finished: SUCCESS                                       │
 └────────────────────────────────────────────────────────────────────────────┘
```

### Tab: Sessions

```
 PipelinePilot ▸ [ Logs ] [ Sessions ] [ Stages ] ────────────────────────────
 ┌────────────────────────────────────────────────────────────────────────────┐
 │  6c9df7cc-2c47-48c5-be14-…   —   FINISHED                                    │
 │  84313d68-c099-4b5b-afb1-…   —   RUNNING                                     │
 │  20824e3b-8cca-481c-8f06-…   —   STOPPED                                     │
 │                                                                              │
 │                                                  [ Stop & Clean Up ]         │
 └────────────────────────────────────────────────────────────────────────────┘
```

### Tab: Stages  (live graph + replay)

```
 PipelinePilot ▸ [ Logs ] [ Sessions ] [ Stages ] ────────────────────────────
 [ Replay Pipeline ]  [ Replay Failed Stages ]
 ┌────────────────────────────────────────────────────────────────────────────┐
 │                                            ┌───────────────┐                 │
 │  ╭──────────╮   ╭───────╮   ╭───────╮   ┌─►│ Branch: Unit  │ ✓              │
 │  │ Checkout │──►│ Build │──►│ Tests │───┤  └───────────────┘                 │
 │  │   ✓      │   │  ✓    │   │  �: ⟳  │   └─►┌──────────────────┐              │
 │  ╰──────────╯   ╰───────╯   ╰───────╯      │ Branch: Integ    │ ⟳           │
 │                                            └──────────────────┘   ╭────────╮ │
 │                                            (parallel, parent=Tests) │ Deploy │ │
 │                                                                     │  ·     │ │
 │   legend:  ✓ success   ⟳ running   ✗ failed   · pending   ⊘ skipped ╰────────╯ │
 │   right-click a stage ▸ "Replay stage \"Tests\""                              │
 └────────────────────────────────────────────────────────────────────────────┘
```

Box fill colors by status: green=success, blue=running, red=failed, grey=skipped, amber=pending.

---

## 4. Editor context menu / actions

```
 Right-click in a Jenkinsfile ────────────
   …
   Validate Jenkinsfile            Ctrl+Alt+V
   Run in Sandbox                  Ctrl+Enter
   Replay Pipeline
   Analyze Pipeline (AI)
   …
```

---

## 5. Settings  (Settings ▸ Tools ▸ PipelinePilot for Jenkins)

```
 ┌─ PipelinePilot for Jenkins ────────────────────────────────────────────────┐
 │  Jenkins URL:              [ http://localhost:8080                       ]  │
 │  Username:                 [ admin                                       ]  │
 │  API token:                [ ••••••••••••••••                            ]  │
 │  Validate endpoint path:   [ /pipeline-model-converter/validate         ]  │
 │  Shared library roots      ┌────────────────────────────────────────────┐  │
 │  (one path per line):      │ C:\src\platform-lib                        │  │
 │                            │ C:\src\shared-ci                           │  │
 │                            └────────────────────────────────────────────┘  │
 │  [x] Enable AI assistance (Anthropic)                                       │
 │  Anthropic API key:        [ ••••••••••••••••                            ]  │
 │  AI model:                 [ claude-sonnet-4-6                           ]  │
 └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. AI suggested-fix dialog

After `Alt+Enter ▸ Suggest fix (AI)` on an error:

```
 ┌─ PipelinePilot AI — Suggested Fix ──────────────────────────────────────────┐
 │  The pipeline is missing the required top-level `agent` directive, so the   │
 │  declarative validator rejects it. Added `agent any`.                        │
 │                                                                              │
 │  Apply the suggested fix?                                                     │
 │                                          [ Apply ]   [ Cancel ]              │
 └─────────────────────────────────────────────────────────────────────────────┘
```

"Apply" replaces the file with the AI-corrected Jenkinsfile (single undo step).

---

## 7. Notifications (balloon)

```
   ┌─────────────────────────────────────────────┐
   │ ✓ Jenkinsfile successfully validated.        │
   └─────────────────────────────────────────────┘
   ┌─────────────────────────────────────────────┐
   │ ✗ Jenkinsfile validation failed: 2 error(s). │
   │   See inline markers.                         │
   └─────────────────────────────────────────────┘
```
