package com.jbcbros.qbitremote.di

import android.content.Context
import com.jbcbros.qbitremote.data.repository.QbRepository
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
    fun provideQbRepository(@ApplicationContext context: Context): QbRepository {
        return QbRepository(context)
    }
}
