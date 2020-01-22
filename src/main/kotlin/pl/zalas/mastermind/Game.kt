package pl.zalas.mastermind

interface Game {
    fun startGame(secret: Code, moves: Int)

    fun makeGuess(guess: Code)
}
