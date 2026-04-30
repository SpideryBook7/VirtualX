package dev.vpad.controller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpad_settings")

data class VPadSettings(
    val inputMode: Int = 0, // 0 = Gamepad, 1 = PC (Keyboard/Mouse)
    
    val sensitivity: Float   = 1.0f,
    val deadZone: Float      = 0.08f,
    val curveExponent: Float = 1.4f,
    val overlayOpacity: Float = 0.75f,
    val buttonScale: Float   = 1.0f,

    val pillX: Int = -1,
    val pillY: Int = -1,

    // Dynamic map of button IDs to their X,Y offset
    val layoutOffsets: Map<String, Pair<Float, Float>> = emptyMap(),
    
    val activeProfileName: String = "Default",
    val savedProfiles: Map<String, Map<String, Pair<Float, Float>>> = emptyMap(),
    
    val pcKeyMap: Map<Int, Int> = emptyMap(),
    
    val editMode: Boolean = false,
    val hapticsEnabled: Boolean = true,
    
    val gyroEnabled: Boolean = false,
    val gyroSensitivity: Float = 1.0f,
    val gyroInvertY: Boolean = false,
    
    val pillFixedCenter: Boolean = false,
    val selectedSkin: String = "Neon",
    val activeControls: Set<String> = emptySet()
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val INPUT_MODE     = intPreferencesKey("input_mode")
        val SENSITIVITY    = floatPreferencesKey("sensitivity")
        val DEAD_ZONE      = floatPreferencesKey("dead_zone")
        val CURVE_EXPONENT = floatPreferencesKey("curve_exponent")
        val OPACITY        = floatPreferencesKey("opacity")
        val BUTTON_SCALE   = floatPreferencesKey("button_scale")
        
        val PILL_X = intPreferencesKey("pill_x")
        val PILL_Y = intPreferencesKey("pill_y")

        // Format is "id:x,y|id2:x,y"
        val LAYOUT_OFFSETS = stringPreferencesKey("layout_offsets")
        
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val SAVED_PROFILE_NAMES = androidx.datastore.preferences.core.stringSetPreferencesKey("saved_profile_names")
        
        // Format is "gamepadKey:pcKey|gamepadKey2:pcKey2"
        val PC_KEY_MAP     = stringPreferencesKey("pc_key_map")
        
        val EDIT_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("edit_mode")
        val HAPTICS_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("haptics_enabled")
        
        val GYRO_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("gyro_enabled")
        val GYRO_SENSITIVITY = androidx.datastore.preferences.core.floatPreferencesKey("gyro_sensitivity")
        val GYRO_INVERT_Y = androidx.datastore.preferences.core.booleanPreferencesKey("gyro_invert_y")
        
        val PILL_FIXED_CENTER = androidx.datastore.preferences.core.booleanPreferencesKey("pill_fixed_center")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin")
        val ACTIVE_CONTROLS = androidx.datastore.preferences.core.stringSetPreferencesKey("active_controls")
    }

    private fun parseLayout(str: String?): Map<String, Pair<Float, Float>> {
        if (str.isNullOrEmpty()) return emptyMap()
        val result = mutableMapOf<String, Pair<Float, Float>>()
        str.split("|").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val coords = parts[1].split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloatOrNull() ?: 0f
                    val y = coords[1].toFloatOrNull() ?: 0f
                    result[parts[0]] = Pair(x, y)
                }
            }
        }
        return result
    }

    private fun encodeLayout(map: Map<String, Pair<Float, Float>>): String {
        return map.entries.joinToString("|") { "${it.key}:${it.value.first},${it.value.second}" }
    }

    private fun parsePcKeyMap(str: String?): Map<Int, Int> {
        if (str.isNullOrEmpty()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        str.split("|").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val gameKey = parts[0].toIntOrNull()
                val pcKey = parts[1].toIntOrNull()
                if (gameKey != null && pcKey != null) result[gameKey] = pcKey
            }
        }
        return result
    }

    private fun encodePcKeyMap(map: Map<Int, Int>): String {
        return map.entries.joinToString("|") { "${it.key}:${it.value}" }
    }

    val settings: Flow<VPadSettings> = context.dataStore.data.map { prefs ->
        val profileNames = prefs[Keys.SAVED_PROFILE_NAMES] ?: emptySet()
        val profilesMap = mutableMapOf<String, Map<String, Pair<Float, Float>>>()
        for (name in profileNames) {
            val key = stringPreferencesKey("profile_$name")
            val data = prefs[key]
            if (!data.isNullOrEmpty()) {
                profilesMap[name] = parseLayout(data)
            }
        }

        VPadSettings(
            inputMode     = prefs[Keys.INPUT_MODE]     ?: 0,
            sensitivity   = prefs[Keys.SENSITIVITY]    ?: 1.0f,
            deadZone      = prefs[Keys.DEAD_ZONE]      ?: 0.08f,
            curveExponent = prefs[Keys.CURVE_EXPONENT] ?: 1.4f,
            overlayOpacity = prefs[Keys.OPACITY]       ?: 0.75f,
            buttonScale   = prefs[Keys.BUTTON_SCALE]   ?: 1.0f,
            pillX         = prefs[Keys.PILL_X]         ?: -1,
            pillY         = prefs[Keys.PILL_Y]         ?: -1,
            layoutOffsets = parseLayout(prefs[Keys.LAYOUT_OFFSETS]),
            activeProfileName = prefs[Keys.ACTIVE_PROFILE] ?: "Custom",
            savedProfiles = profilesMap,
            pcKeyMap      = parsePcKeyMap(prefs[Keys.PC_KEY_MAP]),
            editMode      = prefs[Keys.EDIT_MODE]      ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            gyroEnabled   = prefs[Keys.GYRO_ENABLED]   ?: false,
            gyroSensitivity = prefs[Keys.GYRO_SENSITIVITY] ?: 1.0f,
            gyroInvertY   = prefs[Keys.GYRO_INVERT_Y]  ?: false,
            pillFixedCenter = prefs[Keys.PILL_FIXED_CENTER] ?: false,
            selectedSkin  = prefs[Keys.SELECTED_SKIN]  ?: "Neon",
            activeControls = prefs[Keys.ACTIVE_CONTROLS] ?: emptySet()
        )
    }

    suspend fun updateInputMode(m: Int)        = context.dataStore.edit { it[Keys.INPUT_MODE]     = m }
    suspend fun updateSensitivity(v: Float)    = context.dataStore.edit { it[Keys.SENSITIVITY]    = v }
    suspend fun updateDeadZone(v: Float)       = context.dataStore.edit { it[Keys.DEAD_ZONE]      = v }
    suspend fun updateCurveExponent(v: Float)  = context.dataStore.edit { it[Keys.CURVE_EXPONENT] = v }
    suspend fun updateOpacity(v: Float)        = context.dataStore.edit { it[Keys.OPACITY]        = v }
    suspend fun updateButtonScale(v: Float)    = context.dataStore.edit { it[Keys.BUTTON_SCALE]   = v }
    
    suspend fun updatePillPosition(x: Int, y: Int) = context.dataStore.edit { 
        it[Keys.PILL_X] = x
        it[Keys.PILL_Y] = y 
    }

    suspend fun updateComponentOffset(id: String, offset: Pair<Float, Float>) = context.dataStore.edit { prefs ->
        val current = parseLayout(prefs[Keys.LAYOUT_OFFSETS]).toMutableMap()
        current[id] = offset
        prefs[Keys.LAYOUT_OFFSETS] = encodeLayout(current)
    }

    suspend fun updatePcKeyMapping(gamepadKey: Int, pcKey: Int) = context.dataStore.edit { prefs ->
        val current = parsePcKeyMap(prefs[Keys.PC_KEY_MAP]).toMutableMap()
        current[gamepadKey] = pcKey
        prefs[Keys.PC_KEY_MAP] = encodePcKeyMap(current)
    }

    // Profile Management
    suspend fun saveCurrentLayoutAsProfile(name: String) = context.dataStore.edit { prefs ->
        val names = (prefs[Keys.SAVED_PROFILE_NAMES] ?: emptySet()).toMutableSet()
        names.add(name)
        prefs[Keys.SAVED_PROFILE_NAMES] = names
        
        val currentLayoutStr = prefs[Keys.LAYOUT_OFFSETS] ?: ""
        val profileKey = stringPreferencesKey("profile_$name")
        prefs[profileKey] = currentLayoutStr
        prefs[Keys.ACTIVE_PROFILE] = name
    }

    suspend fun deleteProfile(name: String) = context.dataStore.edit { prefs ->
        val names = (prefs[Keys.SAVED_PROFILE_NAMES] ?: emptySet()).toMutableSet()
        names.remove(name)
        prefs[Keys.SAVED_PROFILE_NAMES] = names
        
        val profileKey = stringPreferencesKey("profile_$name")
        prefs.remove(profileKey)
        
        if (prefs[Keys.ACTIVE_PROFILE] == name) {
            prefs[Keys.ACTIVE_PROFILE] = "Custom"
        }
    }

    suspend fun loadProfile(name: String) = context.dataStore.edit { prefs ->
        val profileKey = stringPreferencesKey("profile_$name")
        val layout = prefs[profileKey]
        if (!layout.isNullOrEmpty()) {
            prefs[Keys.LAYOUT_OFFSETS] = layout
            prefs[Keys.ACTIVE_PROFILE] = name
        }
    }

    suspend fun toggleEditMode(enabled: Boolean) = context.dataStore.edit { 
        it[Keys.EDIT_MODE] = enabled 
        if (enabled) {
            it[Keys.ACTIVE_PROFILE] = "Custom"
        }
    }
    suspend fun updateHapticsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    suspend fun updateGyroEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.GYRO_ENABLED] = enabled }
    suspend fun updateGyroSensitivity(v: Float) = context.dataStore.edit { it[Keys.GYRO_SENSITIVITY] = v }
    suspend fun updateGyroInvertY(enabled: Boolean) = context.dataStore.edit { it[Keys.GYRO_INVERT_Y] = enabled }
    suspend fun updatePillFixedCenter(fixed: Boolean) = context.dataStore.edit { it[Keys.PILL_FIXED_CENTER] = fixed }
    suspend fun updateSelectedSkin(skin: String) = context.dataStore.edit { it[Keys.SELECTED_SKIN] = skin }
    
    private val defaultControls = listOf("analog_left", "trackpad", "dpad_up", "dpad_down", "dpad_left", "dpad_right", "btn_a", "btn_b", "btn_x", "btn_y", "btn_l1", "btn_l2", "btn_r1", "btn_r2", "btn_rm", "btn_select", "btn_start")

    suspend fun addControl(id: String) = context.dataStore.edit { prefs ->
        val current = (prefs[Keys.ACTIVE_CONTROLS] ?: emptySet()).toMutableSet()
        if (current.isEmpty()) current.addAll(defaultControls)
        current.add(id)
        prefs[Keys.ACTIVE_CONTROLS] = current
    }

    suspend fun removeControl(id: String) = context.dataStore.edit { prefs ->
        val current = (prefs[Keys.ACTIVE_CONTROLS] ?: emptySet()).toMutableSet()
        if (current.isEmpty()) current.addAll(defaultControls)
        current.remove(id)
        prefs[Keys.ACTIVE_CONTROLS] = current
    }
    
    suspend fun resetActiveControls(controls: Set<String>) = context.dataStore.edit { prefs ->
        prefs[Keys.ACTIVE_CONTROLS] = controls
    }
}
