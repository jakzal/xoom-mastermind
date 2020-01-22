package pl.zalas.mastermind

data class Code(val pins: List<Pin>) {
    enum class Pin {
        GREEN, BLUE, YELLOW, RED, PURPLE
    }

    constructor(vararg pins: Pin) : this(pins.asList())
}