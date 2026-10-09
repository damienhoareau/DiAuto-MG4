package com.andrerinas.openheadunit.view

/** Preserve explicit choices. MG4 / AllGo-aligned default is SurfaceView (direct composition). */
object ProjectionRendererPolicy {
    fun resolve(storedMode: Int?, model: String): Int {
        if (storedMode in 0..2) return storedMode!!
        return 0 // SURFACE — matches stock AllGo on MG4 EH32
    }
}
