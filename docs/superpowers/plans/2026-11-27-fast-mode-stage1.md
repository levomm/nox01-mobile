# Fast Mode stage 1 implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

Goal: event-driven UI verification for ordinary Android accessibility actions, with existing polling fallback and approvals preserved.
Architecture: a synchronized generation signal wakes the existing verification loops; a wakeup never constitutes proof of success. Existing deadlines and screen fingerprint verification remain intact.
Spec: docs/superpowers/specs/2026-11-27-fast-mode-design.md
Tech stack: Java, Android AccessibilityService, standalone javac smoke test.

- [x] Test notification-before-wait, timeout, concurrent signal, interruption. Baseline polling-only stand-in failed the first assertion; new signal passed all four locally.
- [x] Integrate UiChangeSignal into BuddyService and Tools, enable content-change events and 50ms event coalescing. Keep 150/250ms fallback polling and existing deadlines.
- [x] Log action verification duration without screen content.
- [ ] Run full Android build and signed APK checks in existing GitHub Actions.
- [ ] Benchmark on physical Android device before claiming actual speedup.
- [ ] Remaining design stages (mode selector, continuous capture, OCR, local routines, UI meter) are not implemented in stage 1.

Test command: mkdir -p /tmp/nox-signal-test && javac -d /tmp/nox-signal-test app/src/main/java/app/buddy/assistant/UiChangeSignal.java scripts/tests/UiChangeSignalTest.java && java -cp /tmp/nox-signal-test app.buddy.assistant.UiChangeSignalTest
