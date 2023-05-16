package eu.m0dex.monitoring.service

interface IServiceBuilder<IService> {
    fun build(): IService
}