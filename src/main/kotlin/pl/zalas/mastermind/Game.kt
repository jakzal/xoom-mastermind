package pl.zalas.mastermind

import io.vlingo.common.Completes
import io.vlingo.common.Outcome

typealias FeedbackOutcome = Outcome<Game.GameException, Feedback>
typealias CompletesWithFeedbackOutcome = Completes<FeedbackOutcome>

interface Game {
    sealed class GameException : Throwable()

    fun startGame(secret: Code, moves: Int)

    fun makeGuess(guess: Code): CompletesWithFeedbackOutcome
}
