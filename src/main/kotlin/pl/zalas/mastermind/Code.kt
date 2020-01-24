package pl.zalas.mastermind

data class Code(val pegs: List<CodePeg>) {
    enum class CodePeg {
        GREEN, BLUE, YELLOW, RED, PURPLE, ORANGE
    }

    constructor(vararg pegs: CodePeg) : this(pegs.asList())

    fun matches(guess: Code) = pegs == guess.pegs

    fun exactHits(guess: Code) = pegs
        .zip(guess.pegs)
        .filter { it.first == it.second }
        .size

    fun colourHits(guess: Code) = diff(guess).run {
        val secretColours = first.countPegs()
        val guessColours = second.countPegs()
        secretColours
            .mapValues { minOf(it.value, guessColours.getOrDefault(it.key, 0)) }
            .values
            .sum()
    }

    private fun diff(guess: Code) = pegs.zip(guess.pegs).filter { it.first != it.second }.unzip()

    private fun List<CodePeg>.countPegs() = groupBy { peg -> peg }.mapValues { it.value.size }
}