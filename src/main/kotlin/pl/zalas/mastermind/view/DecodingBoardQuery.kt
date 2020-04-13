package pl.zalas.mastermind.view

import io.vlingo.common.Completes

interface DecodingBoardQuery {
    fun getDecodingBoardForGame(gameId: String): Completes<DecodingBoard>
}