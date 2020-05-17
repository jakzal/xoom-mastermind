package pl.zalas.mastermind.infrastructure.factory

import io.vlingo.actors.Logger
import io.vlingo.actors.Stage
import io.vlingo.lattice.model.stateful.StatefulTypeRegistry
import io.vlingo.symbio.store.dispatch.Dispatcher
import io.vlingo.symbio.store.state.StateStore
import io.vlingo.symbio.store.state.StateTypeStateStoreMap
import io.vlingo.symbio.store.state.inmemory.InMemoryStateStoreActor
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.view.DecodingBoard

class StateStoreFactory(private val stage: Stage, private val logger: Logger, private val configuration: StateStoreConfiguration) {
    sealed class StateStoreConfiguration {
        object InMemoryConfiguration : StateStoreConfiguration()
    }

    fun createStateStore(dispatcher: Dispatcher<*>): StateStore {
        StateTypeStateStoreMap.stateTypeToStoreName(DecodingBoard::class.java, DecodingBoard::class.simpleName);

        val store = when (configuration) {
            is InMemoryConfiguration -> stage.actorFor(
                StateStore::class.java,
                InMemoryStateStoreActor::class.java,
                listOf(dispatcher)
            )
        }
        val registry = StatefulTypeRegistry(stage.world())
        registry.register(
            StatefulTypeRegistry.Info(store, DecodingBoard::class.java, DecodingBoard::class.java.simpleName)
        )
        return store
    }
}