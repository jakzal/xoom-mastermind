package pl.zalas.mastermind.view

import io.vlingo.xoom.lattice.model.projection.Projectable
import io.vlingo.xoom.lattice.model.projection.StateStoreProjectionActor
import io.vlingo.xoom.symbio.Entry
import io.vlingo.xoom.symbio.store.state.StateStore
import pl.zalas.mastermind.model.Code.CodePeg
import pl.zalas.mastermind.model.Feedback.KeyPeg
import pl.zalas.mastermind.model.GameEvent
import pl.zalas.mastermind.model.GameEvent.GameStarted
import pl.zalas.mastermind.model.GameEvent.GuessMade
import pl.zalas.mastermind.view.DecodingBoard.Move

class DecodingBoardProjectionActor(store: StateStore) : StateStoreProjectionActor<DecodingBoard>(store) {

    private var gameId: String = ""

    override fun currentDataFor(projectable: Projectable): DecodingBoard = captureGameId {
        projectable.entries()
            .mapNotNull(::mapToGameEvent)
            .first() // we only expect one entry
            .let { e ->
                when (e) {
                    is GameStarted -> createBoard(e)
                    is GuessMade -> placeMoveOnTheBoard(e)
                }
            }
    }

    override fun dataIdFor(projectable: Projectable): String = gameId

    override fun merge(
        previousData: DecodingBoard?,
        previousVersion: Int,
        currentData: DecodingBoard,
        currentVersion: Int
    ): DecodingBoard = when (previousData) {
        is DecodingBoard -> DecodingBoard(
            previousData.gameId,
            previousData.maxMoves,
            previousData.moves + currentData.moves
        )
        else -> currentData
    }

    private fun captureGameId(block: () -> DecodingBoard): DecodingBoard {
        gameId = ""
        return block().also { board ->
            gameId = board.gameId
        }
    }

    private fun mapToGameEvent(entry: Entry<*>): GameEvent? = entryAdapter<GameEvent, Entry<*>>().fromEntry(entry)

    private fun createBoard(e: GameStarted) = DecodingBoard(e.id.toString(), e.moves, emptyList())

    private fun placeMoveOnTheBoard(e: GuessMade): DecodingBoard {
        val code = e.guess.pegs.map(CodePeg::toString)
        val feedback = e.feedback.pegs.map(KeyPeg::toString)
        return DecodingBoard(e.id.toString(), 0, listOf(Move(code, feedback)))
    }
}