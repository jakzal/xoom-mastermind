package pl.zalas.mastermind

import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.EventSourced.registerConsumer
import pl.zalas.mastermind.GameEvent.*

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
            registerConsumer(GameEntity::class.java, GameWon::class.java, GameEntity::applyGameWon)
            registerConsumer(GameEntity::class.java, GameLost::class.java, GameEntity::applyGameLost)
        }
    }

    override fun startGame(secret: Code, moves: Int) {
        apply(GameStarted(state.id, secret, moves))
    }

    override fun makeGuess(guess: Code) = when {
        state.secret.matches(guess) -> apply(
            GuessMade(state.id, guess, Feedback.give(state.secret, guess)),
            GameWon(state.id)
        )
        state.hasLastMoveLeft() -> apply(
            GuessMade(state.id, guess, Feedback.give(state.secret, guess)),
            GameLost(state.id)
        )
        else -> apply(
            GuessMade(state.id, guess, Feedback.give(state.secret, guess))
        )
    }

    private fun applyGameStarted(gameStarted: GameStarted) {
        state = state.start(gameStarted.secret, gameStarted.moves)
    }

    private fun applyGuessMade(guessMade: GuessMade) {
        state = state.makeGuess(guessMade.guess)
    }

    private fun applyGameWon(gameWon: GameWon) {
    }

    private fun applyGameLost(gameLost: GameLost) {
    }

    override fun streamName() = state.id.toString()

    private fun apply(vararg events: GameEvent) = apply(events.toList())
}