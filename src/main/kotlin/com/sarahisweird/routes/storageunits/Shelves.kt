package com.sarahisweird.routes.storageunits

import com.sarahisweird.data.Shelf
import com.sarahisweird.data.Shelves
import com.sarahisweird.data.StorageUnits
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

data class NewShelfDTO(
    val name: String,
    val description: String,
)

fun Route.storageShelves() {
    get {
        val storageUnit = getStorageUnit() ?: return@get

        val shelves = transaction {
            Shelves.join(StorageUnits, JoinType.INNER, additionalConstraint = { Shelves.ofStorageUnit eq storageUnit.id.value })
                .slice(Shelves.columns + StorageUnits.id)
                .select { Shelves.ofStorageUnit eq storageUnit.id.value }
                .let { result -> Shelf.wrapRows(result).map(Shelf::toPartialDTO) }
        }

        call.respond(shelves)
    }

    post {
        val storageUnit = getStorageUnit() ?: return@post
        val shelf = call.receive<NewShelfDTO>()

        if (shelf.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The shelf name cannot exceed 25 characters.")
            return@post
        }

        val createdShelf = transaction {
            Shelf.new(id = Random.nextLong()) {
                name = shelf.name
                description = shelf.description
                ofStorageUnit = storageUnit.id.value
            }.toPartialDTO()
        }

        call.respond(createdShelf)
    }

    route("/{shid}") {
        storageShelf()
    }
}

fun PipelineContext<Unit, ApplicationCall>.getShelf(): Shelf? {
    val shid = call.parameters["shid"]?.toLongOrNull()

    if (shid == null) {
        launch {
            call.respond(HttpStatusCode.BadRequest, "Bad shelf ID")
        }

        return null
    }

    val shelf = transaction { Shelf.findById(shid) }

    if (shelf == null) {
        launch {
            call.respond(HttpStatusCode.NotFound, "Unknown shelf")
        }
    }

    return shelf
}

data class PatchShelfDTO(
    val name: String?,
    val description: String?,
)

fun Route.storageShelf() {
    get {
        val shelf = getShelf() ?: return@get

        call.respond(shelf.toDTO())
    }

    patch {
        val shelf = getShelf() ?: return@patch
        val patchData = call.receive<PatchShelfDTO>()

        if (patchData.name == null && patchData.description == null) {
            call.respond(HttpStatusCode.NoContent)
            return@patch
        }

        if (patchData.name != null && patchData.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The shelf name cannot exceed 25 characters.")
            return@patch
        }

        transaction {
            if (patchData.name != null) shelf.name = patchData.name
            if (patchData.description != null) shelf.description = patchData.description
        }

        call.respond(shelf.toDTO())
    }

    delete {
        val shelf = getShelf() ?: return@delete

        transaction {
            shelf.delete()
        }

        call.respond(HttpStatusCode.NoContent)
    }

    route("/items") {
        storedItems()
    }
}
