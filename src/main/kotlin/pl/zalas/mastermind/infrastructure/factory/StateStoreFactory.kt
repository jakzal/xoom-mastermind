package pl.zalas.mastermind.infrastructure.factory

import io.vlingo.xoom.actors.Definition
import io.vlingo.xoom.actors.Logger
import io.vlingo.xoom.actors.Stage
import io.vlingo.xoom.lattice.model.stateful.StatefulTypeRegistry
import io.vlingo.xoom.symbio.Entry
import io.vlingo.xoom.symbio.State
import io.vlingo.xoom.symbio.store.DataFormat
import io.vlingo.xoom.symbio.store.common.jdbc.Configuration
import io.vlingo.xoom.symbio.store.common.jdbc.DatabaseType
import io.vlingo.xoom.symbio.store.common.jdbc.postgres.PostgresConfigurationProvider
import io.vlingo.xoom.symbio.store.dispatch.Dispatchable
import io.vlingo.xoom.symbio.store.dispatch.Dispatcher
import io.vlingo.xoom.symbio.store.dispatch.DispatcherControl
import io.vlingo.xoom.symbio.store.dispatch.control.DispatcherControlActor
import io.vlingo.xoom.symbio.store.journal.jdbc.JDBCDispatcherControlDelegate
import io.vlingo.xoom.symbio.store.state.StateStore
import io.vlingo.xoom.symbio.store.state.StateTypeStateStoreMap
import io.vlingo.xoom.symbio.store.state.inmemory.InMemoryStateStoreActor
import io.vlingo.xoom.symbio.store.state.jdbc.JDBCEntriesInstantWriter
import io.vlingo.xoom.symbio.store.state.jdbc.JDBCStateStoreActor
import io.vlingo.xoom.symbio.store.state.jdbc.JDBCStorageDelegate
import io.vlingo.xoom.symbio.store.state.jdbc.postgres.PostgresStorageDelegate
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration.PostgreSQLConfiguration
import pl.zalas.mastermind.view.DecodingBoard

class StateStoreFactory(
    private val stage: Stage,
    private val logger: Logger,
    private val configuration: StateStoreConfiguration
) {
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
            is PostgreSQLConfiguration -> with(
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
                )
            ) {
                val postgresStorageDelegate = PostgresStorageDelegate(this, logger)
                stage.actorFor(
                    StateStore::class.java,
                    JDBCStateStoreActor::class.java,
                    postgresStorageDelegate,
                    JDBCEntriesInstantWriter(
                        postgresStorageDelegate as JDBCStorageDelegate<State.TextState>,
                        listOf(dispatcher as Dispatcher<Dispatchable<out Entry<*>, out State<*>>>),
                        dispatcherControl(dispatcher, this)
                    )
                )
            }
        }
        val registry = StatefulTypeRegistry(stage.world())
        registry.register(
            StatefulTypeRegistry.Info(store, DecodingBoard::class.java, DecodingBoard::class.java.simpleName)
        )
        return store
    }

    private fun dispatcherControl(
        dispatcher: Dispatcher<Dispatchable<out Entry<*>, out State<*>>>,
        configuration: Configuration
    ) = stage.actorFor(
        DispatcherControl::class.java,
        Definition.has(
            DispatcherControlActor::class.java,
            DispatcherControl.DispatcherControlInstantiator<Entry<*>, State<*>>(
                listOf(dispatcher),
                JDBCDispatcherControlDelegate(Configuration.cloneOf(configuration), stage.world().defaultLogger()),
                StateStore.DefaultCheckConfirmationExpirationInterval,
                StateStore.DefaultConfirmationExpiration
            )
        )
    )
}