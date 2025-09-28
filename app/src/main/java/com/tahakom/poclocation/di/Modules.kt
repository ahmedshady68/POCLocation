package com.tahakom.poclocation.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.tahakom.poclocation.data.AppDatabase
import com.tahakom.poclocation.net.TripApi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "app.db").build()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://your.api/") // TODO: set your base URL
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .client(client)
        .build()

    @Provides @Singleton
    fun tripApi(retrofit: Retrofit): TripApi = retrofit.create(TripApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides @Singleton
    fun fused(@ApplicationContext ctx: Context) =
        LocationServices.getFusedLocationProviderClient(ctx)
}
