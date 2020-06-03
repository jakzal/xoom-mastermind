package pl.zalas.mastermind.infrastructure.http

import io.vlingo.actors.Definition
import io.vlingo.actors.Stage
import io.vlingo.common.Completes
import io.vlingo.common.Completes.withSuccess
import io.vlingo.http.Response.Status.*
import io.vlingo.http.ResponseHeader
import io.vlingo.http.ResponseHeader.Location
import io.vlingo.http.ResponseHeader.headers
import io.vlingo.http.resource.ObjectResponse
import io.vlingo.http.resource.Resource
import io.vlingo.http.resource.ResourceBuilder.*
import pl.zalas.mastermind.infrastructure.address.GameAddress
import pl.zalas.mastermind.model.*
import pl.zalas.mastermind.view.DecodingBoard
import pl.zalas.mastermind.view.DecodingBoardQuery

class GameResource(
    private val codeMaker: CodeMaker,
    private val decodingBoardQuery: DecodingBoardQuery,
    private val stage: Stage
) {
    private val logger = stage.world().defaultLogger()

    fun routes(): Resource<*> = resource(
        "mastermind game resource",
        post("/games").handle(::startGame),
        get("/games/{gameId}").param(String::class.java).handle(::viewGame),
        post("/games/{gameId}").param(String::class.java).body(MakeGuessRequest::class.java)
            .handle(::makeGuess)
    )

    data class GameStartedResponse(val gameId: String)
    data class MakeGuessRequest(val guess: List<String>)
    data class MakeGuessResponse(val gameId: String, val feedback: Feedback?, val error: String?)

    private fun startGame(): Completes<ObjectResponse<GameStartedResponse>> = GameId.generate().let { gameId ->
        logger.info("Starting the game ${gameId.id}")
        newGame(gameId).startGame(codeMaker, 12)
        withSuccess(
            ObjectResponse.of(
                Created,
                headers(ResponseHeader.of(Location, "/games/${gameId.id}")),
                GameStartedResponse(gameId.id)
            )
        )
    }

    private fun makeGuess(gameId: String, request: MakeGuessRequest): Completes<ObjectResponse<MakeGuessResponse>> {
        val id = GameId(gameId)
        val guess = try {
            Code(request.guess.map { peg -> Code.CodePeg.valueOf(peg) })
        } catch (e: IllegalArgumentException) {
            return withSuccess(ObjectResponse.of(BadRequest, MakeGuessResponse(id.id, null, "Invalid guess code: ${request.guess.joinToString(", ")}. Allowed colours: ${Code.CodePeg.values().joinToString(", ")}.")))
        }
        return existingGame(id)
            .andThenTo { game ->
                logger.info("Making a guess $guess for the game ${id.id}")
                game.makeGuess(guess)
            }
            .andThenTo { outcome ->
                outcome.resolve(
                    { error: GameError ->
                        withSuccess(
                            ObjectResponse.of(BadRequest, MakeGuessResponse(id.id, null, error.message))
                        )
                    },
                    { feedback: Feedback ->
                        withSuccess(
                            ObjectResponse.of(Ok, MakeGuessResponse(id.id, feedback, null))
                        )
                    }
                )
            }
            .otherwise<Any> { ObjectResponse.of(InternalServerError, null) }
    }

    private fun viewGame(gameId: String): Completes<ObjectResponse<DecodingBoard?>> =
        decodingBoardQuery.findDecodingBoardForGame(gameId)
            .andThenTo(false) { board ->
                when (board) {
                    is DecodingBoard -> withSuccess(ObjectResponse.of(Ok, board))
                    else -> withSuccess(ObjectResponse.of<DecodingBoard>(NotFound, null))
                }
            }
            .otherwise<Boolean> { ObjectResponse.of(InternalServerError, null) }

    private fun newGame(gameId: GameId) = stage.actorFor(
        Game::class.java,
        Definition.has(GameEntity::class.java, Definition.parameters(gameId)),
        GameAddress(gameId)
    )

    private fun existingGame(gameId: GameId) = stage.actorOf(
        Game::class.java,
        GameAddress(gameId),
        GameEntity::class.java,
        gameId
    )
}