package pl.zalas.mastermind.model

import io.vlingo.xoom.actors.World
import io.vlingo.xoom.common.Completes
import io.vlingo.xoom.symbio.Entry
import io.vlingo.xoom.symbio.State
import io.vlingo.xoom.symbio.store.dispatch.Dispatchable
import io.vlingo.xoom.symbio.store.dispatch.Dispatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import pl.zalas.mastermind.infrastructure.factory.JournalFactory
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.model.Code.CodePeg.*
import pl.zalas.mastermind.model.Feedback.KeyPeg.BLACK
import pl.zalas.mastermind.model.Feedback.KeyPeg.WHITE
import pl.zalas.mastermind.model.GameEvent.GameStarted
import pl.zalas.mastermind.model.GameEvent.GuessMade
import pl.zalas.mastermind.test.FakeCodeMaker
import pl.zalas.mastermind.test.FakeGameEventDispatcher
import java.util.stream.Stream

class GameTests {

    private lateinit var world: World

    private lateinit var dispatcher: FakeGameEventDispatcher

    @BeforeEach
    fun startWorld() {
        world = World.startWithDefaults("mastermind")
        dispatcher = FakeGameEventDispatcher()
        @Suppress("UNCHECKED_CAST")
        JournalFactory(world.stage(), InMemoryConfiguration).createJournal(dispatcher as Dispatcher<Dispatchable<out Entry<*>, out State<*>>>)
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

        shouldRaiseEvents(
            GameStarted(gameId, secret, 12),
            GuessMade(gameId, Code(RED, RED, RED, RED), Feedback.inProgress(BLACK)),
            GuessMade(gameId, Code(RED, BLUE, YELLOW, BLUE), Feedback.won(BLACK, BLACK, BLACK, BLACK))
        ) {
            gameBoard.startGame(FakeCodeMaker(secret), 12)
            gameBoard.makeGuess(Code(RED, RED, RED, RED))
            gameBoard.makeGuess(Code(RED, BLUE, YELLOW, BLUE))
        }
    }

    @Test
    fun `game is lost if there is no attempts left`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        shouldRaiseEvents(
            GameStarted(gameId, secret, 2),
            GuessMade(gameId, Code(RED, RED, RED, RED), Feedback.inProgress(BLACK)),
            GuessMade(gameId, Code(PURPLE, PURPLE, PURPLE, PURPLE), Feedback.lost())
        ) {
            gameBoard.startGame(FakeCodeMaker(secret), 2)
            gameBoard.makeGuess(Code(RED, RED, RED, RED))
            gameBoard.makeGuess(Code(PURPLE, PURPLE, PURPLE, PURPLE))
        }
    }

    @ParameterizedTest
    @MethodSource("provideFeedbackCases")
    fun `feedback is given on each move made`(secret: Code, guess: Code, expectedFeedback: Feedback) {
        val gameId = GameId.generate()
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        shouldRaiseEvents(
            GameStarted(gameId, secret, 12),
            GuessMade(gameId, guess, expectedFeedback)
        ) {
            gameBoard.startGame(FakeCodeMaker(secret), 12)
            gameBoard.makeGuess(guess)
        }
    }

    @Test
    fun `feedback is given on the guess`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        gameBoard.startGame(FakeCodeMaker(secret), 12)

        gameBoard.makeGuess(Code(RED, BLUE, YELLOW, BLUE)).waitForFeedbackAndThen {
            assertEquals(Feedback.won(BLACK, BLACK, BLACK, BLACK), it)
        }
    }

    @Test
    fun `error is returned if the code is not the same length as the secret`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(FakeCodeMaker(Code(RED, RED, RED, RED)), 12)

        assertThrows<GameError.IncompleteCode> {
            gameBoard.makeGuess(Code(RED, RED)).waitForException()
        }
    }

    @Test
    fun `error is returned if the game is already won`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(FakeCodeMaker(Code(RED, RED, RED, RED)), 12)
        gameBoard.makeGuess(Code(RED, RED, RED, RED))

        assertThrows<GameError.GameFinished> {
            gameBoard.makeGuess(Code(RED, RED, RED, RED)).waitForException()
        }
    }

    @Test
    fun `error is returned if the game is already lost`() {
        val gameBoard = world.actorFor(Game::class.java, GameEntity::class.java, GameId.generate())

        gameBoard.startGame(FakeCodeMaker(Code(RED, RED, RED, RED)), 1)
        gameBoard.makeGuess(Code(GREEN, GREEN, GREEN, GREEN))

        assertThrows<GameError.GameFinished> {
            gameBoard.makeGuess(Code(RED, RED, RED, RED)).waitForException()
        }
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

    private fun shouldRaiseEvents(vararg events: GameEvent, block: () -> Unit) {
        dispatcher.updateExpectedEventHappenings(events.size)
        block()
        assertEquals(events.toList(), dispatcher.events(), "Should have raised the following events")
    }

    private fun Completes<FeedbackOutcome>.waitForFeedbackAndThen(verify: (Feedback) -> Unit) {
        await<FeedbackOutcome>(1000).andThen { verify(it) }
    }

    private fun Completes<FeedbackOutcome>.waitForException() {
        await<FeedbackOutcome>(1000).otherwise { throw it }
    }
}