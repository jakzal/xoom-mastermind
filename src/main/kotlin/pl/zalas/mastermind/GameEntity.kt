package pl.zalas.mastermind

import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.EventSourced.registerConsumer
import pl.zalas.mastermind.GameEvent.*

class GameEntity(val id: GameId) : EventSourced(), Game {
    private var state: State = State.initial(id)

    data class State(
        val id: GameId,
        val secret: Code,
        val moves: Int,
        val guesses: List<Code>
    ) {

        companion object {
            fun initial(id: GameId) = State(id, Code(emptyList()), 0, emptyList())
        }

        fun start(secret: Code, moves: Int): State = copy(secret = secret, moves = moves)

        fun makeGuess(guess: Code): State = copy(guesses = guesses + listOf(guess))

        fun isGuessSuccessful(guess: Code) = secret.pins == guess.pins

        fun hasLastMoveLeft() = guesses.size == moves - 1

        fun exactHits(guess: Code) = secret.pins
            .zip(guess.pins)
            .filter { it.first == it.second }
            .map { Feedback.Pin.EXACT_HIT }

        fun colourHits(guess: Code) = secret.diff(guess).run {
            val secretColours = first.countPins()
            val guessColours = second.countPins()
            secretColours
                .map { minOf(it.value, guessColours.getOrDefault(it.key, 0)) }
                .flatMap { (1..it).map { Feedback.Pin.COLOUR_HIT } }
        }

        private fun List<Code.Pin>.countPins() = groupBy { pin -> pin }.mapValues { it.value.size }

        private fun Code.diff(guess: Code) = pins.zip(guess.pins).filter { it.first != it.second }.unzip()
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
        apply(GameStarted(id, secret, moves))
    }

    override fun makeGuess(guess: Code) = when {
        state.isGuessSuccessful(guess) -> apply(
            GuessMade(id, guess, giveFeedback(guess)),
            GameWon(id)
        )
        state.hasLastMoveLeft() -> apply(
            GuessMade(id, guess, giveFeedback(guess)),
            GameLost(id)
        )
        else -> apply(
            GuessMade(id, guess, giveFeedback(guess))
        )
    }

    private fun giveFeedback(guess: Code): Feedback {
        val exactHits = state.exactHits(guess)
        val colourHits = state.colourHits(guess)
        return Feedback(exactHits + colourHits)
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

    override fun streamName() = id.toString()

    private fun apply(vararg events: GameEvent) = apply(events.toList())
}