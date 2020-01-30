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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import pl.zalas.mastermind.Code.CodePeg.*
import pl.zalas.mastermind.Feedback.KeyPeg.BLACK
import pl.zalas.mastermind.Feedback.KeyPeg.WHITE
import pl.zalas.mastermind.GameEvent.*
import java.util.stream.Stream

class GameTests {

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
            GuessMade(gameId, Code(RED, RED, RED, RED), Feedback.inProgress(BLACK)),
            GuessMade(gameId, Code(RED, BLUE, YELLOW, BLUE), Feedback.won(BLACK, BLACK, BLACK, BLACK))
        )
    }

    @Test
    fun `game is lost if there is no attempts left`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(secret, 2)
        gameBoard.makeGuess(Code(RED, RED, RED, RED))
        gameBoard.makeGuess(Code(PURPLE, PURPLE, PURPLE, PURPLE))

        shouldHaveRaisedEvents(
            GameStarted(gameId, secret, 2),
            GuessMade(gameId, Code(RED, RED, RED, RED), Feedback.inProgress(BLACK)),
            GuessMade(gameId, Code(PURPLE, PURPLE, PURPLE, PURPLE), Feedback.lost())
        )
    }

    @ParameterizedTest
    @MethodSource("provideFeedbackCases")
    fun `feedback is given on each move made`(secret: Code, guess: Code, expectedFeedback: Feedback) {
        val gameId = GameId.generate()
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(secret, 12)
        gameBoard.makeGuess(guess)

        shouldHaveRaisedEvents(
            GameStarted(gameId, secret, 12),
            GuessMade(gameId, guess, expectedFeedback)
        )
    }

    private fun shouldHaveRaisedEvents(vararg events: GameEvent) {
        dispatcher.updateExpectedEventHappenings(events.size)
        assertEquals(events.toList(), dispatcher.events(), "Should have raised the following events")
    }

    companion object {
        @JvmStatic
        private fun provideFeedbackCases() = Stream.of(
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(PURPLE, PURPLE, PURPLE, PURPLE), Feedback.inProgress()),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(RED, PURPLE, PURPLE, PURPLE), Feedback.inProgress(BLACK)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(RED, RED, RED, RED), Feedback.inProgress(BLACK)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(PURPLE, BLUE, PURPLE, PURPLE), Feedback.inProgress(BLACK)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(PURPLE, BLUE, PURPLE, BLUE), Feedback.inProgress(BLACK, BLACK)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(RED, BLUE, YELLOW, PURPLE), Feedback.inProgress(BLACK, BLACK, BLACK)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(PURPLE, RED, PURPLE, PURPLE), Feedback.inProgress(WHITE)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(PURPLE, RED, RED, RED), Feedback.inProgress(WHITE)),
            Arguments.of(Code(RED, BLUE, YELLOW, BLUE), Code(RED, RED, PURPLE, PURPLE), Feedback.inProgress(BLACK)),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(BLUE, RED, BLUE, YELLOW),
                Feedback.inProgress(WHITE, WHITE, WHITE, WHITE)
            ),
            Arguments.of(Code(RED, BLUE, RED, BLUE), Code(RED, RED, PURPLE, PURPLE), Feedback.inProgress(BLACK, WHITE)),
            Arguments.of(Code(RED, BLUE, RED, BLUE), Code(RED, RED, BLUE, BLUE), Feedback.inProgress(BLACK, BLACK, WHITE, WHITE))
        )
    }
}