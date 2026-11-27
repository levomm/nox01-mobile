# NOX01 Fast Mode — design
Date: 2026-11-27
Status: design only; implementation and device benchmarks pending.

## Observed baseline (main)
- `Tools.java`: tap uses a 60 ms gesture and afterAction(before, 1200); afterAction polls the accessibility screen fingerprint every 150 ms. waitFor polls every 250 ms and treats 700 ms of stability as settled.
- `Tools.java`: screenshotOnce waits 150 ms for bubble pass-through and uses AccessibilityService.takeScreenshot; a rate-limit error triggers a 1100 ms wait and retry. Screenshots are resized to a 720 px short side and JPEG quality 70.
- Existing tools include read_screen, wait_for, do_steps, screenshot, and a Background.java virtual-display path. Preserve these interfaces and avoid a second automation stack.
- Actual provider latency, foreground/background device timings and camera/frame capture limits have not yet been benchmarked.

## Design
Add a per-session execution mode (AI, HYBRID, LOCAL) with HYBRID as the proposed default for supported routine UI tasks and existing behavior available in AI mode. LOCAL is limited to deterministic, explicitly supported routines; unsupported actions return a clear status instead of guessing. Keep capture, perception and provider calls independent, with a bounded latest-state queue and cancellation on session end. Use monotonic timestamps and generation IDs; discard outdated observations and revalidate the target before acting. Keep a single action executor per display/session and preserve existing Guard, approval, permission and stop paths.

Stage 1: instrument capture, UI lookup, action, verification, and provider round trips; expose measured latency in existing UI (DOOM bubble only where space permits). Separate action verification from fixed sleeps using accessibility events with a deadline and polling fallback. Do not report a changed screen without verification. Benchmark before selecting timeouts.
Stage 2: introduce a local, event-driven screen-state observer using existing accessibility and background capture paths; coalesce duplicate state changes. Add on-device text recognition for supported screens where accessibility is insufficient, subject to device permissions. Never assume AccessibilityService screenshots support continuous 10–20 FPS; evaluate a user-authorized MediaProjection path separately if warranted.
Stage 3: add local execution of known, reversible UI routines, with existing do_steps as the compatibility layer; send only relevant observations to the selected model when local execution is insufficient. Do not hard-code a particular provider. Preserve user takeover and stop behavior.

## Reference: Agent-VScreen
https://github.com/kilolonion/agent-vscreen (MIT project; verify individual bundled component licenses before copying).
Useful architectural ideas: event/change reports, on-device OCR, background virtual display, live timeline and takeover, model-independent interface. Existing NOX01 background execution should be extended rather than duplicated. No third-party code imported in this design.

## Validation
- Measure p50/p95 capture, observation, action, verification, and full end-to-end latency on physical Android hardware.
- Compare identical routine UI tasks in current and new modes, both foreground and background.
- Test stale frames, rapid screen changes, cancellation, permission denial, orientation changes, accessibility rate limits, timeout and offline provider.
- Verify identical authorization/approval behavior across modes; no bypasses.
- Run unit/instrumentation tests and Android build; no release until verified.
- Initial target: sub-200 ms local observation/decision on supported simple cases, not a guarantee for whole actions or remote model calls.

## Non-goals
No provider replacement, new account requirements, or unrelated UI redesign in the first phase. No assumption that every app exposes readable UI or allows capture.
