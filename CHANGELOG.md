# Changelog

All notable changes to **PipelinePilot for Jenkins** are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/); this project
adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]
### Added
- AI review popup: per-line findings with severity, navigation to the line, and an
  **Apply Fix (AI)** action.
- **Test Connection** button for the AI provider (green/red status).
- AI provider dropdown (OpenAI, OpenRouter, Ollama, LM Studio, Custom) that auto-fills
  the base URL and a suggested model.
- Marketplace listing metadata (rich `description`, `change-notes`, homepage URL) and a
  publication checklist (`docs/MARKETPLACE_LISTING.md`).

### Changed
- AI backend is now provider-agnostic (OpenAI-compatible Chat Completions) instead of a
  single vendor.
- `Analyze Pipeline (AI)` now opens an inline review popup instead of writing to the
  Logs console.

## [0.1.0] — 2026-06-04
### Added
- Jenkinsfile detection and inline validation with gutter diagnostics.
- Quick-fixes on linter errors (deterministic + AI-assisted).
- Remote sandbox execution (Ctrl+Enter) with live, timestamped log streaming.
- Stage graph for sequential / parallel / nested stages with status colors.
- Replay: whole pipeline, only-failed stages, or a single stage.
- Shared library awareness: `@Library` detection, `vars/` step completion and navigation.
- Jenkins companion plugin exposing `/ide/*` (validate, runSandbox, replay, logs,
  session) with CSRF + permission checks and Groovy-sandbox isolation.
- Dockerized demo Jenkins and a real Spring Boot **Products API** example.

[Unreleased]: https://github.com/pipelinepilot/pipelinepilot/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/pipelinepilot/pipelinepilot/releases/tag/v0.1.0
