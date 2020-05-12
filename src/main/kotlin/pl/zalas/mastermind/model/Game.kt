package pl.zalas.mastermind.model

import io.vlingo.common.Completes
import io.vlingo.common.Outcome

typealias FeedbackOutcome = Outcome<GameError, Feedback>
typealias CompletesWithFeedbackOutcome = Completes<FeedbackOutcome>

interface Game {

    fun startGame(codeMaker: CodeMaker, moves: Int)

    fun makeGuess(guess: Code): CompletesWithFeedbackOutcome
}
