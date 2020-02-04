package pl.zalas.mastermind.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

open class GameIdTests {
    @Test
    fun `GameId can be created from an existing string`() {
        val gameId = GameId("0576046d-d39e-4d44-83b1-75a37c0ad565")

        assertEquals("0576046d-d39e-4d44-83b1-75a37c0ad565", gameId.id)
    }

    @Test
    fun `GameId can be created with a randomly generated UUID`() {
        val gameId = GameId.generate()
        assertTrue {
            gameId.id.matches("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")
        }
    }

    @Test
    fun `GameId can be cast to a string`() {
        val gameId = GameId("31761884-2089-4026-98ac-ac47b26a9993")

        assertEquals("31761884-2089-4026-98ac-ac47b26a9993", gameId.toString())
    }

    private fun String.matches(expression: String) = Pattern.matches(expression, this)
}