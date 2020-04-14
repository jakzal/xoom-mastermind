package pl.zalas.mastermind.view

import io.vlingo.common.Completes
import io.vlingo.lattice.query.StateStoreQueryActor
import io.vlingo.symbio.store.state.StateStore

class DecodingBoardQueryActor(store: StateStore) : DecodingBoardQuery, StateStoreQueryActor(store) {
    override fun getDecodingBoardForGame(gameId: String): Completes<DecodingBoard>
            = queryStateFor(gameId, DecodingBoard::class.java)
}