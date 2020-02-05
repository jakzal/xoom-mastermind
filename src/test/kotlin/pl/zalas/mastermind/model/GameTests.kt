package pl.zalas.mastermind.model

import io.vlingo.actors.World
import io.vlingo.common.Completes
import io.vlingo.lattice.model.sourcing.Sourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.inmemory.InMemoryJournalActor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import pl.zalas.mastermind.model.Code.CodePeg.*
import pl.zalas.mastermind.model.Feedback.KeyPeg.BLACK
import pl.zalas.mastermind.model.Feedback.KeyPeg.WHITE
import pl.zalas.mastermind.model.GameEvent.GameStarted
import pl.zalas.mastermind.model.GameEvent.GuessMade
import pl.zalas.mastermind.test.FakeGameEventDispatcher
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

    @Test
    fun `feedback is given on the guess`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(secret, 12)

        gameBoard.makeGuess(Code(RED, BLUE, YELLOW, BLUE)).waitForFeedbackAndThen {
            assertEquals(Feedback.won(BLACK, BLACK, BLACK, BLACK), it)
        }
    }

    @Test
    fun `error is returned if the code is not the same length as the secret`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(Code(RED, RED, RED, RED), 12)

        assertThrows<GameError.IncompleteCode> {
            gameBoard.makeGuess(Code(RED, RED)).waitForException()
        }
    }

    @Test
    fun `error is returned if the game is already won`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(Code(RED, RED, RED, RED), 12)
        gameBoard.makeGuess(Code(RED, RED, RED, RED))

        assertThrows<GameError.GameFinished> {
            gameBoard.makeGuess(Code(RED, RED, RED, RED)).waitForException()
        }
    }

    @Test
    fun `error is returned if the game is already lost`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(Code(RED, RED, RED, RED), 1)
        gameBoard.makeGuess(Code(GREEN, GREEN, GREEN, GREEN))

        assertThrows<GameError.GameFinished> {
            gameBoard.makeGuess(Code(RED, RED, RED, RED)).waitForException()
        }
    }

    private fun shouldHaveRaisedEvents(vararg events: GameEvent) {
        dispatcher.updateExpectedEventHappenings(events.size)
        assertEquals(events.toList(), dispatcher.events(), "Should have raised the following events")
    }

    companion object {
        @JvmStatic
        private fun provideFeedbackCases() = Stream.of(
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(PURPLE, PURPLE, PURPLE, PURPLE),
                Feedback.inProgress()
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(RED, PURPLE, PURPLE, PURPLE),
                Feedback.inProgress(BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(RED, RED, RED, RED),
                Feedback.inProgress(BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(PURPLE, BLUE, PURPLE, PURPLE),
                Feedback.inProgress(BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(PURPLE, BLUE, PURPLE, BLUE),
                Feedback.inProgress(BLACK, BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(RED, BLUE, YELLOW, PURPLE),
                Feedback.inProgress(BLACK, BLACK, BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(PURPLE, RED, PURPLE, PURPLE),
                Feedback.inProgress(WHITE)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(PURPLE, RED, RED, RED),
                Feedback.inProgress(WHITE)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(RED, RED, PURPLE, PURPLE),
                Feedback.inProgress(BLACK)
            ),
            Arguments.of(
                Code(RED, BLUE, YELLOW, BLUE),
                Code(BLUE, RED, BLUE, YELLOW),
                Feedback.inProgress(WHITE, WHITE, WHITE, WHITE)
            ),
            Arguments.of(
                Code(RED, BLUE, RED, BLUE),
                Code(RED, RED, PURPLE, PURPLE),
                Feedback.inProgress(BLACK, WHITE)
            ),
            Arguments.of(
                Code(RED, BLUE, RED, BLUE),
                Code(RED, RED, BLUE, BLUE),
                Feedback.inProgress(BLACK, BLACK, WHITE, WHITE)
            )
        )
    }

    private fun Completes<FeedbackOutcome>.waitForFeedbackAndThen(verify: (Feedback) -> Unit) {
        await<FeedbackOutcome>(1000).andThen { verify(it) }
    }

    private fun Completes<FeedbackOutcome>.waitForException() {
        await<FeedbackOutcome>(1000).otherwise { throw it }
    }
}