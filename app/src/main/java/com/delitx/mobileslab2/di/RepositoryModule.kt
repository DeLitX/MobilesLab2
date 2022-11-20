package com.delitx.mobileslab2.di

import com.delitx.mobileslab2.data.ProductionRepository
import com.delitx.mobileslab2.data.ProductionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindProductionRepository(impl: ProductionRepositoryImpl): ProductionRepository
}
