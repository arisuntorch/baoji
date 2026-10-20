package com.baoji.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.baoji.data.db.BaojiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BaojiDatabase =
        Room.databaseBuilder(context, BaojiDatabase::class.java, BaojiDatabase.NAME)
            .addMigrations(*BaojiDatabase.MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides fun profileDao(db: BaojiDatabase) = db.profileDao()
    @Provides fun weightDao(db: BaojiDatabase) = db.weightDao()
    @Provides fun diaryDao(db: BaojiDatabase) = db.diaryDao()
    @Provides fun planDao(db: BaojiDatabase) = db.planDao()
    @Provides fun trainingLogDao(db: BaojiDatabase) = db.trainingLogDao()
    @Provides fun mealDao(db: BaojiDatabase) = db.mealDao()
    @Provides fun freeLogDao(db: BaojiDatabase) = db.freeLogDao()
    @Provides fun exportDao(db: BaojiDatabase) = db.exportDao()
}
