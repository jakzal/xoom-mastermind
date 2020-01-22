package pl.zalas.mastermind

data class Code(val pins: List<Pin>) {
    enum class Pin {
        GREEN, BLUE, YELLOW, RED, PURPLE
    }

    constructor(vararg pins: Pin) : this(pins.asList())

    fun exactHits(guess: Code) = pins
        .zip(guess.pins)
        .filter { it.first == it.second }
        .size

    fun colourHits(guess: Code) = diff(guess).run {
        val secretColours = first.countPins()
        val guessColours = second.countPins()
        secretColours
            .mapValues { minOf(it.value, guessColours.getOrDefault(it.key, 0)) }
            .values
            .sum()
    }

    private fun diff(guess: Code) = pins.zip(guess.pins).filter { it.first != it.second }.unzip()

    private fun List<Pin>.countPins() = groupBy { pin -> pin }.mapValues { it.value.size }
}