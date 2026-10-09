package com.andrerinas.openheadunit.view

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectionRendererPolicyTest {
    @Test fun `first install defaults to SurfaceView for MG4 AllGo alignment`() {
        assertEquals(0, ProjectionRendererPolicy.resolve(null, "DiLink5.1"))
        assertEquals(0, ProjectionRendererPolicy.resolve(null, "Other"))
        assertEquals(0, ProjectionRendererPolicy.resolve(null, "EH32"))
    }
    @Test fun `upgrades preserve every explicitly selected renderer`() {
        for (mode in 0..2) assertEquals(mode, ProjectionRendererPolicy.resolve(mode, "DiLink5.1"))
    }
    @Test fun `invalid imported preferences fall back to SurfaceView`() {
        assertEquals(0, ProjectionRendererPolicy.resolve(99, "DiLink5.1"))
        assertEquals(0, ProjectionRendererPolicy.resolve(-1, "Other"))
    }
}
