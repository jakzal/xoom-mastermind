package pl.zalas.mastermind.model

import java.util.*

data class GameId(val id: String) {

    companion object {
        fun generate(): GameId = GameId(UUID.randomUUID().toString())
    }

    override fun toString(): String = id
}
