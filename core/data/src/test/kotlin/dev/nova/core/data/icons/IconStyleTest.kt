package dev.nova.core.data.icons

import dev.nova.core.domain.model.IconShape
import org.junit.Assert.assertEquals
import org.junit.Test

class IconStyleTest {
    @Test fun roundTrip() {
        val style = IconStyle("com.pack.x", themed = true, shape = IconShape.TEARDROP)
        assertEquals(style, IconStyle.parse(style.key))
    }

    @Test fun defaultStyleHasNoPack() {
        val style = IconStyle.parse(IconStyle("", false, IconShape.SYSTEM).key)
        assertEquals("", style.pack)
        assertEquals(IconShape.SYSTEM, style.shape)
    }

    @Test fun garbageFallsBackToDefaults() {
        assertEquals(IconStyle("", false, IconShape.SYSTEM), IconStyle.parse("||???"))
    }
}
