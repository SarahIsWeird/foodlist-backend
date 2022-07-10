package com.sarahisweird.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

inline fun <reified T : Enum<T>> Table.enumerationOf(name: String): Column<T> {
    val sqlEnumDefinition = "ENUM(" + enumValues<T>().joinToString(",") { "'${it.name}'" } + ")"

    return customEnumeration(name, sqlEnumDefinition, { enumValueOf(it as String) }, { it.name })
}