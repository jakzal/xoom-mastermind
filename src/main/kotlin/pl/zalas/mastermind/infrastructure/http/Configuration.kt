package pl.zalas.mastermind.infrastructure.http

import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

data class Configuration(
    val journal: JournalConfiguration,
    val stateStore: StateStoreConfiguration
) {

    class ConfigurationFileNotFound(filePath: String) :
        RuntimeException("Configuration file could not be loaded: `${filePath}`.")

    companion object {
        fun fromProperties(filePath: String): Configuration {
            val props = loadProperties(filePath)

            return Configuration(
                loadJournalConfiguration(props),
                loadStateStoreConfiguration(props)
            )
        }

        private fun loadProperties(filePath: String): Properties {
            val props = Properties()
            props.load(openStream(filePath))
            return props
        }

        private fun openStream(filePath: String): InputStream = try {
            if (filePath.startsWith("classpath:")) {
                Configuration::class.java.getResourceAsStream(filePath.replace("classpath:", "/"))
            } else {
                FileInputStream(filePath)
            } ?: throw ConfigurationFileNotFound(filePath)
        } catch (e: FileNotFoundException) {
            throw ConfigurationFileNotFound(filePath)
        }

        private fun loadJournalConfiguration(props: Properties) =
            when (props.resolveProperty("mastermind.journal.strategy")) {
                "postgresql" -> JournalConfiguration.PostgreSQLConfiguration(
                    props.resolveProperty("mastermind.journal.username", "mastermind"),
                    props.resolveProperty("mastermind.journal.password", "mastermind"),
                    props.resolveProperty("mastermind.journal.database", "mastermind"),
                    props.resolveProperty("mastermind.journal.hostname", "[::1]"),
                    props.resolveProperty("mastermind.journal.port", "5432").toInt(),
                    props.resolveProperty("mastermind.journal.useSsl", "false") == "true"
                )
                else -> JournalConfiguration.InMemoryConfiguration
            }

        private fun loadStateStoreConfiguration(props: Properties) =
            when (props.resolveProperty("mastermind.stateStore.strategy")) {
                "postgresql" -> StateStoreConfiguration.PostgreSQLConfiguration(
                    props.resolveProperty("mastermind.stateStore.username", "mastermind"),
                    props.resolveProperty("mastermind.stateStore.password", "mastermind"),
                    props.resolveProperty("mastermind.stateStore.database", "mastermind"),
                    props.resolveProperty("mastermind.stateStore.hostname", "[::1]"),
                    props.resolveProperty("mastermind.stateStore.port", "5432").toInt(),
                    props.resolveProperty("mastermind.stateStore.useSsl", "false") == "true"
                )
                else -> StateStoreConfiguration.InMemoryConfiguration
            }

        private fun Properties.resolveProperty(name: String, default: String): String =
            System.getProperty(name, getProperty(name, default))

        private fun Properties.resolveProperty(name: String): String? =
            System.getProperty(name, getProperty(name))
    }

}
