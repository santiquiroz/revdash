package com.revdash.app

import com.revdash.app.ui.escalaDiseno
import org.junit.Assert.assertEquals
import org.junit.Test

class EscalaDisenoTest {
    @Test
    fun pantallaDeDisenoNoSeEscala() = assertEquals(1f, escalaDiseno(1024f, 600f), 0.0001f)

    @Test
    fun celularApaisadoSeAjustaAlAlto() = assertEquals(0.64f, escalaDiseno(832f, 384f), 0.0001f)

    @Test
    fun pantallaAngostaSeAjustaAlAncho() = assertEquals(0.5f, escalaDiseno(512f, 600f), 0.0001f)
}
