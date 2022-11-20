package com.delitx.mobileslab2.di

import android.content.Context
import androidx.room.Room
import com.delitx.mobileslab2.db.AppDB
import com.delitx.mobileslab2.db.ProductionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideDB(@ApplicationContext context: Context): AppDB =
        Room.databaseBuilder(
            context,
            AppDB::class.java,
            AppDB.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductionDao(db: AppDB): ProductionDao = db.getProductionDao()
}
