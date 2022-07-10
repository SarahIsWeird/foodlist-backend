package com.sarahisweird.plugins

import com.sarahisweird.database.Database
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureHTTP() {
    Database.instance

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(Compression) {
        gzip {
            priority = 1.0

            matchContentType(ContentType.Image.Any)
        }

        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

}
