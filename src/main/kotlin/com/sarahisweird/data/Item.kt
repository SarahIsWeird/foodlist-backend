package com.sarahisweird.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

data class ItemDTO(
    val id: Long,
    val name: String,
    val description: String,
)

object Items : LongIdTable() {
    val name = varchar("name", 25)
    val description = text("description")
    val inShelf = long("inShelf")
}

class Item(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Item>(Items)

    var name by Items.name
    var description by Items.description
    var inShelf by Items.inShelf

    fun toDTO() =
        ItemDTO(this.id.value, this.name, this.description)
}