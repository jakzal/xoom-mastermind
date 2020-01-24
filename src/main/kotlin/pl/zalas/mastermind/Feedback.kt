package pl.zalas.mastermind

data class Feedback(val pegs: List<KeyPeg>) {
    enum class KeyPeg {
        BLACK, WHITE
    }

    constructor(vararg pegs: KeyPeg) : this(pegs.asList())

    companion object {
        fun give(secret: Code, guess: Code): Feedback {
            val exactHits = (1..secret.exactHits(guess)).map { KeyPeg.BLACK }
            val colourHits = (1..secret.colourHits(guess)).map { KeyPeg.WHITE }
            return Feedback(exactHits + colourHits)
        }
    }
}