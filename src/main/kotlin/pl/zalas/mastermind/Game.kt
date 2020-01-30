package pl.zalas.mastermind

import io.vlingo.common.Completes
import io.vlingo.common.Outcome

typealias FeedbackOutcome = Outcome<Game.GameException, Feedback>
typealias CompletesWithFeedbackOutcome = Completes<FeedbackOutcome>

interface Game {
    sealed class GameException(message: String, cause: Throwable? = null) : Throwable(message, cause) {
        class IncompleteCode(expectedSize: Int, actualSize: Int)
            : GameException("The code is $actualSize colours long but expected it to be $expectedSize.")
    }

    fun startGame(secret: Code, moves: Int)

    fun makeGuess(guess: Code): CompletesWithFeedbackOutcome
}
