package pl.zalas.mastermind.view

import io.vlingo.common.Completes

interface DecodingBoardQuery {
    fun findDecodingBoardForGame(gameId: String): Completes<DecodingBoard?>
}