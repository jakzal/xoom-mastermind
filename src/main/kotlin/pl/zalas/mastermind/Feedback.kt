package pl.zalas.mastermind

data class Feedback(val pegs: List<KeyPeg>, val outcome: Outcome) {
    enum class KeyPeg {
        BLACK, WHITE
    }

    enum class Outcome {
        IN_PROGRESS, WON, LOST
    }

    companion object {
        fun won(vararg pegs: KeyPeg) = Feedback(pegs.toList(), Outcome.WON)
        fun won(pegs: List<KeyPeg>) = Feedback(pegs, Outcome.WON)
        fun won(secret: Code, guess: Code): Feedback = won(hits(secret, guess))

        fun lost(vararg pegs: KeyPeg) = Feedback(pegs.toList(), Outcome.LOST)
        fun lost(pegs: List<KeyPeg>) = Feedback(pegs, Outcome.LOST)
        fun lost(secret: Code, guess: Code): Feedback = lost(hits(secret, guess))

        fun inProgress(vararg pegs: KeyPeg) = Feedback(pegs.toList(), Outcome.IN_PROGRESS)
        fun inProgress(pegs: List<KeyPeg>) = Feedback(pegs, Outcome.IN_PROGRESS)
        fun inProgress(secret: Code, guess: Code): Feedback = inProgress(hits(secret, guess))

        private fun hits(secret: Code, guess: Code): List<KeyPeg> {
            val exactHits = (1..secret.exactHits(guess)).map { KeyPeg.BLACK }
            val colourHits = (1..secret.colourHits(guess)).map { KeyPeg.WHITE }
            return exactHits + colourHits
        }
    }
}