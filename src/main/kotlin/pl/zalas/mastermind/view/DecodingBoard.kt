package pl.zalas.mastermind.view

data class DecodingBoard(val gameId: String, val maxMoves: Int, val moves: List<Move>) {
    data class Move(val guess: List<String>, val feedback: List<String>)

    companion object {
        val NOT_FOUND = DecodingBoard("", 0, emptyList())
    }

    fun isFound() = !gameId.isEmpty()
}
