package com.codingfactory

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

/**
 * Exposes Prometheus metrics on GET /metrics.
 *
 * Out of the box this gives us, per route:
 *   - ktor_http_server_requests_seconds (count + latency histogram, tagged by route/method/status)
 *   - ktor_http_server_active_requests (in-flight gauge)
 * plus JVM metrics (heap, GC pauses, threads, classloading) and process CPU.
 *
 * Scrape it with Prometheus, or eyeball it during a k6 run (see perf/README.md).
 */
fun Application.configureMonitoring() {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        this.registry = registry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmThreadMetrics(),
            ClassLoaderMetrics(),
            ProcessorMetrics(),
        )
    }

    routing {
        get("/metrics") {
            call.respond(registry.scrape())
        }
    }
}
