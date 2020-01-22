package pl.zalas.mastermind

data class Feedback(val pins: List<Pin>) {
    enum class Pin {
        EXACT_HIT, COLOUR_HIT
    }

    constructor(vararg pins: Pin) : this(pins.asList())

    companion object {
        fun give(secret: Code, guess: Code): Feedback {
            val exactHits = (1..secret.exactHits(guess)).map { Pin.EXACT_HIT }
            val colourHits = (1..secret.colourHits(guess)).map { Pin.COLOUR_HIT }
            return Feedback(exactHits + colourHits)
        }
    }
}