package pl.zalas.mastermind.infrastructure.http.restassured

import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import pl.zalas.mastermind.infrastructure.http.Application

data class StartGameResponse(val gameId: String)

data class MakeGuessRequest(val guess: List<String>)

data class ViewBoardResponse(val gameId: String)

fun Application.startGame(): StartGameResponse =
    Given {
        port(port)
    } When {
        post("/games")
    } Then {
        statusCode(201)
        contentType(ContentType.JSON)
    } Extract {
        `as`(StartGameResponse::class.java)
    }

fun Application.makeGuess(gameId: String, request: MakeGuessRequest) =
    Given {
        port(port)
        contentType(ContentType.JSON)
    } When {
        body(request)
        post("/games/$gameId")
    } Then {
        statusCode(200)
        contentType(ContentType.JSON)
    }

fun Application.viewBoard(gameId: String): ViewBoardResponse =
    Given {
        port(port)
    } When {
        get("/games/$gameId")
    } Then {
        statusCode(200)
        contentType(ContentType.JSON)
    } Extract {
        `as`(ViewBoardResponse::class.java)
    }
