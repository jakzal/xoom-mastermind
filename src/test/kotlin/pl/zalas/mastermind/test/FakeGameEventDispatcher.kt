package pl.zalas.mastermind.test

import io.vlingo.xoom.actors.testkit.AccessSafely
import io.vlingo.xoom.lattice.model.DomainEvent
import io.vlingo.xoom.symbio.DefaultTextEntryAdapter
import io.vlingo.xoom.symbio.Entry
import io.vlingo.xoom.symbio.State
import io.vlingo.xoom.symbio.store.Result
import io.vlingo.xoom.symbio.store.dispatch.Dispatchable
import io.vlingo.xoom.symbio.store.dispatch.Dispatcher
import io.vlingo.xoom.symbio.store.dispatch.DispatcherControl
import pl.zalas.mastermind.model.GameEvent

class FakeGameEventDispatcher : Dispatcher<Dispatchable<Entry<DomainEvent>, State.TextState>> {
    private val eventAdapter = DefaultTextEntryAdapter()
    private val entries: MutableList<Entry<DomainEvent>> = mutableListOf()
    private var control: DispatcherControl? = null
    private var access = AccessSafely.immediately()
        .writingWith("registerEntry") { entry: Entry<DomainEvent> -> entries.add(entry) }
        .readingWith("entries") { -> entries.toList() }

    override fun controlWith(control: DispatcherControl) {
        this.control = control
    }

    override fun dispatch(dispatchable: Dispatchable<Entry<DomainEvent>, State.TextState>) {
        dispatchable.entries().forEach {
            access.writeUsing("registerEntry", it)
        }
        this.control?.confirmDispatched(dispatchable.id()) { _: Result, _: String -> }
    }

    fun entries(): List<Entry<DomainEvent>> = access.readFrom("entries")

    fun events(): List<GameEvent> = entries().mapNotNull(::mapToGameEvent)

    fun updateExpectedEventHappenings(times: Int) {
        access = access.resetAfterCompletingTo(times)
    }

    private fun mapToGameEvent(entry: Entry<DomainEvent>): GameEvent? = eventAdapter.fromEntry(entry) as? GameEvent
}