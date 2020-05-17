package pl.zalas.mastermind.infrastructure.factory

import io.vlingo.actors.Definition
import io.vlingo.actors.Protocols
import io.vlingo.actors.Stage
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.projection.ProjectionDispatcher
import io.vlingo.lattice.model.projection.TextProjectionDispatcherActor
import io.vlingo.lattice.model.sourcing.Sourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.Entry
import io.vlingo.symbio.State
import io.vlingo.symbio.store.dispatch.Dispatchable
import io.vlingo.symbio.store.dispatch.Dispatcher
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.inmemory.InMemoryJournalActor
import io.vlingo.symbio.store.state.StateStore
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration.InMemoryConfiguration
import pl.zalas.mastermind.model.GameEntity
import pl.zalas.mastermind.model.GameEvent
import pl.zalas.mastermind.view.DecodingBoardProjectionActor
import java.util.*

class JournalFactory(private val stage: Stage, private val configuration: JournalConfiguration) {
    sealed class JournalConfiguration {
        object InMemoryConfiguration : JournalConfiguration()
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
            Protocols.two<Dispatcher<Dispatchable<Entry<DomainEvent>, State.TextState>>, ProjectionDispatcher>(
                dispatcherProtocols
            )

        return createJournal(dispatchers._1)
    }

    fun createJournal(dispatcher: Dispatcher<Dispatchable<Entry<DomainEvent>, State.TextState>>): Journal<DomainEvent> {
        val journal = when(configuration) {
            is InMemoryConfiguration -> Journal.using(stage, InMemoryJournalActor::class.java, dispatcher)
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
}