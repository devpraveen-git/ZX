package com.example.callscribe.di

import com.example.callscribe.data.repository.RecordingRepository
import com.example.callscribe.data.repository.RecordingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        recordingRepositoryImpl: RecordingRepositoryImpl
    ): RecordingRepository
}
