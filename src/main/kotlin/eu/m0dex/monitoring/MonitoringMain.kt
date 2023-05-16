package eu.m0dex.monitoring

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.runBlocking

object MonitoringMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("monitoring")
        val configPath by parser.argument(ArgType.String, description = "Path to the config file")

        parser.parse(args)

        runBlocking {
            MonitoringServiceBuilder(configPath)
                .build()
                .run()
        }
    }
}