# V-PAD (Virtual Gamepad Bridge) - Task List

## Phase 0: Planning
- [/] Analyze requirements and write implementation plan
- [ ] Get user approval on plan

## Phase 1: Project Scaffolding (Foundation)
- [ ] Initialize Android project in VirtualX directory with Kotlin + Compose
- [ ] Configure build.gradle (Shizuku API, Compose, Room, DataStore deps)
- [ ] Setup AndroidManifest.xml (permissions, services, activity)
- [ ] Create AIDL interfaces (IInputManager stub)

## Phase 2: Shizuku + Privilege Layer
- [ ] Implement ShizukuProvider setup
- [ ] Create IInputManagerBinder wrapper
- [ ] Register virtual InputDevice (VID 0x045E, PID 0x02EA)
- [ ] Test KeyEvent injection (KEYCODE_BUTTON_A)

## Phase 3: Foreground Service (Input Engine)
- [ ] Create VPadService (foreground service)
- [ ] Implement low-latency event loop
- [ ] Implement dead-zone and sensitivity curve logic
- [ ] Handle axis events (AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_LTRIGGER, AXIS_RTRIGGER)

## Phase 4: Overlay UI
- [ ] Create OverlayManager with WindowManager TYPE_APPLICATION_OVERLAY
- [ ] Implement transparent button overlay with FLAG_NOT_FOCUSABLE
- [ ] Build Compose-based gamepad layout (buttons, D-pad, triggers)
- [ ] Implement Touch Zone Exclusion logic

## Phase 5: Camera Trackpad Algorithm
- [ ] Implement touch delta calculation (ΔX, ΔY)
- [ ] Map deltas to AXIS_Z / AXIS_RZ as velocity
- [ ] Apply exponential smoothing curve
- [ ] Support analog triggers (L2/R2 → AXIS_LTRIGGER/AXIS_RTRIGGER)

## Phase 6: Configuration & Persistence
- [ ] Design JSON profile schema per game
- [ ] Implement DataStore or Room for layout persistence
- [ ] Build basic settings UI (sensitivity, dead-zones, transparency)

## Phase 7: Verification
- [ ] Manual testing with Gamepad Tester app
- [ ] Battery optimization audit
- [ ] Prepare Play Store assets (description, screenshots)
