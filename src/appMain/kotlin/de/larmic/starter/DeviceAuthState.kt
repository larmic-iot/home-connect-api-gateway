package de.larmic.starter

/**
 * In-memory holder for the latest device authorization state.
 * Note: this is ephemeral and resets on application restart.
 */
object DeviceAuthState {
    var deviceCode: String? = null
        private set

    fun updateDeviceCode(deviceCode: String) {
        DeviceAuthState.deviceCode = deviceCode
        println("Stored device_code in memory for later token exchange.")
    }
}
