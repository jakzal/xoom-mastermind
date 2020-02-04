package pl.zalas.mastermind.model

sealed class GameError(message: String, cause: Throwable? = null) : Throwable(message, cause) {

    class IncompleteCode(expectedSize: Int, actualSize: Int)
        : GameError("The code is $actualSize colours long but expected it to be $expectedSize.")

    class GameFinished() : GameError("The game has already finished.")
}