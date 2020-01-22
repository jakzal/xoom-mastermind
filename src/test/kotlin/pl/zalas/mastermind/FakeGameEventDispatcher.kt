package pl.zalas.mastermind

import io.vlingo.actors.testkit.TestUntil
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.symbio.DefaultTextEntryAdapter
import io.vlingo.symbio.Entry
import io.vlingo.symbio.State
import io.vlingo.symbio.store.Result
import io.vlingo.symbio.store.dispatch.Dispatchable
import io.vlingo.symbio.store.dispatch.Dispatcher
import io.vlingo.symbio.store.dispatch.DispatcherControl
import pl.zalas.mastermind.GameEvent.*


class FakeGameEventDispatcher : Dispatcher<Dispatchable<Entry<String>, State<String>>> {
    private val testUntil = TestUntil.happenings(1)
    private val eventAdapter = DefaultTextEntryAdapter()
    private val events: MutableList<DomainEvent> = mutableListOf()
    private var control: DispatcherControl? = null

    override fun controlWith(control: DispatcherControl) {
        this.control = control
    }

    override fun dispatch(dispatchable: Dispatchable<Entry<String>, State<String>>) {
        dispatchable.entries().mapNotNull(::mapToGameEvent).apply {
            events.addAll(this)
        }
        testUntil.happened()
        this.control?.confirmDispatched(dispatchable.id()) { _: Result, _: String -> }
    }

    private fun mapToGameEvent(entry: Entry<*>): GameEvent? = when (entry.typeName()) {
        GameStarted::class.java.name -> eventAdapter.fromEntry(entry) as? GameStarted
        GuessMade::class.java.name -> eventAdapter.fromEntry(entry) as? GuessMade
        GameWon::class.java.name -> eventAdapter.fromEntry(entry) as? GameWon
        GameLost::class.java.name -> eventAdapter.fromEntry(entry) as? GameLost
        else -> throw RuntimeException("Unexpected event type ${entry.typeName()}")
    }

    fun events(): List<DomainEvent> {
        testUntil.completesWithin(2000)
        return events
    }

    fun updateExpectedEventHappenings(times: Int) {
        val happenings = times - events.size
        if (happenings > 0) {
            testUntil.resetHappeningsTo(happenings)
        }
    }
}