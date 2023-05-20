package eu.m0dex.monitoring.statusapi.resources

import io.ktor.resources.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.time.Instant as JavaInstant

@Resource("/services")
class Services {
    @Resource("{name}")
    class Get(val parent: Services = Services(), val name: String, val closestBefore: Instant = Instant.DISTANT_FUTURE) {
        @Resource("history")
        class History(val parent: Get, val from: Instant = JavaInstant.EPOCH.toKotlinInstant(), val to: Instant = Clock.System.now())
    }
}