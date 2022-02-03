package pl.zalas.mastermind.view

import io.vlingo.xoom.common.Completes
import io.vlingo.xoom.lattice.query.StateStoreQueryActor
import io.vlingo.xoom.symbio.store.state.StateStore

class DecodingBoardQueryActor(store: StateStore) : DecodingBoardQuery, StateStoreQueryActor(store) {
    override fun findDecodingBoardForGame(gameId: String): Completes<DecodingBoard> =
        queryStateFor(gameId, DecodingBoard::class.java, DecodingBoard.NOT_FOUND)
}