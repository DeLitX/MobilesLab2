package com.delitx.mobileslab2.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.delitx.mobileslab2.models.Production

@Database(
    entities = [
        Production::class
    ],
    version = 1
)
abstract class AppDB : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "app_db"
    }

    abstract fun getProductionDao(): ProductionDao
}
