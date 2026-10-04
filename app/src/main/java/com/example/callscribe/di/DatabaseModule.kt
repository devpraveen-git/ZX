package com.example.callscribe.di

import android.content.Context
import androidx.room.Room
import com.example.callscribe.data.local.CallRecordingDao
import com.example.callscribe.data.local.CallScribeDatabase
import com.example.callscribe.data.local.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallScribeDatabase {
        return Room.databaseBuilder(
            context,
            CallScribeDatabase::class.java,
            "callscribe_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideCallRecordingDao(database: CallScribeDatabase): CallRecordingDao {
        return database.callRecordingDao()
    }

    @Provides
    fun provideTranscriptDao(database: CallScribeDatabase): TranscriptDao {
        return database.transcriptDao()
    }
}
