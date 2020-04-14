package pl.zalas.mastermind.view

import io.vlingo.lattice.model.projection.Projectable
import io.vlingo.lattice.model.projection.StateStoreProjectionActor
import io.vlingo.symbio.Entry
import io.vlingo.symbio.store.state.StateStore
import pl.zalas.mastermind.model.Code.CodePeg
import pl.zalas.mastermind.model.Feedback.KeyPeg
import pl.zalas.mastermind.model.GameEvent
import pl.zalas.mastermind.model.GameEvent.GameStarted
import pl.zalas.mastermind.model.GameEvent.GuessMade
import pl.zalas.mastermind.view.DecodingBoard.Move

class DecodingBoardProjectionActor(store: StateStore) : StateStoreProjectionActor<DecodingBoard>(store) {

    private var gameId: String = ""

    class OutOfSequenceException(previous: Int, current: Int) :
        RuntimeException("Out of sequence event received in projection ($previous -> $current).")

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
    ): DecodingBoard {
        if (isOutOfSequence(previousVersion, currentVersion)) {
            throw OutOfSequenceException(previousVersion, currentVersion)
        }
        return when (previousData) {
            is DecodingBoard -> DecodingBoard(previousData.gameId, previousData.maxMoves, previousData.moves + currentData.moves)
            else -> currentData
        }
    }

    private fun isOutOfSequence(previousVersion: Int, currentVersion: Int) =
        (previousVersion != -1 && currentVersion - previousVersion != 1) || (previousVersion == -1 && currentVersion != 1)

    private fun captureGameId(block: () -> DecodingBoard): DecodingBoard {
        gameId = ""
        return block().also { board ->
            gameId = board.gameId
        }
    }

    private fun mapToGameEvent(entry: Entry<*>): GameEvent? = when (entry.typeName()) {
        GameStarted::class.java.name -> fromEntry(entry) as? GameStarted
        GuessMade::class.java.name -> fromEntry(entry) as? GuessMade
        else -> throw Exception("Unexpected type ${entry.typeName()}")
    }

    private fun fromEntry(entry: Entry<*>) = entryAdapter<GameEvent, Entry<*>>().fromEntry(entry)

    private fun createBoard(e: GameStarted) = DecodingBoard(e.id.toString(), e.moves, emptyList())

    private fun placeMoveOnTheBoard(e: GuessMade): DecodingBoard {
        val code = e.guess.pegs.map(CodePeg::toString)
        val feedback = e.feedback.pegs.map(KeyPeg::toString)
        return DecodingBoard(e.id.toString(), 0, listOf(Move(code, feedback)))
    }
}