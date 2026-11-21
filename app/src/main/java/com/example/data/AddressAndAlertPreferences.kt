package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.customerAddressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lahori_address_and_alerts_prefs"
)

data class PersistedManualAddress(
    val addressId: String = "",
    val label: String = "Home",
    val fullAddress: String = "",
    val landmark: String = "",
    val branchId: String = ""
) {
    val hasSavedAddress: Boolean get() = fullAddress.isNotBlank()

    fun combinedDeliveryAddress(): String {
        val base = fullAddress.trim()
        val cleanLandmark = landmark.trim()
        if (base.isBlank()) return ""
        return if (cleanLandmark.isNotBlank() && !base.contains(cleanLandmark, ignoreCase = true)) {
            "$base (Landmark: $cleanLandmark)"
        } else {
            base
        }
    }
}

object AddressAndAlertPreferences {
    private val KEY_ADDRESS_ID = stringPreferencesKey("manual_address_id")
    private val KEY_ADDRESS_LABEL = stringPreferencesKey("manual_address_label")
    private val KEY_FULL_ADDRESS = stringPreferencesKey("manual_full_address")
    private val KEY_LANDMARK = stringPreferencesKey("manual_landmark")
    private val KEY_BRANCH_ID = stringPreferencesKey("manual_branch_id")
    private val KEY_ADMIN_DELIVERY_SOUND = booleanPreferencesKey("admin_delivery_sound_enabled")

    fun observeSavedManualAddress(context: Context): Flow<PersistedManualAddress> =
        context.applicationContext.customerAddressDataStore.data.map { prefs ->
            PersistedManualAddress(
                addressId = prefs[KEY_ADDRESS_ID].orEmpty(),
                label = prefs[KEY_ADDRESS_LABEL].orEmpty().ifBlank { "Home" },
                fullAddress = prefs[KEY_FULL_ADDRESS].orEmpty(),
                landmark = prefs[KEY_LANDMARK].orEmpty(),
                branchId = prefs[KEY_BRANCH_ID].orEmpty()
            )
        }

    suspend fun saveManualAddress(
        context: Context,
        fullAddress: String,
        landmark: String = "",
        label: String = "Home",
        branchId: String = "",
        addressId: String = ""
    ) {
        val cleanAddress = fullAddress.trim().take(300)
        val cleanLandmark = landmark.trim().take(120)
        val cleanLabel = label.trim().ifBlank { "Home" }.take(40)
        context.applicationContext.customerAddressDataStore.edit { prefs ->
            prefs[KEY_ADDRESS_ID] = addressId
            prefs[KEY_ADDRESS_LABEL] = cleanLabel
            prefs[KEY_FULL_ADDRESS] = cleanAddress
            prefs[KEY_LANDMARK] = cleanLandmark
            prefs[KEY_BRANCH_ID] = branchId.trim()
        }
    }

    suspend fun clearManualAddress(context: Context) {
        context.applicationContext.customerAddressDataStore.edit { prefs ->
            prefs.remove(KEY_ADDRESS_ID)
            prefs.remove(KEY_ADDRESS_LABEL)
            prefs.remove(KEY_FULL_ADDRESS)
            prefs.remove(KEY_LANDMARK)
            prefs.remove(KEY_BRANCH_ID)
        }
    }

    fun observeAdminSoundEnabled(context: Context): Flow<Boolean> =
        context.applicationContext.customerAddressDataStore.data.map { prefs ->
            prefs[KEY_ADMIN_DELIVERY_SOUND] ?: true
        }

    suspend fun setAdminSoundEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.customerAddressDataStore.edit { prefs ->
            prefs[KEY_ADMIN_DELIVERY_SOUND] = enabled
        }
    }

    private const val ROLE_SESSION_PREFS = "lahori_active_role_session_prefs"
    private const val KEY_ACTIVE_ROLE = "active_role"
    private const val KEY_ACTIVE_BRANCH_ID = "active_branch_id"
    private const val KEY_ACTIVE_EMAIL = "active_email"

    /**
     * Synchronously stores the currently signed-in user's role & assigned branchId so
     * [com.example.notifications.LahoriFirebaseMessagingService] can strictly filter
     * incoming FCM pushes (ensuring SuperAdmin NEVER receives new-order alerts and
     * BranchAdmin ONLY receives alerts for their assigned branch) even when the app is closed.
     */
    fun saveActiveRoleSession(
        context: Context,
        role: com.example.model.UserRole,
        assignedBranchId: String,
        email: String
    ) {
        context.applicationContext
            .getSharedPreferences(ROLE_SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_ROLE, role.name)
            .putString(KEY_ACTIVE_BRANCH_ID, assignedBranchId.trim())
            .putString(KEY_ACTIVE_EMAIL, email.trim().lowercase())
            .apply()
    }

    fun clearActiveRoleSession(context: Context) {
        context.applicationContext
            .getSharedPreferences(ROLE_SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun readActiveRoleSession(context: Context): Triple<com.example.model.UserRole, String, String> {
        val prefs = context.applicationContext
            .getSharedPreferences(ROLE_SESSION_PREFS, Context.MODE_PRIVATE)
        val role = com.example.model.UserRole.fromString(prefs.getString(KEY_ACTIVE_ROLE, null))
        val branchId = prefs.getString(KEY_ACTIVE_BRANCH_ID, "").orEmpty().trim()
        val email = prefs.getString(KEY_ACTIVE_EMAIL, "").orEmpty().trim().lowercase()
        return Triple(role, branchId, email)
    }
}
