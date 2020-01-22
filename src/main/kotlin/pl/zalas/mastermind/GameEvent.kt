package pl.zalas.mastermind

import pl.zalas.mastermind.Game.Code

sealed class GameEvent {
    data class GameStarted(val gameId: GameId, val secret: Code) : GameEvent()
    data class GuessMade(val gameId: GameId, val guess: Code) : GameEvent()
    data class GameWon(val gameId: GameId) : GameEvent()
    data class GameLost(val gameId: GameId) : GameEvent()
}
