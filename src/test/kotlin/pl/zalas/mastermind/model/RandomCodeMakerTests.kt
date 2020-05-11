package pl.zalas.mastermind.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RandomCodeMakerTests {

    @Test
    fun `it creates a code of given length`() {
        assertEquals(4, RandomCodeMaker(4)().size)
        assertEquals(2, RandomCodeMaker(2)().size)
    }

    @Test
    fun `it creates a random code`() {
        val codeMaker = RandomCodeMaker(10)

        // it's still possible to get the same code in two subsequent calls,
        // but we're gonna go for it anyway since it's rather unlikely
        assertNotEquals(codeMaker(), codeMaker())
    }
}
