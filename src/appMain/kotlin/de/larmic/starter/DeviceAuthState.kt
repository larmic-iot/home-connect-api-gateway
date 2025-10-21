package de.larmic.starter

/**
 * In-memory holder for the latest device authorization state.
 * Note: this is ephemeral and resets on application restart.
 */
object DeviceAuthState {
    private var _deviceCode: String? = null

    val deviceCode: String?
        get() = _deviceCode

    fun updateDeviceCode(deviceCode: String) {
        _deviceCode = deviceCode
        println("Stored device_code in memory for later token exchange.")
    }
}
