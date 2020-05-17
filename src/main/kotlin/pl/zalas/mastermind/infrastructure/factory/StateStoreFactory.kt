package pl.zalas.mastermind.infrastructure.factory

import io.vlingo.actors.Logger
import io.vlingo.actors.Stage
import io.vlingo.lattice.model.stateful.StatefulTypeRegistry
import io.vlingo.symbio.store.DataFormat
import io.vlingo.symbio.store.common.jdbc.Configuration
import io.vlingo.symbio.store.common.jdbc.DatabaseType
import io.vlingo.symbio.store.common.jdbc.postgres.PostgresConfigurationProvider
import io.vlingo.symbio.store.dispatch.Dispatcher
import io.vlingo.symbio.store.state.StateStore
import io.vlingo.symbio.store.state.StateTypeStateStoreMap
import io.vlingo.symbio.store.state.inmemory.InMemoryStateStoreActor
import io.vlingo.symbio.store.state.jdbc.JDBCStateStoreActor
import io.vlingo.symbio.store.state.jdbc.postgres.PostgresStorageDelegate
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration.PostgreSQLConfiguration
import pl.zalas.mastermind.view.DecodingBoard

class StateStoreFactory(private val stage: Stage, private val logger: Logger, private val configuration: StateStoreConfiguration) {
    sealed class StateStoreConfiguration {
        object InMemoryConfiguration : StateStoreConfiguration()
        data class PostgreSQLConfiguration(
            val username: String,
            val password: String,
            val database: String,
            val hostname: String = "[::1]",
            val port: Int = 5432,
            val useSsl: Boolean = false
        ) : StateStoreConfiguration()
    }

    fun createStateStore(dispatcher: Dispatcher<*>): StateStore {
        StateTypeStateStoreMap.stateTypeToStoreName(DecodingBoard::class.java, DecodingBoard::class.simpleName);

        val store = when (configuration) {
            is InMemoryConfiguration -> stage.actorFor(
                StateStore::class.java,
                InMemoryStateStoreActor::class.java,
                listOf(dispatcher)
            )
            is PostgreSQLConfiguration -> stage.actorFor(
                StateStore::class.java,
                JDBCStateStoreActor::class.java,
                listOf(dispatcher),
                PostgresStorageDelegate(
                    Configuration(
                        DatabaseType.Postgres,
                        PostgresConfigurationProvider.interest,
                        org.postgresql.Driver::class.java.name,
                        DataFormat.Text,
                        "jdbc:postgresql://${configuration.hostname}:${configuration.port}/",
                        configuration.database,
                        configuration.username,
                        configuration.password,
                        configuration.useSsl,
                        "",
                        true
                    ), logger)
            )
        }
        val registry = StatefulTypeRegistry(stage.world())
        registry.register(
            StatefulTypeRegistry.Info(store, DecodingBoard::class.java, DecodingBoard::class.java.simpleName)
        )
        return store
    }
}