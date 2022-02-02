package pl.zalas.mastermind.infrastructure.http

import io.vlingo.xoom.actors.World
import io.vlingo.xoom.common.Completes
import io.vlingo.xoom.http.resource.Configuration.Sizing
import io.vlingo.xoom.http.resource.Configuration.Timing
import io.vlingo.xoom.http.resource.Resources
import io.vlingo.xoom.http.resource.Server
import io.vlingo.xoom.symbio.store.dispatch.NoOpDispatcher
import pl.zalas.mastermind.infrastructure.factory.JournalFactory
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory
import pl.zalas.mastermind.model.RandomCodeMaker
import pl.zalas.mastermind.view.DecodingBoardQuery
import pl.zalas.mastermind.view.DecodingBoardQueryActor


class Application(val port: Int, configFile: String) {
    private val world = World.startWithDefaults("mastermind")
    private val stage = world.stageNamed("game")
    private val server: Server

    init {
        val configuration = Configuration.fromProperties(configFile)

        val stateStore = StateStoreFactory(stage, world.defaultLogger(), configuration.stateStore)
            .createStateStore(NoOpDispatcher())
        JournalFactory(stage, configuration.journal).createJournal(stateStore)

        val decodingBoardQuery =
            stage.actorFor(DecodingBoardQuery::class.java, DecodingBoardQueryActor::class.java, stateStore)
        val gameResource = GameResource(RandomCodeMaker(4), decodingBoardQuery, stage)

        server = startServer(Resources.are(gameResource.routes()))

        registerShutdownHook()
    }

    fun serverStartup(): Completes<Boolean> {
        return server.startUp()
    }

    fun stop() {
        server.stop()
        world.terminate()
    }

    private fun startServer(resources: Resources): Server = Server.startWith(
        stage,
        resources,
        port,
        Sizing.define().withProcessorPoolSize(Runtime.getRuntime().availableProcessors()),
        Timing.define()
    )

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            println("")
            println("====================")
            println("Stopping mastermind.")
            println("====================")
            println("")
            stop()
            Thread.sleep(1000L)
        }))
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val configFile = if (args.isNotEmpty()) args[0] else "classpath:in-memory.properties"
            val port = if (args.size > 1) args[1].toInt() else 8080
            val app = Application(port, configFile)

            println("=====================================================================================")
            println(
                "                              ,--.                              ,--.             ,--. \n" +
                        ",--,--,--.  ,--,--.  ,---.  ,-'  '-.  ,---.  ,--.--. ,--,--,--. `--' ,--,--,   ,-|  | \n" +
                        "|        | ' ,-.  | (  .-'  '-.  .-' | .-. : |  .--' |        | ,--. |      \\ ' .-. | \n" +
                        "|  |  |  | \\ '-'  | .-'  `)   |  |   \\   --. |  |    |  |  |  | |  | |  ||  | \\ `-' | \n" +
                        "`--`--`--'  `--`--' `----'    `--'    `----' `--'    `--`--`--' `--' `--''--'  `---'  "
            )
            println("service: started at http://localhost:${app.port}")
            println("try: curl -X POST http://localhost:${app.port}/games")
            println("=====================================================================================")
        }
    }
}

