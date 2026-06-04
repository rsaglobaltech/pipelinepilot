package com.pipelinepilot.api

/**
 * Shared API contracts between the IntelliJ plugin and the Jenkins companion plugin.
 * These DTOs map 1:1 to the backend endpoints documented in PROJECT_PLAN.md (§6).
 */

// ---------------------------------------------------------------------------
// POST /ide/validate
// ---------------------------------------------------------------------------

data class ValidateRequest(
    val jenkinsfile: String,
    val pipelineId: String? = null,
    val branch: String? = null,
)

data class ValidateResponse(
    val valid: Boolean,
    val diagnostics: List<Diagnostic> = emptyList(),
)

enum class Severity { ERROR, WARNING, INFO }

/**
 * A single inline diagnostic. Line/column are 1-based to match Jenkins' linter output.
 * [offsetStart]/[offsetEnd] are optional 0-based character offsets; when absent the
 * IDE falls back to highlighting the whole line.
 */
data class Diagnostic(
    val severity: Severity,
    val message: String,
    val line: Int,
    val column: Int = 1,
    val offsetStart: Int? = null,
    val offsetEnd: Int? = null,
)

// ---------------------------------------------------------------------------
// POST /ide/runSandbox   (Phase 2 — defined now so the contract is stable)
// ---------------------------------------------------------------------------

enum class SandboxStrategy { TEMP_FOLDER, REPLAY }

data class RunSandboxRequest(
    val jenkinsfile: String,
    val strategy: SandboxStrategy = SandboxStrategy.REPLAY,
    val pipelineId: String? = null,
    val branch: String? = null,
)

data class RunSandboxResponse(
    val sessionId: String,
    val accepted: Boolean,
    val message: String? = null,
)

// ---------------------------------------------------------------------------
// POST /ide/replay   (Phase 3)
// ---------------------------------------------------------------------------

/**
 * Replay a pipeline. [stageName] null = whole pipeline; set it to rerun a single
 * stage. [onlyFailedStages] reruns just the stages that failed in the source run.
 * The original pipeline config is never mutated (REPLAY semantics).
 */
data class ReplayRequest(
    val jenkinsfile: String,
    val sourceSessionId: String? = null,
    val pipelineId: String? = null,
    val branch: String? = null,
    val stageName: String? = null,
    val onlyFailedStages: Boolean = false,
)

// ---------------------------------------------------------------------------
// GET /ide/logs/{sessionId}   (Phase 2/3)
// ---------------------------------------------------------------------------

enum class StageStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED }

data class StageInfo(
    val name: String,
    val status: StageStatus,
    val parent: String? = null,
    val parallel: Boolean = false,
)

data class LogEvent(
    val sessionId: String,
    val timestampMillis: Long,
    val line: String? = null,
    val stage: StageInfo? = null,
    val buildStatus: String? = null,
    val finished: Boolean = false,
)

/**
 * One progressive poll response from GET /ide/logs/{sessionId}?start=N.
 * [nextStart] is the cursor to pass on the next poll; [more] is false when the
 * run has finished and no further logs will arrive.
 */
data class LogChunk(
    val events: List<LogEvent> = emptyList(),
    val nextStart: Long = 0,
    val more: Boolean = true,
)
