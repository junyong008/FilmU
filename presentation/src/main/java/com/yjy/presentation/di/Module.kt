package com.yjy.presentation.di

import android.content.Context
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.HandDetector
import com.yjy.presentation.util.HandDetectorImpl
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageProcessorImpl
import com.yjy.presentation.util.ImageUtils
import com.yjy.presentation.util.ImageUtilsImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named

@Module
@InstallIn(ViewModelComponent::class)
abstract class Module {

    @Binds
    @ViewModelScoped
    abstract fun bindImageProcessor(imageProcessorImpl: ImageProcessorImpl): ImageProcessor

    @Binds
    @ViewModelScoped
    abstract fun bindImageUtils(imageUtilsImpl: ImageUtilsImpl): ImageUtils

    companion object {
        @Provides
        @ViewModelScoped
        fun provideHandDetector(@ApplicationContext context: Context, @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher): HandDetector {
            return HandDetectorImpl(context, defaultDispatcher)
        }

        @Provides
        @ViewModelScoped
        @Named("defaultDispatcher")
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        @ViewModelScoped
        fun provideDisplayManager(@ApplicationContext context: Context): DisplayManager = DisplayManager(context)
    }
}