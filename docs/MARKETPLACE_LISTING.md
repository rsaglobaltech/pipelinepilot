# JetBrains Marketplace — Listing Guide & Compliance

Pre-publication checklist for **PipelinePilot for Jenkins**, mapped to the official
[Best practices for listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html).
Items marked ✅ are done in-repo; ⛏️ require action in the Marketplace admin panel at
publish time (cannot be set from `plugin.xml`).

## Plugin name
- ✅ Latin characters, title case, no "Plugin"/emoji, no JetBrains product reference.
- ✅ Reflects purpose. "Jenkins" is a third-party tool we integrate with (allowed).
- ⚠️ Slightly over the soft 20-char target; kept for clarity. Acceptable.

## Compatibility
- ✅ `since-build` set; no artificial `until-build` cap.
- ⛏️ Verify the compatibility range in the admin panel after upload. List only IDEs
  where it works (any IntelliJ-based IDE with the bundled Groovy plugin — IDEA
  Ultimate/Community, etc.).

## Description (`plugin.xml` → `<description>`)
- ✅ English, value proposition in the first sentence (< 40 chars summary up front).
- ✅ Bulleted feature list; numbered "Getting started".
- ✅ Inline links to docs/source and issue tracker.
- ✅ Factual, no marketing fluff, no unverifiable claims, no third-party brands beyond
  the tools we integrate with.
- ⛏️ Final spelling/grammar proofread before upload.

## Logo / icon
- ✅ `META-INF/pluginIcon.svg` (+ dark) shipped — not the IntelliJ template, not text-only.
- ⛏️ Confirm it renders crisply at 40×40 in the Marketplace preview.

## Media (screenshots / video)
- ⛏️ Add ≥1 screenshot, **min 1200×760**, consistent aspect ratio, legible text, no
  desktop background / personal info. Suggested shots:
  1. Jenkinsfile with inline validation + Alt+Enter quick-fixes.
  2. PipelinePilot tool window: **Logs** streaming + **Stages** graph (parallel branches).
  3. **AI Pipeline Review** popup with per-line findings.
  4. Settings page with the provider dropdown + Test Connection (green).
- ⛏️ Optional: < 5-min YouTube demo (edit → Ctrl+Enter → logs/stages).
- Reference mockups: [`UI_MOCKUPS.md`](UI_MOCKUPS.md).

## Tags / categories
- ⛏️ At least one tag at upload. Suggested: **Build**, **CI/CD**, **DevOps**,
  **Continuous Integration**. Email marketplace@jetbrains.com if a needed tag is missing.

## License
- ✅ Apache-2.0 declared in the project README.
- ⛏️ Select the license in the admin panel (mandatory) and keep the public source link.

## Vendor & links (`plugin.xml`)
- ✅ `<vendor email url>` set; `<idea-plugin url>` homepage set.
- ✅ Issue tracker + source links inside the description.
- ⛏️ Verify all URLs resolve publicly before publishing (placeholders today:
  `pipelinepilot.dev`, `github.com/pipelinepilot/...` — replace with the real ones).

## Getting started
- ✅ Numbered, action-oriented steps embedded in the description (not just an external link).

## Change notes (`plugin.xml` → `<change-notes>`)
- ✅ Per-version summary present for 0.1.0. Keep updating each release.

## Donations (optional)
- ⛏️ If desired, add a donation link via the Monetization tab — never in the description.

## General quality
- ✅ Accurate representation, professional tone, consistent branding, no excessive
  self-promotion.

---

### Publish-time TODO (admin panel)
1. Replace placeholder URLs/email with real ones (`plugin.xml` + this doc).
2. Upload 1–4 screenshots (≥1200×760) and an optional demo video.
3. Pick tags + license.
4. Proofread description; verify compatibility range.
5. Build the distribution: `./gradlew :intellij-plugin:buildPlugin` → upload the zip.
