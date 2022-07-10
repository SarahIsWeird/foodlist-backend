package com.sarahisweird.routes

import com.sarahisweird.data.StorageType
import com.sarahisweird.data.StorageUnit
import com.sarahisweird.data.StorageUnits
import com.sarahisweird.routes.storageunits.storedItems
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.transactions.transaction

data class NewStorageUnitDTO(
    val name: String,
    val description: String,
    val storageType: StorageType,
)

fun Route.storageUnits() {
    get {
        call.respond(StorageUnit.getStorageListDTO())
    }

    post {
        val storageUnitInformation = call.receive<NewStorageUnitDTO>()

        if (storageUnitInformation.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The storage unit name cannot exceed 25 characters.")
            return@post
        }

        if (transaction { StorageUnit.find { StorageUnits.name eq storageUnitInformation.name }.any() }) {
            call.respond(HttpStatusCode.Conflict, "A storage unit with this name already exists.")
            return@post
        }

        val newStorageUnit = transaction {
            StorageUnit.new {
                this.name = storageUnitInformation.name
                this.description = storageUnitInformation.description
                this.storageType = storageUnitInformation.storageType
            }
        }

        call.respond(newStorageUnit.toPartialDTO())
    }

    route("/{sid}") {
        storageUnit()
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getStorageUnit(): StorageUnit? {
    val sid = call.parameters["sid"]?.toLongOrNull()

    if (sid == null) {
        call.respond(HttpStatusCode.BadRequest, "Bad storage unit ID")
        return null
    }

    val storageUnit = transaction { StorageUnit.findById(sid) }

    if (storageUnit == null) {
        call.respond(HttpStatusCode.NotFound, "Unknown storage unit")
    }

    return storageUnit
}

data class PatchStorageUnitDTO(
    val name: String?,
    val description: String?,
    val storageType: StorageType?,
)

@Suppress("DuplicatedCode")
fun Route.storageUnit() {
    get {
        val storageUnit = getStorageUnit() ?: return@get

        call.respond(storageUnit.toDTO())
    }

    patch {
        val storageUnit = getStorageUnit() ?: return@patch
        val patchData = call.receive<PatchStorageUnitDTO>()

        if (patchData.name == null && patchData.description == null && patchData.storageType == null) {
            call.respond(HttpStatusCode.NoContent)
            return@patch
        }

        if (patchData.name != null && patchData.name.length > 25) {
            call.respond(HttpStatusCode.BadRequest, "The storage unit name cannot exceed 25 characters.")
            return@patch
        }

        transaction {
            if (patchData.name != null) storageUnit.name = patchData.name
            if (patchData.description != null) storageUnit.description = patchData.description
            if (patchData.storageType != null) storageUnit.storageType = patchData.storageType
        }

        call.respond(storageUnit.toDTO())
    }

    delete {
        val storageUnit = getStorageUnit() ?: return@delete

        transaction {
            storageUnit.delete()
        }

        call.respond(HttpStatusCode.NoContent)
    }

    route("items") {
        storedItems()
    }
}