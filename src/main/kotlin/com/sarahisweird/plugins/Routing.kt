package com.sarahisweird.plugins

import com.sarahisweird.routes.storageUnits
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

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
