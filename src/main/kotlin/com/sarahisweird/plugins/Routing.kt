package com.sarahisweird.plugins

import com.sarahisweird.routes.storageunits.storageUnits
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/api") {
            route("/storageUnits") {
                storageUnits()
            }
        }
    }
}
