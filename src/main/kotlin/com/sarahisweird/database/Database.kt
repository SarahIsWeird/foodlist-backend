package com.sarahisweird.database

import com.sarahisweird.data.Items
import com.sarahisweird.data.StorageUnits
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    val instance by lazy {
        val db = Database.connect(
            "jdbc:mysql://localhost:3306/foodlist",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "foodlist",
            password = "foodlist"
        )

        transaction {
            SchemaUtils.create(StorageUnits, Items)
        }

        db
    }
}