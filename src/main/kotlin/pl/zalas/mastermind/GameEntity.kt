package pl.zalas.mastermind

import io.vlingo.common.Completes
import io.vlingo.common.Failure
import io.vlingo.common.Outcome
import io.vlingo.common.Success
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.EventSourced.registerConsumer
import pl.zalas.mastermind.Game.GameException.IncompleteCode
import pl.zalas.mastermind.GameEvent.GameStarted
import pl.zalas.mastermind.GameEvent.GuessMade

class GameEntity(id: GameId) : EventSourced(), Game {
    private var state: State = State.initial(id)

    data class State(
        val id: GameId,
        val secret: Code,
        private val moves: Int,
        private val guesses: List<Code>
    ) {

        companion object {
            fun initial(id: GameId) = State(id, Code(emptyList()), 0, emptyList())
        }

        fun start(secret: Code, moves: Int): State = copy(secret = secret, moves = moves)

        fun makeGuess(guess: Code): State = copy(guesses = guesses + listOf(guess))

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

    override fun makeGuess(guess: Code): CompletesWithFeedbackOutcome {
        if (state.secret.pegs.size != guess.pegs.size) {
            return applyError(IncompleteCode(state.secret.pegs.size, guess.pegs.size))
        }
        return giveFeedback(guess).run {
            applySuccess(GuessMade(state.id, guess, this), this)
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
        state = state.makeGuess(guessMade.guess)
    }

    private fun <E : Throwable, T> applyError(error: E): Completes<Outcome<E, T>> = apply(emptyList()) {
        Failure.of<E, T>(error)
    }

    private fun <E : Throwable, T> applySuccess(event: DomainEvent, value: T): Completes<Outcome<E, T>> = apply(event) {
        Success.of<E, T>(value)
    }

    override fun streamName() = state.id.toString()
}