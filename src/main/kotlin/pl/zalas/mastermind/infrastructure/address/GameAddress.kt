package pl.zalas.mastermind.infrastructure.address

import io.vlingo.actors.Address
import pl.zalas.mastermind.model.GameId
import java.util.*

/**
 * Vlingo decides whether to use the uuid if it's run on the grid (is distributable).
 * Otherwise an integer id is used.
 * GameAddress was created to workaround this issue and be able to use uuid without the grid.
 */
data class GameAddress(val gameId: GameId) : Address {

    override fun idString() = gameId.id

    override fun id() = UUID.fromString(gameId.id).mostSignificantBits and Long.MAX_VALUE

    override fun idSequenceString() = idString()

    override fun compareTo(other: Address?) = when (other) {
        is GameAddress -> if (gameId == other.gameId) 0 else 1
        else -> -1
    }

    override fun idSequence() = id()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> idTyped(): T? = idString() as? T

    override fun name(): String = "game"

    override fun isDistributable() = true
}