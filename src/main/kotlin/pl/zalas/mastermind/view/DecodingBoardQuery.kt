package pl.zalas.mastermind.view

import io.vlingo.xoom.common.Completes

interface DecodingBoardQuery {
    fun findDecodingBoardForGame(gameId: String): Completes<DecodingBoard>
}