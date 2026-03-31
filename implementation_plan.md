# V-PAD: Virtual Gamepad Bridge — Implementation Plan

## Goal
Build an Android app that exposes a **virtual X-Input gamepad** (Xbox One identity) to the system using Shizuku's privileged access to `IInputManager`. A floating overlay lets the user touch-control the virtual gamepad from any app/game.

---

## User Review Required

> [!IMPORTANT]
> **Shizuku Requirement**: The device must have Shizuku installed and running (ADB or root). V-PAD cannot inject input without it. This must be clearly communicated to users before the app is published.

> [!WARNING]
> **Anti-Cheat Risk (Phase 4)**: The plan includes adding random noise to coordinates to avoid anti-cheat detection. This relies on game-specific tuning. The initial implementation will leave this as a configurable option — disabled by default.

> [!CAUTION]
> **Copyright**: No Xbox/Microsoft logos or branding will be used in the UI or Play Store listing. The hardware IDs (VID/PID) are used only internally to identify the virtual device to the OS — this is standard practice in gamepad emulation apps.

---

## Proposed Changes

### Component 1: Android Project Foundation

#### [NEW] Android Project (Kotlin + Compose)
- **Language**: Kotlin; UI: Jetpack Compose; Min SDK: 26; Target SDK: 35
- **Module**: single `:app` module
- **Location**: `/home/spiderybook78/Downloads/Develop/Producción app/VirtualX/`

#### [MODIFY] `build.gradle.kts` (app-level)
Key dependencies to add:
```kotlin
// Shizuku
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")

// Jetpack Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.05.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

#### [MODIFY] `AndroidManifest.xml`
Permissions and components:
- `SYSTEM_ALERT_WINDOW`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `RECEIVE_BOOT_COMPLETED`
- `<provider>` for `ShizukuProvider`
- `<service>` for `VPadService` (foreground, exported=false)
- `<activity>` for `MainActivity`

---

### Component 2: AIDL / Binder (Privilege Layer)

#### [NEW] `aidl/android/hardware/input/IInputManager.aidl`
Minimal stub of the hidden `IInputManager` interface exposing:
- `injectInputEvent(InputEvent, int): boolean`
- `createVirtualInputDevice(...)` (hidden API via reflection fallback)

#### [NEW] `VirtualDeviceManager.kt`
- Binds to Shizuku's binder with `Shizuku.getBinder()`
- Uses reflection to call `InputManager.getInstance().injectInputEvent()` under the privileged context
- Registers the virtual device with:
  - **Vendor ID**: `0x045E` (Microsoft)
  - **Product ID**: `0x02EA` (Xbox One Controller)
  - **Source**: `SOURCE_GAMEPAD | SOURCE_JOYSTICK`

---

### Component 3: Foreground Service (Input Engine)

#### [NEW] `VPadService.kt`
- Extends `Service`, runs as `startForeground()`
- Maintains a persistent notification (required by Android)
- Provides a `sendButton(keycode: Int, down: Boolean)` API
- Provides a `sendAxis(axis: Int, value: Float)` API
- Uses a `HandlerThread` or coroutine `Dispatchers.IO` for the low-latency event loop

#### [NEW] `InputProcessor.kt`
- **Dead-zone**: if `sqrt(x² + y²) < threshold` → clamp to 0.0
- **Sensitivity curve**: `out = sign(in) * |in|^exponent` (configurable exponent, default 1.5)
- **Axis mapping table**:

| Physical Input | Android Axis |
|---|---|
| Left Stick H | `AXIS_X` |
| Left Stick V | `AXIS_Y` |
| Right Stick H (trackpad) | `AXIS_Z` |
| Right Stick V (trackpad) | `AXIS_RZ` |
| L2 Trigger | `AXIS_LTRIGGER` |
| R2 Trigger | `AXIS_RTRIGGER` |

---

### Component 4: Overlay UI (Jetpack Compose)

#### [NEW] `OverlayManager.kt`
- Uses `WindowManager` with `TYPE_APPLICATION_OVERLAY`
- Flags: `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN`
- Hosts a `ComposeView` as the floating overlay

#### [NEW] `GamepadOverlay.kt` (Composable)
- D-pad (4 directional buttons)
- Face buttons: A (green), B (red), X (blue), Y (yellow)
- Shoulder buttons: LB, RB
- Analog triggers: LT, RT (touch-and-hold with pressure simulation)
- Start / Select / Guide buttons
- **Right Trackpad Zone**: a draggable area for camera control

#### [NEW] `TrackpadZone.kt` (Composable)
Implements the camera algorithm:
```
TOUCH DOWN → store (x₀, y₀)
TOUCH MOVE → ΔX = x_now - x_prev; ΔY = y_now - y_prev
             velocity_x += ΔX * sensitivity
             velocity_y += ΔY * sensitivity
APPLY EXPO  → out = sign(v) * |v/max|^exponent * range
SEND AXIS   → sendAxis(AXIS_Z, out_x); sendAxis(AXIS_RZ, out_y)
TOUCH UP    → sendAxis(AXIS_Z, 0f); sendAxis(AXIS_RZ, 0f)
```

---

### Component 5: Configuration & Persistence

#### [NEW] `ProfileRepository.kt`
- Uses `DataStore<Preferences>` for active settings (sensitivity, dead-zone, overlay opacity, layout position)
- Stores per-game profiles as JSON files in app's internal storage (`filesDir/profiles/`)

#### [NEW] `GameProfile.kt` (data class)
```kotlin
data class GameProfile(
    val name: String,
    val packageName: String,
    val sensitivity: Float = 1.0f,
    val deadZone: Float = 0.1f,
    val exponentialCurve: Float = 1.5f,
    val overlayOpacity: Float = 0.75f
)
```

#### [NEW] `SettingsScreen.kt` (Compose)
- Slider for sensitivity, dead-zone, opacity
- Profile selector (by installed game)

---

## Verification Plan

### Phase 1 — Shizuku Binding
**Manual test** (requires physical Android device with Shizuku running):
1. Install the APK via `adb install app-debug.apk`
2. Open the app → grant Shizuku permission when prompted
3. Check logcat: `adb logcat -s VPadService` — should show `"Shizuku binder acquired"`
4. Tap "Test Button A" in SettingsScreen
5. Open any web browser → confirm text cursor or page scroll responds to the key event

### Phase 2 — Overlay Display
**Manual test**:
1. Grant `SYSTEM_ALERT_WINDOW` permission in Android Settings
2. Start VPadService from the app
3. Switch to another app (e.g., home screen)
4. Confirm the gamepad overlay appears floating over other apps

### Phase 3 — Gamepad Detection
**Manual test** with [Gamepad Tester](https://hardwaretester.com/gamepad):
1. Start VPadService
2. Open Chrome → navigate to `https://hardwaretester.com/gamepad`
3. Press any overlay button
4. The page should detect a connected gamepad and show button presses in real-time

### Phase 4 — Camera Trackpad
**Manual test** in any game that uses the right stick for camera:
1. Enable trackpad mode in Settings
2. Drag the right trackpad zone → confirm camera moves smoothly
3. Verify exponential response: slow drag = small movement; fast drag = large movement

### Build Verification
```bash
cd "/home/spiderybook78/Downloads/Develop/Producción app/VirtualX"
./gradlew assembleDebug
```
Build must succeed with 0 errors.
