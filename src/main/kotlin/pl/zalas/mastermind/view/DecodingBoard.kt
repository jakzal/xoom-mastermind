package pl.zalas.mastermind.view

typealias Code = List<String>

typealias Feedback = List<String>

data class DecodingBoard(val gameId: String, val maxMoves: Int, val moves: List<Move>) {
    data class Move(val guess: Code, val feedback: Feedback)
}
