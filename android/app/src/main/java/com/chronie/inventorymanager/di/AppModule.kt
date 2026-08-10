package com.chronie.inventorymanager.di

import android.content.Context
import androidx.room.Room
import com.chronie.inventorymanager.data.local.InventoryDao
import com.chronie.inventorymanager.data.local.InventoryDatabase
import com.chronie.inventorymanager.data.repository.ApiFactory
import com.chronie.inventorymanager.data.network.InventoryApiService
import com.chronie.inventorymanager.data.repository.InventoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideInventoryApiService(): InventoryApiService = ApiFactory.create()

    @Provides
    @Singleton
    fun provideInventoryDatabase(@ApplicationContext context: Context): InventoryDatabase =
        Room.databaseBuilder(
            context,
            InventoryDatabase::class.java,
            InventoryDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideInventoryDao(database: InventoryDatabase): InventoryDao = database.inventoryDao()

    @Provides
    @Singleton
    fun provideInventoryRepository(
        apiService: InventoryApiService,
        dao: InventoryDao
    ): InventoryRepository = InventoryRepository(apiService, dao)
}
