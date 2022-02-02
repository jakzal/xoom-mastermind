package pl.zalas.mastermind.test

import io.vlingo.xoom.actors.testkit.AccessSafely
import io.vlingo.xoom.symbio.Entry
import io.vlingo.xoom.symbio.State
import io.vlingo.xoom.symbio.store.Result
import io.vlingo.xoom.symbio.store.dispatch.Dispatchable
import io.vlingo.xoom.symbio.store.dispatch.Dispatcher
import io.vlingo.xoom.symbio.store.dispatch.DispatcherControl

class FakeStateStoreDispatcher : Dispatcher<Dispatchable<Entry<String>, State.TextState>> {
    private val states: MutableList<State.TextState> = mutableListOf()
    private var control: DispatcherControl? = null
    private var access = AccessSafely.immediately()
        .writingWith("registerState") { event: State.TextState -> states.add(event) }
        .readingWith("states") { -> states }


    override fun controlWith(control: DispatcherControl) {
        this.control = control
    }

    override fun dispatch(dispatchable: Dispatchable<Entry<String>, State.TextState>) {
        dispatchable.state().ifPresent { s ->
            access.writeUsing("registerState", s)
        }
        this.control?.confirmDispatched(dispatchable.id()) { _: Result, _: String -> }
    }

    fun updateExpectedEventHappenings(times: Int) {
        access = access.resetAfterCompletingTo(times)
    }

    fun states(): List<State.TextState> = access.readFrom("states")
}