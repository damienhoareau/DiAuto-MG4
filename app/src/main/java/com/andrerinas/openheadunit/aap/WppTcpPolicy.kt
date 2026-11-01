package com.andrerinas.openheadunit.aap

/**
 * Whether WPP over TCP is in use, from the setting and the `debug.diauto.wpptcp` system property.
 *
 * The property wins when it is set to `0` or `1`, so the feature can be switched off (or forced on) on a
 * car without installing anything; any other value, or none, leaves the setting in charge.
 */
object WppTcpPolicy {
    const val PROPERTY = "debug.diauto.wpptcp"

    fun enabled(setting: Boolean, property: String?): Boolean = when (property?.trim()) {
        "0" -> false
        "1" -> true
        else -> setting
    }

    /** [enabled] with the property read from the system now. */
    fun enabledNow(setting: Boolean): Boolean = enabled(setting, readProperty())

    private fun readProperty(): String? = try {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java, String::class.java)
            .invoke(null, PROPERTY, "") as? String
    } catch (e: Throwable) {
        null
    }
}
