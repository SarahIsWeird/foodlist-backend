package com.sarahisweird.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class PartialShelfDTO(
    val id: Long,
    val name: String,
    val description: String,
    val ofStorageUnit: Long,
)

data class ShelfDTO(
    val id: Long,
    val name: String,
    val description: String,
    val ofStorageUnit: Long,
    val items: List<ItemDTO>,
)

object Shelves : LongIdTable() {
    val name = varchar("name", 25)
    val description = text("description")
    val ofStorageUnit = long("ofStorageUnit")
}

class Shelf(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Shelf>(Shelves)

    var name by Shelves.name
    var description by Shelves.description
    var ofStorageUnit by Shelves.ofStorageUnit

    fun toPartialDTO(): PartialShelfDTO =
        PartialShelfDTO(
            this.id.value,
            this.name,
            this.description,
            this.ofStorageUnit
        )

    fun toDTO(): ShelfDTO =
        transaction {
            ShelfDTO(
                this@Shelf.id.value,
                this@Shelf.name,
                this@Shelf.description,
                this@Shelf.ofStorageUnit,
                Shelves.join(Items, JoinType.INNER, additionalConstraint = { Shelves.id eq Items.inShelf })
                    .slice(Items.columns + Shelves.columns)
                    .select { Items.inShelf eq this@Shelf.id.value }
                    .let { result -> Item.wrapRows(result).toList().map(Item::toDTO) }
            )
        }
}