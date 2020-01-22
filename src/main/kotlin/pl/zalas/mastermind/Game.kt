package pl.zalas.mastermind

interface Game {
    interface Code

    fun startGame(secret: Code)

    fun makeGuess(guess: Code)
}
