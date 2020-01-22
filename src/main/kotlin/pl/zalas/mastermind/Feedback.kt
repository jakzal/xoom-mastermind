package pl.zalas.mastermind

data class Feedback(val pins: List<Pin>) {
    enum class Pin {
        EXACT_HIT, COLOUR_HIT
    }

    constructor(vararg pins: Pin) : this(pins.asList())
}