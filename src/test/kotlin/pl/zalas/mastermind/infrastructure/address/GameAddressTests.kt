package pl.zalas.mastermind.infrastructure.address

import io.vlingo.xoom.actors.Address
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.zalas.mastermind.model.GameId

class GameAddressTests {

    @Test
    fun `it exposes address properties`() {
        val address = GameAddress(GameId("8f29645c-ee9d-471c-a340-3966e326c81a"))

        assertEquals("8f29645c-ee9d-471c-a340-3966e326c81a", address.idString())
        assertEquals("8f29645c-ee9d-471c-a340-3966e326c81a", address.idSequenceString())
        assertEquals(1092514734917109532, address.id())
        assertEquals(1092514734917109532, address.idSequence())
        assertEquals("game", address.name())
        assertEquals(true, address.isDistributable())
        assertEquals("8f29645c-ee9d-471c-a340-3966e326c81a", address.idTyped<String>())
    }

    @Test
    fun `addresses are equal if the game id is the same`() {
        val address = GameAddress(GameId("8f29645c-ee9d-471c-a340-3966e326c81a"))
        val otherAddress = GameAddress(GameId("8f29645c-ee9d-471c-a340-3966e326c81a"))
        val differentAddress = GameAddress(GameId("e07adc13-c16b-4c87-b170-fa31b4de3cd8"))

        assertEquals(0, address.compareTo(otherAddress))
        assertEquals(1, address.compareTo(differentAddress))
    }

    @Test
    fun `addresses are not equal if they are different types`() {
        val address = GameAddress(GameId("8f29645c-ee9d-471c-a340-3966e326c81a"))
        val otherAddress = object : Address {
            override fun idString() = "8f29645c-ee9d-471c-a340-3966e326c81a"

            override fun id() = 1092514734917109532

            override fun idSequenceString() = idString()

            override fun compareTo(other: Address?) = 0

            override fun idSequence() = id()

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any?> idTyped(): T? = idString() as? T

            override fun name(): String = "test"

            override fun isDistributable(): Boolean = false
        }

        assertEquals(-1, address.compareTo(otherAddress))
    }
}