package pl.zalas.mastermind.infrastructure.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.*
import org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES
import pl.zalas.mastermind.infrastructure.factory.JournalFactory.JournalConfiguration
import pl.zalas.mastermind.infrastructure.factory.StateStoreFactory.StateStoreConfiguration
import pl.zalas.mastermind.infrastructure.http.Configuration.ConfigurationFileNotFound

@ResourceLock(value = SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
class ConfigurationTests {

    @Test
    fun `it loads the in memory configuration`() {
        val configuration = Configuration.fromProperties("classpath:config/in-memory.properties")

        assertEquals(JournalConfiguration.InMemoryConfiguration, configuration.journal)
        assertEquals(StateStoreConfiguration.InMemoryConfiguration, configuration.stateStore)
    }

    @Test
    fun `it loads the in memory configuration by default`() {
        val configuration = Configuration.fromProperties("classpath:config/empty.properties")

        assertEquals(JournalConfiguration.InMemoryConfiguration, configuration.journal)
        assertEquals(StateStoreConfiguration.InMemoryConfiguration, configuration.stateStore)
    }

    @Test
    fun `it loads the postgresql configuration`() {
        val configuration = Configuration.fromProperties("classpath:config/postgresql-defined.properties")

        assertEquals(
            JournalConfiguration.PostgreSQLConfiguration(
                "user1",
                "pass123",
                "db1",
                "host1",
                5000,
                true
            ), configuration.journal
        )
        assertEquals(
            StateStoreConfiguration.PostgreSQLConfiguration(
                "user2",
                "pass789",
                "db2",
                "host2",
                6000,
                true
            ), configuration.stateStore
        )
    }

    @Test
    fun `it loads default postgresql configuration`() {
        val configuration = Configuration.fromProperties("classpath:config/postgresql-defaults.properties")

        assertEquals(
            JournalConfiguration.PostgreSQLConfiguration(
                "mastermind",
                "mastermind",
                "mastermind",
                "[::1]",
                5432,
                false
            ), configuration.journal
        )
        assertEquals(
            StateStoreConfiguration.PostgreSQLConfiguration(
                "mastermind",
                "mastermind",
                "mastermind",
                "[::1]",
                5432,
                false
            ), configuration.stateStore
        )
    }

    @Test
    fun `it loads the configuration by full path`() {
        val configuration = Configuration.fromProperties("src/test/resources/config/in-memory.properties")

        assertEquals(JournalConfiguration.InMemoryConfiguration, configuration.journal)
        assertEquals(StateStoreConfiguration.InMemoryConfiguration, configuration.stateStore)
    }

    @Test
    fun `it throws an exception if the configuration file cannot be loaded`() {
        assertThrows<ConfigurationFileNotFound> {
            Configuration.fromProperties("classpath:missing.properties")
        }
        assertThrows<ConfigurationFileNotFound> {
            Configuration.fromProperties("src/test/resources/config/missing.properties")
        }
    }

    @Test
    fun `it favours system properties over the configuration file`() {
        System.setProperty("mastermind.journal.strategy", "postgresql")
        System.setProperty("mastermind.journal.username", "user1")

        val configuration = Configuration.fromProperties("classpath:in-memory.properties")

        System.clearProperty("mastermind.journal.strategy")
        System.clearProperty("mastermind.journal.username")

        assertEquals(
            JournalConfiguration.PostgreSQLConfiguration(
                "user1",
                "mastermind",
                "mastermind",
                "[::1]",
                5432,
                false
            ), configuration.journal
        )
        assertEquals(StateStoreConfiguration.InMemoryConfiguration, configuration.stateStore)
    }
}