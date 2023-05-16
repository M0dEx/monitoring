package eu.m0dex.monitoring

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.service.IServiceBuilder
import io.questdb.client.Sender
import org.jetbrains.exposed.sql.Database

class MonitoringServiceBuilder(
    private val configPath: String
) : IServiceBuilder<MonitoringService>, ILoggable {
    override fun build(): MonitoringService {
        val config = ConfigLoaderBuilder
            .default()
            .addFileSource(configPath)
            .build()
            .loadConfigOrThrow<MonitoringServiceConfig>()

        logger.info("Successfully parsed the configuration file")

        val questDbSender = Sender
            .builder()
            .address(config.questDb.host)
            .port(config.questDb.writePort)
            .bufferCapacity(config.questDb.writeBufferSize)
            .build()

        logger.info("Connected to QuestDB with the ILP sender")

        val questDbReader = Database
            .connect(
                url = "jdbc:postgresql://${config.questDb.host}:${config.questDb.readPort}/questdb",
                driver = "org.postgresql.Driver",
                user = config.questDb.username,
                password = config.questDb.password,
            )

        logger.info("Connected to QuestDB with the JDBC reader")

        return MonitoringService(
            config = config,
            questDbSender = questDbSender,
            questDbReader = questDbReader,
        )
    }
}