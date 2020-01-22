package pl.zalas.mastermind

import io.vlingo.actors.World
import io.vlingo.lattice.model.sourcing.Sourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.inmemory.InMemoryJournalActor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.zalas.mastermind.Code.Pin.*
import pl.zalas.mastermind.GameEvent.*

class MastermindTests {

    private lateinit var world: World

    private val dispatcher = FakeGameEventDispatcher()

    @BeforeEach
    fun startWorld() {
        world = World.startWithDefaults("mastermind")
        val journal = Journal.using(world.stage(), InMemoryJournalActor::class.java, dispatcher)
        val registry = SourcedTypeRegistry(world)
        registry.register(
            SourcedTypeRegistry.Info(
                journal,
                GameEntity::class.java as Class<Sourced<String>>,
                GameEntity::class.java.simpleName
            )
        )
    }

    @AfterEach
    fun stopWorld() {
        world.terminate()
    }

    @Test
    fun `game is won if the guess code matches the secret code`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(secret, 12)
        gameBoard.makeGuess(Code(RED, RED, RED, RED))
        gameBoard.makeGuess(Code(RED, BLUE, YELLOW, BLUE))

        shouldHaveRaisedEvents(
            GameStarted(gameId, secret, 12),
            GuessMade(gameId, Code(RED, RED, RED, RED)),
            GuessMade(gameId, Code(RED, BLUE, YELLOW, BLUE)),
            GameWon(gameId)
        )
    }

    @Test
    fun `game is lost if there is no attempts left`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(secret, 2)
        gameBoard.makeGuess(Code(RED, RED, RED, RED))
        gameBoard.makeGuess(Code(BLUE, BLUE, PURPLE, PURPLE))

        shouldHaveRaisedEvents(
            GameStarted(gameId, secret, 2),
            GuessMade(gameId, Code(RED, RED, RED, RED)),
            GuessMade(gameId, Code(BLUE, BLUE, PURPLE, PURPLE)),
            GameLost(gameId)
        )
    }

    private fun shouldHaveRaisedEvents(vararg events: GameEvent) {
        dispatcher.updateExpectedEventHappenings(events.size)
        assertEquals(events.toList(), dispatcher.events(), "Should have raised the following events")
    }
}