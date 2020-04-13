package pl.zalas.mastermind.view

import io.vlingo.actors.Definition
import io.vlingo.actors.Protocols
import io.vlingo.actors.World
import io.vlingo.common.Completes
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.projection.ProjectionDispatcher
import io.vlingo.lattice.model.projection.ProjectionDispatcher.ProjectToDescription
import io.vlingo.lattice.model.projection.TextProjectionDispatcherActor
import io.vlingo.lattice.model.sourcing.Sourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.lattice.model.stateful.StatefulTypeRegistry
import io.vlingo.symbio.Entry
import io.vlingo.symbio.State
import io.vlingo.symbio.store.dispatch.Dispatchable
import io.vlingo.symbio.store.dispatch.Dispatcher
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.inmemory.InMemoryJournalActor
import io.vlingo.symbio.store.state.StateStore
import io.vlingo.symbio.store.state.StateTypeStateStoreMap
import io.vlingo.symbio.store.state.inmemory.InMemoryStateStoreActor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.zalas.mastermind.model.*
import pl.zalas.mastermind.model.Code
import pl.zalas.mastermind.model.Code.CodePeg.*
import pl.zalas.mastermind.test.FakeStateStoreDispatcher
import pl.zalas.mastermind.view.DecodingBoard.Move
import java.util.*

class DecodingBoardTests {
    private lateinit var world: World

    private lateinit var store: StateStore

    private val storeDispatcher = FakeStateStoreDispatcher<DomainEvent, State.TextState>()

    @BeforeEach
    fun startWorld() {
        world = World.startWithDefaults("mastermind")
        store = createStateStore(world, storeDispatcher)

        createJournal(world, store)
    }

    @AfterEach
    fun stopWorld() {
        world.terminate()
    }

    @Test
    fun `game state is created from game events`() {
        val gameId = GameId.generate()
        val secret = Code(RED, BLUE, YELLOW, BLUE)
        val game = world.actorFor(Game::class.java, GameEntity::class.java, gameId)

        waitForEvents(3) {
            game.startGame(secret, 12)
            game.makeGuess(Code(RED, RED, GREEN, ORANGE))
            game.makeGuess(Code(BLUE, RED, GREEN, ORANGE))
        }

        waitFor(decodingBoardQuery().getDecodingBoardForGame(gameId.toString())) { decodingBoard ->
            assertEquals(
                DecodingBoard(gameId.id, 12, listOf(
                    Move(listOf("RED", "RED", "GREEN", "ORANGE"), listOf("BLACK")),
                    Move(listOf("BLUE", "RED", "GREEN", "ORANGE"), listOf("WHITE", "WHITE"))
                )),
                decodingBoard
            )
        }
    }

    private fun decodingBoardQuery() =
        world.actorFor(DecodingBoardQuery::class.java, DecodingBoardQueryActor::class.java, store)

    private fun createJournal(world: World, store: StateStore): Journal<DomainEvent> {
        val decodingBoardProjectionDescription = ProjectToDescription.with(
            DecodingBoardProjectionActor::class.java,
            Optional.of<Any>(store),
            GameEvent.GameStarted::class.java,
            GameEvent.GuessMade::class.java
        )
        val descriptions = listOf(
            decodingBoardProjectionDescription
        )
        val dispatcherProtocols = world.actorFor(
            arrayOf(Dispatcher::class.java, ProjectionDispatcher::class.java),
            Definition.has(TextProjectionDispatcherActor::class.java, listOf(descriptions))
        )
        val dispatchers =
            Protocols.two<Dispatcher<Dispatchable<Entry<DomainEvent>, State.TextState>>, ProjectionDispatcher>(
                dispatcherProtocols
            )

        val journal = Journal.using(world.stage(), InMemoryJournalActor::class.java, dispatchers._1)
        val registry = SourcedTypeRegistry(world)
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

    private fun createStateStore(world: World, dispatcher: Dispatcher<Dispatchable<Entry<DomainEvent>, State.TextState>>): StateStore {
        StateTypeStateStoreMap.stateTypeToStoreName(DecodingBoard::class.java, DecodingBoard::class.simpleName);

        val store = world.actorFor(StateStore::class.java, InMemoryStateStoreActor::class.java, listOf(dispatcher))
        val registry = StatefulTypeRegistry(world)
        registry.register(
            StatefulTypeRegistry.Info(store, DecodingBoard::class.java, DecodingBoard::class.java.simpleName)
        )
        return store
    }

    private fun waitForEvents(number: Int, block: () -> Unit) {
        storeDispatcher.updateExpectedEventHappenings(number)
        block()
        assertEquals(number, storeDispatcher.states().size)
    }

    private fun <T> waitFor(completes: Completes<T>, block: (t: T) -> Unit) = block(completes.await())
}