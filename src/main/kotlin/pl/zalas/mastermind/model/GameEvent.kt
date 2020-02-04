package pl.zalas.mastermind.model

import io.vlingo.lattice.model.DomainEvent

sealed class GameEvent : DomainEvent() {
    abstract val id: GameId

    data class GameStarted(override val id: GameId, val secret: Code, val moves: Int) : GameEvent()
    data class GuessMade(override val id: GameId, val guess: Code, val feedback: Feedback) : GameEvent()
}
