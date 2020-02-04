package pl.zalas.mastermind.model

import io.vlingo.common.Completes
import io.vlingo.common.Failure
import io.vlingo.common.Outcome
import io.vlingo.common.Success
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.EventSourced.registerConsumer
import pl.zalas.mastermind.model.GameError.GameFinished
import pl.zalas.mastermind.model.GameError.IncompleteCode
import pl.zalas.mastermind.model.GameEvent.GameStarted
import pl.zalas.mastermind.model.GameEvent.GuessMade

class GameEntity(id: GameId) : EventSourced(), Game {
    private var state: State = State.initial(id)

    data class State(
        val id: GameId,
        val secret: Code,
        val isGameFinished: Boolean,
        private val moves: Int,
        private val guesses: List<Code>
    ) {

        companion object {
            fun initial(id: GameId) = State(id, Code(emptyList()), false, 0, emptyList())
        }

        fun start(secret: Code, moves: Int): State = copy(secret = secret, moves = moves)

        fun makeGuess(guess: Code, isGameFinished: Boolean): State = copy(
            guesses = guesses + listOf(guess),
            isGameFinished = isGameFinished
        )

        fun hasLastMoveLeft() = guesses.size == moves - 1
    }

    companion object {
        init {
            registerConsumer(GameEntity::class.java, GameStarted::class.java, GameEntity::applyGameStarted)
            registerConsumer(GameEntity::class.java, GuessMade::class.java, GameEntity::applyGuessMade)
        }
    }

    override fun startGame(secret: Code, moves: Int) {
        apply(GameStarted(state.id, secret, moves))
    }

    override fun makeGuess(guess: Code): CompletesWithFeedbackOutcome = when {
        state.isGameFinished ->
            applyError(GameFinished())
        state.secret.size != guess.size ->
            applyError(IncompleteCode(state.secret.size, guess.size))
        else -> giveFeedback(guess).andThen {
            applySuccess<GameError, Feedback>(GuessMade(state.id, guess, this), this)
        }
    }

    private fun giveFeedback(guess: Code): Feedback = when {
        state.secret.matches(guess) -> Feedback.won(state.secret, guess)
        state.hasLastMoveLeft() -> Feedback.lost(state.secret, guess)
        else -> Feedback.inProgress(state.secret, guess)
    }

    private fun applyGameStarted(gameStarted: GameStarted) {
        state = state.start(gameStarted.secret, gameStarted.moves)
    }

    private fun applyGuessMade(guessMade: GuessMade) {
        state = state.makeGuess(guessMade.guess, guessMade.feedback.isGameFinished())
    }

    override fun streamName() = state.id.toString()

    private fun <E : Throwable, T> applyError(error: E): Completes<Outcome<E, T>> = apply(emptyList()) {
        Failure.of<E, T>(error)
    }

    private fun <E : Throwable, T> applySuccess(event: DomainEvent, value: T): Completes<Outcome<E, T>> = apply(event) {
        Success.of<E, T>(value)
    }

    private inline fun <T, R> T.andThen(block: T.() -> R): R = block()
}