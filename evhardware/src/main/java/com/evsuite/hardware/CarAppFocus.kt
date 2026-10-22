package com.evsuite.hardware

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * The car's own app-focus service (`android.car.CarAppFocusManager`), which the stock Android Auto
 * app uses to tell the rest of the head unit that projected navigation or a voice session is running.
 * Taking it through the car's service, rather than keeping a private flag, is what lets two apps on
 * the same car (DiAuto and DiPlay) see each other, and the car's own navigation.
 *
 * Reached by reflection over the `Car` object [EVHardware] already holds: `android.car` is not part
 * of the public SDK.
 */
object CarAppFocus {

    private const val TAG = "CarAppFocus"

    const val NAVIGATION = 1
    const val VOICE_COMMAND = 2

    private const val OWNERSHIP_CALLBACK = "android.car.CarAppFocusManager\$OnAppFocusOwnershipCallback"
    private const val CHANGED_LISTENER = "android.car.CarAppFocusManager\$OnAppFocusChangedListener"

    @Volatile private var ownershipLost: ((Int) -> Unit)? = null
    @Volatile private var ownershipProxy: Any? = null
    @Volatile private var changedProxy: Any? = null
    @Volatile private var changedListener: ((Int, Boolean) -> Unit)? = null
    private val listened = HashSet<Int>()

    private fun manager(): Any? = EVHardware.carManagerNamed("APP_FOCUS_SERVICE")

    private fun proxy(interfaceName: String, handler: InvocationHandler): Any? {
        val loader = EVHardware.appClassLoader() ?: return null
        return try {
            Proxy.newProxyInstance(loader, arrayOf(loader.loadClass(interfaceName))) { proxy, method, args ->
                // hashCode, equals and toString reach a proxy too, and a null answer to the primitive
                // ones fails the whole call: the car's service keeps its callbacks in a map.
                when (method.name) {
                    "hashCode" -> if (method.parameterCount == 0) System.identityHashCode(proxy) else handler.invoke(proxy, method, args)
                    "equals" -> if (method.parameterCount == 1) proxy === args?.getOrNull(0) else handler.invoke(proxy, method, args)
                    "toString" -> if (method.parameterCount == 0) "CarAppFocus.proxy" else handler.invoke(proxy, method, args)
                    else -> handler.invoke(proxy, method, args)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "cannot implement $interfaceName: ${e.message}")
            null
        }
    }

    /** Whether the car's app-focus service answers, which is when the car is bound and has it. */
    fun isAvailable(): Boolean = manager() != null

    /**
     * Takes the focus of [appType]. [onLost] is called with the type when another app takes it.
     * True when the car granted it; false when it refused, or when there is no service.
     */
    fun request(appType: Int, onLost: (Int) -> Unit): Boolean {
        val m = manager() ?: return false
        ownershipLost = onLost
        val callback = ownershipProxy ?: proxy(OWNERSHIP_CALLBACK) { _, method, args ->
            if (method.name == "onAppFocusOwnershipLost") {
                val type = (args?.getOrNull(0) as? Int) ?: 0
                AppLogger.i(TAG, "ownership of $type lost")
                ownershipLost?.invoke(type)
            }
            null
        }?.also { ownershipProxy = it } ?: return false
        return try {
            val result = m.javaClass.getMethod("requestAppFocus", Int::class.javaPrimitiveType,
                Class.forName(OWNERSHIP_CALLBACK, false, callback.javaClass.classLoader))
                .invoke(m, appType, callback) as? Int
            AppLogger.i(TAG, "requestAppFocus($appType) = $result")
            result == 1
        } catch (e: Exception) {
            AppLogger.w(TAG, "requestAppFocus($appType) failed: ${(e.cause ?: e).message}")
            false
        }
    }

    fun abandon(appType: Int) {
        val m = manager() ?: return
        val callback = ownershipProxy ?: return
        try {
            m.javaClass.getMethod("abandonAppFocus",
                Class.forName(OWNERSHIP_CALLBACK, false, callback.javaClass.classLoader),
                Int::class.javaPrimitiveType)
                .invoke(m, callback, appType)
            AppLogger.i(TAG, "abandonAppFocus($appType)")
        } catch (e: Exception) {
            AppLogger.w(TAG, "abandonAppFocus($appType) failed: ${(e.cause ?: e).message}")
        }
    }

    /**
     * Watches who holds the focus of [appType], any app included: the car's own navigation, or its
     * voice recognition. [onChanged] gets the type and whether it is now held.
     */
    @Synchronized
    fun listen(appType: Int, onChanged: (Int, Boolean) -> Unit) {
        changedListener = onChanged
        if (!listened.add(appType)) return
        val m = manager() ?: run { listened.remove(appType); return }
        val listener = changedProxy ?: proxy(CHANGED_LISTENER) { _, method, args ->
            if (method.name == "onAppFocusChanged") {
                val type = (args?.getOrNull(0) as? Int) ?: 0
                val active = (args?.getOrNull(1) as? Boolean) ?: false
                changedListener?.invoke(type, active)
            }
            null
        }?.also { changedProxy = it } ?: run { listened.remove(appType); return }
        try {
            m.javaClass.getMethod("addFocusListener",
                Class.forName(CHANGED_LISTENER, false, listener.javaClass.classLoader),
                Int::class.javaPrimitiveType)
                .invoke(m, listener, appType)
            AppLogger.i(TAG, "listening to the focus of $appType")
        } catch (e: Exception) {
            listened.remove(appType)
            AppLogger.w(TAG, "addFocusListener($appType) failed: ${(e.cause ?: e).message}")
        }
    }
}
