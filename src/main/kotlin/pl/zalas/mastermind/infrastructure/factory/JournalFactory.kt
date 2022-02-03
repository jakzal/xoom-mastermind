package pl.zalas.mastermind.infrastructure.factory

import io.vlingo.xoom.actors.Definition
import io.vlingo.xoom.actors.Protocols
import io.vlingo.xoom.actors.Stage
import io.vlingo.xoom.lattice.model.DomainEvent
import io.vlingo.xoom.lattice.model.projection.ProjectionDispatcher
import io.vlingo.xoom.lattice.model.projection.TextProjectionDispatcherActor
import io.vlingo.xoom.lattice.model.sourcing.Sourced
import io.vlingo.xoom.lattice.model.sourcing.SourcedTypeRegistry
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
import io.vlingo.xoom.symbio.store.journal.Journal
import io.vlingo.xoom.symbio.store.journal.inmemory.InMemoryJournalActor
import io.vlingo.xoom.symbio.store.journal.jdbc.JDBCDispatcherControlDelegate
import io.vlingo.xoom.symbio.store.journal.jdbc.JDBCJournalActor
import io.vlingo.xoom.symbio.store.journal.jdbc.JDBCJournalInstantWriter
import io.vlingo.xoom.symbio.store.state.StateStore
import org.postgresql.Driver
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration.PostgreSQLConfiguration
import pl.zalas.mastermind.model.GameEntity
import pl.zalas.mastermind.model.GameEvent
import pl.zalas.mastermind.view.DecodingBoardProjectionActor
import java.util.*

class JournalFactory(private val stage: Stage, private val configuration: JournalConfiguration) {
    sealed class JournalConfiguration {
        object InMemoryConfiguration : JournalConfiguration()
        data class PostgreSQLConfiguration(
            val username: String,
            val password: String,
            val database: String,
            val hostname: String = "[::1]",
            val port: Int = 5432,
            val useSsl: Boolean = false
        ) : JournalConfiguration()
    }

    fun createJournal(store: StateStore): Journal<DomainEvent> {
        val decodingBoardProjectionDescription = ProjectionDispatcher.ProjectToDescription.with(
            DecodingBoardProjectionActor::class.java,
            Optional.of<Any>(store),
            GameEvent.GameStarted::class.java,
            GameEvent.GuessMade::class.java
        )
        val descriptions = listOf(
            decodingBoardProjectionDescription
        )
        val dispatcherProtocols = stage.actorFor(
            arrayOf(Dispatcher::class.java, ProjectionDispatcher::class.java),
            Definition.has(TextProjectionDispatcherActor::class.java, listOf(descriptions))
        )
        val dispatchers =
            Protocols.two<Dispatcher<Dispatchable<out Entry<*>, out State<*>>>, ProjectionDispatcher>(
                dispatcherProtocols
            )

        return createJournal(dispatchers._1)
    }

    fun createJournal(dispatcher: Dispatcher<Dispatchable<out Entry<*>, out State<*>>>): Journal<DomainEvent> {
        val journal = when (configuration) {
            is InMemoryConfiguration -> inMemory(dispatcher)
            is PostgreSQLConfiguration -> postgreSQL(configuration, dispatcher)
        }

        val registry = SourcedTypeRegistry(stage.world())
        @Suppress("UNCHECKED_CAST")
        registry.register(
            SourcedTypeRegistry.Info(
                journal,
                GameEntity::class.java as Class<Sourced<DomainEvent>>,
                GameEntity::class.java.simpleName
            )
        )
        return journal
    }

    private fun postgreSQL(
        configuration: PostgreSQLConfiguration,
        dispatcher: Dispatcher<Dispatchable<out Entry<*>, out State<*>>>
    ): Journal<DomainEvent> =
        with(
            Configuration(
                DatabaseType.Postgres,
                PostgresConfigurationProvider.interest,
                Driver::class.java.name,
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
            @Suppress("UNCHECKED_CAST")
            stage.actorFor(
                Journal::class.java,
                JDBCJournalActor::class.java,
                this,
                JDBCJournalInstantWriter(
                    this,
                    listOf(dispatcher as Dispatcher<Dispatchable<Entry<String>, State.TextState>>),
                    dispatcherControl(dispatcher, this)
                )
            ) as Journal<DomainEvent>
        }

    private fun inMemory(dispatcher: Dispatcher<Dispatchable<out Entry<*>, out State<*>>>): Journal<DomainEvent> =
        Journal.using(stage, InMemoryJournalActor::class.java, listOf(dispatcher))

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