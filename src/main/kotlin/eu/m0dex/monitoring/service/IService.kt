package eu.m0dex.monitoring.service

interface IService {
    val config: IServiceConfig

    suspend fun run()
}
