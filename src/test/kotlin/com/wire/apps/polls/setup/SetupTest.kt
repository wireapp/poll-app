package com.wire.apps.polls.setup

import com.wire.apps.polls.dao.DatabaseSetup
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SetupTest {
    @Test
    fun `connectDatabase throws when connection failed`() =
        testApplication {
            // Arrange
            mockkObject(DatabaseSetup)

            every { DatabaseSetup.isConnected() } returns false

            // Act & Assert
            assertFailsWith<IllegalStateException> {
                application.connectDatabase()
            }

            unmockkObject(DatabaseSetup)
        }
}
