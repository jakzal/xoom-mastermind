package pl.zalas.mastermind.view

import io.vlingo.xoom.actors.World
import io.vlingo.xoom.common.Completes
import io.vlingo.xoom.symbio.store.state.StateStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.zalas.mastermind.infrastructure.factory.JournalFactory
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration
import pl.zalas.mastermind.model.Code
import pl.zalas.mastermind.model.Code.CodePeg.*
import pl.zalas.mastermind.model.Game
import pl.zalas.mastermind.model.GameEntity
import pl.zalas.mastermind.model.GameId
import pl.zalas.mastermind.test.FakeCodeMaker
import pl.zalas.mastermind.test.FakeStateStoreDispatcher
import pl.zalas.mastermind.view.DecodingBoard.Move

class DecodingBoardTests {
    private lateinit var world: World

    private lateinit var store: StateStore

    private lateinit var storeDispatcher: FakeStateStoreDispatcher

    @BeforeEach
    fun startWorld() {
        world = World.startWithDefaults("mastermind")
        storeDispatcher = FakeStateStoreDispatcher()
        store = StateStoreFactory(world.stage(), world.defaultLogger(), StateStoreConfiguration.InMemoryConfiguration)
            .createStateStore(storeDispatcher)

        JournalFactory(world.stage(), JournalConfiguration.InMemoryConfiguration).createJournal(store)
    }

    @AfterEach
    fun stopWorld() {
        world.terminate()
    }

    @Test
    fun `game state is created from game events`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val game = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        waitForEvents(3) {
            game.startGame(FakeCodeMaker(secret), 12)
            game.makeGuess(Code(RED, RED, GREEN, ORANGE))
            game.makeGuess(Code(BLUE, RED, GREEN, ORANGE))
        }

        assertCompletesAs(
            DecodingBoard(gameId.id, 12, listOf(
                Move(listOf("RED", "RED", "GREEN", "ORANGE"), listOf("BLACK")),
                Move(listOf("BLUE", "RED", "GREEN", "ORANGE"), listOf("WHITE", "WHITE"))
            )),
            decodingBoardQuery().findDecodingBoardForGame(gameId.toString())
        )
    }

    @Test
    fun `game state is not found if the game has not been started`() {
        assertCompletesAs(
            DecodingBoard.NOT_FOUND,
            decodingBoardQuery().findDecodingBoardForGame(GameId.generate().toString())
        )
    }

    private fun decodingBoardQuery() =
        world.actorFor(DecodingBoardQuery::class.java, DecodingBoardQueryActor::class.java, store)

    private fun waitForEvents(number: Int, block: () -> Unit) {
        storeDispatcher.updateExpectedEventHappenings(number)
        block()
        assertEquals(number, storeDispatcher.states().size)
    }

    private fun <T> waitFor(completes: Completes<T>, block: (t: T) -> Unit) = block(completes.await())

    private fun <T> assertCompletesAs(expected: T, completes: Completes<T>) {
        waitFor(completes) {
            assertEquals(it, expected)
        }
    }
}