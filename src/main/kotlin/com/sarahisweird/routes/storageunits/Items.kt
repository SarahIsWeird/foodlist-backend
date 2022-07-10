package com.sarahisweird.routes.storageunits

import com.sarahisweird.data.Item
import com.sarahisweird.data.Items
import com.sarahisweird.data.StorageUnit
import com.sarahisweird.data.StorageUnits
import com.sarahisweird.routes.getStorageUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class NewItemDTO(
    val name: String,
    val description: String,
)

fun Route.storedItems() {
    get {
        val storageUnit = getStorageUnit() ?: return@get

        val items = transaction {
            Items.join(StorageUnits, JoinType.INNER, additionalConstraint = { Items.inStorageUnit eq storageUnit.id.value })
                .slice(Items.columns + StorageUnits.columns)
                .select { Items.inStorageUnit eq storageUnit.id.value }
                .let { result -> Item.wrapRows(result).toList().map(Item::toDTO) }
        }

        call.respond(items)
    }

    post {
        val storageUnit = getStorageUnit() ?: return@post
        val item = call.receive<NewItemDTO>()

        if (item.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The item name cannot exceed 25 characters.")
            return@post
        }

        val createdItem = transaction {
            Item.new {
                name = item.name
                description = item.description
                inStorageUnit = storageUnit.id.value
            }.toDTO()
        }

        call.respond(createdItem)
    }

    route("/{iid}") {
        storedItem()
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getItem(): Item? {
    val iid = call.parameters["iid"]?.toLongOrNull()

    if (iid == null) {
        call.respond(HttpStatusCode.BadRequest, "Bad item ID")
        return null
    }

    val item = transaction { Item.findById(iid) }

    if (item == null) {
        call.respond(HttpStatusCode.NotFound, "Unknown item")
    }

    return item
}

data class PatchItemDTO(
    val name: String?,
    val description: String?,
    val inStorageUnit: Long?,
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

        if (patchData.name == null && patchData.description == null && patchData.inStorageUnit == null) {
            call.respond(HttpStatusCode.NoContent)
            return@patch
        }

        if (patchData.name != null && patchData.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The item name cannot exceed 25 characters.")
            return@patch
        }

        if (patchData.inStorageUnit != null && transaction { StorageUnit.findById(patchData.inStorageUnit) } == null) {
            call.respond(HttpStatusCode.NotFound, "The receiving storage unit cannot be found.")
            return@patch
        }

        transaction {
            if (patchData.name != null) item.name = patchData.name
            if (patchData.description != null) item.description = patchData.description
            if (patchData.inStorageUnit != null) item.inStorageUnit = patchData.inStorageUnit
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