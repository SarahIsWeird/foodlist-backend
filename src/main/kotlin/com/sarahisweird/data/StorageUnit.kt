package com.sarahisweird.data

import com.sarahisweird.database.enumerationOf
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class PartialStorageUnitDTO(
    val id: Long,
    val name: String,
    val description: String,
    val storageType: StorageType,
)

data class StorageUnitDTO(
    val id: Long,
    val name: String,
    val description: String,
    val storageType: StorageType,
    val shelves: List<PartialShelfDTO>,
)

object StorageUnits : LongIdTable() {
    val name = varchar("name", 25)
    val description = text("description")
    val storageType = enumerationOf<StorageType>("storageType")
}

class StorageUnit(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<StorageUnit>(StorageUnits) {
        fun getStorageListDTO(): List<PartialStorageUnitDTO> =
            transaction {
                StorageUnit.all()
                    .map(StorageUnit::toPartialDTO)
            }
    }

    var name by StorageUnits.name
    var description by StorageUnits.description
    var storageType by StorageUnits.storageType

    fun toPartialDTO(): PartialStorageUnitDTO =
        PartialStorageUnitDTO(
            this.id.value,
            this.name,
            this.description,
            this.storageType,
        )

    fun toDTO(): StorageUnitDTO =
        transaction {
            StorageUnitDTO(
                this@StorageUnit.id.value,
                this@StorageUnit.name,
                this@StorageUnit.description,
                this@StorageUnit.storageType,
                StorageUnits.join(Shelves, JoinType.INNER, additionalConstraint = { StorageUnits.id eq Shelves.ofStorageUnit })
                    .slice(Shelves.columns + StorageUnits.id)
                    .select { Shelves.ofStorageUnit eq this@StorageUnit.id.value }
                    .let { result -> Shelf.wrapRows(result).toList().map(Shelf::toPartialDTO) }
            )
        }
}