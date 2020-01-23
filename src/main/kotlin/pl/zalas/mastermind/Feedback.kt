package pl.zalas.mastermind

data class Feedback(val pegs: List<Peg>) {
    enum class Peg {
        BLACK, WHITE
    }

    constructor(vararg pegs: Peg) : this(pegs.asList())

    companion object {
        fun give(secret: Code, guess: Code): Feedback {
            val exactHits = (1..secret.exactHits(guess)).map { Peg.BLACK }
            val colourHits = (1..secret.colourHits(guess)).map { Peg.WHITE }
            return Feedback(exactHits + colourHits)
        }
    }
}