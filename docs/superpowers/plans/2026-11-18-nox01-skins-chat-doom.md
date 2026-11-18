# NOX01 Global Skins, Chat Contrast and DOOM Eyes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a signed NOX01 2.3.15 APK where the user-supplied DOOM face has white eyes during execution, all native screens share switchable skin presets, and chat messages are unmistakably differentiated.

**Architecture:** Keep existing native Java `Theme`/`Ui`/`Prefs` as the single palette and preference layer. Keep DOOM animation in `Bubble`/`DoomFaces` with an updated base64 sprite atlas. Render visually distinct conversation cards in both `ChatActivity` and `Bubble`.

**Tech Stack:** Android Java 8, Android SDK 36, `gradle` Github Actions release pipeline, Python 3 Pillow for source-image atlas, existing NOX01 test scripts.

**Spec:** `docs/superpowers/specs/2026-11-18-nox01-skins-chat-doom-design.md`

## Global Constraints

- Application ID `ee.osx01.mobile`; next version `2.3.15` with `VERSION_CODE = 214`.
- Preserve every existing user preference and the release signing certificate. Don't affect Wallet, Cloudflare, MCP or permission policy.
- Use only user-supplied DOOM images; DOOM stays a bubble image, never inside chat replies.
- Honor Android reduced-motion settings, font scaling, live theme updates and original copy/Markdown accessibility.

---

### Task 1: White-eyed DOOM sprite and correct runtime state

**Files:**
- Modify: `app/src/main/assets/nox01/doom_faces.b64`
- Modify: `app/src/main/java/app/buddy/assistant/DoomFaces.java`, `Bubble.java`
- Test: `scripts/test_nox01_live_skins.py`

**Interfaces:** `DoomFaces.WORKING = 4` maps the source white-eye face; `Bubble.setWorking(boolean, boolean)` controls it.

- [ ] Step 1: Write failing regression test for active WORKING white-eye sprite, no novel image sources, and no routine tool-step override.
- [ ] Step 2: Run the regression test; verify it fails.
- [ ] Step 3: Use Pillow to copy the original 7 sprite cells from the approved atlas, crop the ninth first-row white-eye character from user `001Doom.png`, remove baked checkered background, and replace only frame 4. Save as `doom_faces.b64`.
- [ ] Step 4: Make `doomTick()` prioritize `doomRunning` as a continuous WHITE-EYE state; leave red/bloody frames for actual error; do not change `DoomFaces.WORKING` ordinal.
- [ ] Step 5: Run asset/behavior tests and existing `scripts/test_doom_bubble.py`; commit.

### Task 2: Five genuine global themes and appearance controls

**Files:**
- Modify: `app/src/main/java/app/buddy/assistant/Theme.java`, `Prefs.java`, `Ui.java`, `SetupActivity.java`
- Modify: `app/src/main/res/values/strings.xml`, `values-et/strings.xml`
- Test: `scripts/test_nox01_live_skins.py`

**Interfaces:** `Theme.surfaces(Context)` for surface selectors, `Prefs.surface(Context)` for persisted choice, `Theme.changed(Context)` for bubble refresh; `Prefs.cardDepth(Context)` for visual elevation, `Prefs.bubbleFrame(Context)` for frame style.

- [ ] Step 1: Extend failing regression test with five preset names, matching palette cases, and shared appearance settings.
- [ ] Step 2: Run and confirm failure.
- [ ] Step 3: Add `terminal`, `alert`, `steel`, `mono` surface palettes, localized UI labels, and five `themePresets()` choices; keep compatibility with previous surface values.
- [ ] Step 4: Replace hard-coded `Ui.neuSurface` colors with theme-derived colors; introduce selectable `Kaardiefekt` and `Mulli raam` controls through `Prefs` and `SetupActivity`. Make all controls trigger `Theme.changed`.
- [ ] Step 5: Re-test new and legacy appearance; commit.

### Task 3: Strong message contrast in both chat surfaces

**Files:**
- Modify: `app/src/main/java/app/buddy/assistant/ChatActivity.java`, `Bubble.java`, `Ui.java`
- Modify: `app/src/main/res/values/strings.xml`, `values-et/strings.xml`
- Test: `scripts/test_nox01_live_skins.py`

**Interfaces:** `Ui.userMessageSurface(Context)`, `Ui.agentMessageSurface(Context)` produce color-appropriate background drawables; `Ui.userLabel(Context)` and `Ui.agentLabel(Context)` use localized text.

- [ ] Step 1: Add failing tests for right-aligned user card, left-aligned agent card in both views, theme-derived contrast, and copy/link/table preservation.
- [ ] Step 2: Run to verify failing assertions.
- [ ] Step 3: Add theme-derived shared message card helpers in `Ui`; replace only message container logic in full and floating chat, preserving selection and Markdown contents.
- [ ] Step 4: Run tests and legacy branding / chat tests; commit.

### Task 4: Release validation

**Files:**
- Modify: `app/src/main/java/app/buddy/assistant/BuildInfo.java`
- Test: `scripts/test_nox01_live_skins.py`, `scripts/test_doom_bubble.py`, `scripts/test_neu_dark_ui.py`, `scripts/test_nox01_agent_expansion.py`

**Interfaces:** `BuildInfo.VERSION = "2.3.15"`, `VERSION_CODE = 214`.

- [ ] Step 1: Increment app version; run static tests and `git diff --check`.
- [ ] Step 2: Push feature branch, open PR, run GitHub Actions signed-release build.
- [ ] Step 3: Inspect compile and test logs, fix each root cause, and repeat until passing.
- [ ] Step 4: Verify release SHA-256/signature policy, merge into main, publish v2.3.15 APK if build is valid. Distinguish device test unavailable from build success.
