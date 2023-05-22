package eu.m0dex.monitoring.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface ILoggable {
    val logger: Logger
        get() = LoggerFactory.getLogger(this::class.java)
}
