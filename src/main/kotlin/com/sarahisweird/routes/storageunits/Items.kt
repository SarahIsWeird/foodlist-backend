package com.sarahisweird.routes.storageunits

import com.sarahisweird.data.*
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

data class NewItemDTO(
    val name: String,
    val description: String,
)

fun Route.storedItems() {
    get {
        val shelf = getShelf() ?: return@get

        val items = transaction {
            Items.join(Shelves, JoinType.INNER, additionalConstraint = { Items.inShelf eq shelf.id.value })
                .slice(Items.columns + Shelves.id)
                .select { Items.inShelf eq shelf.id.value }
                .let { result -> Item.wrapRows(result).map(Item::toDTO) }
        }

        call.respond(items)
    }

    post {
        val shelf = getShelf() ?: return@post
        val item = call.receive<NewItemDTO>()

        if (item.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The item name cannot exceed 25 characters.")
            return@post
        }

        val createdItem = transaction {
            Item.new(id = Random.nextLong()) {
                name = item.name
                description = item.description
                inShelf = shelf.id.value
            }.toDTO()
        }

        call.respond(createdItem)
    }

    route("/{iid}") {
        storedItem()
    }
}

fun PipelineContext<Unit, ApplicationCall>.getItem(): Item? {
    val iid = call.parameters["iid"]?.toLongOrNull()

    if (iid == null) {
        launch {
            call.respond(HttpStatusCode.BadRequest, "Bad item ID")
        }

        return null
    }

    val item = transaction { Item.findById(iid) }

    if (item == null) {
        launch {
            call.respond(HttpStatusCode.NotFound, "Unknown item")
        }
    }

    return item
}

data class PatchItemDTO(
    val name: String?,
    val description: String?,
    val inShelf: Long?,
)

@Suppress("DuplicatedCode")
fun Route.storedItem() {
    get {
        val item = getItem() ?: return@get

        call.respond(item.toDTO())
    }

    patch {
        val item = getItem() ?: return@patch
        val patchData = call.receive<PatchItemDTO>()

        if (patchData.name == null && patchData.description == null && patchData.inShelf == null) {
            call.respond(HttpStatusCode.NoContent)
            return@patch
        }

        if (patchData.name != null && patchData.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The item name cannot exceed 25 characters.")
            return@patch
        }

        if (patchData.inShelf != null && transaction { Shelf.findById(patchData.inShelf) } == null) {
            call.respond(HttpStatusCode.NotFound, "The receiving storage unit cannot be found.")
            return@patch
        }

        transaction {
            if (patchData.name != null) item.name = patchData.name
            if (patchData.description != null) item.description = patchData.description
            if (patchData.inShelf != null) item.inShelf = patchData.inShelf
        }

        call.respond(item.toDTO())
    }

    delete {
        val item = getItem() ?: return@delete

        transaction {
            item.delete()
        }

        call.respond(HttpStatusCode.NoContent)
    }
}