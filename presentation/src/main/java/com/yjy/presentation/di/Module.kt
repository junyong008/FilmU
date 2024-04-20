package com.yjy.presentation.di

import android.content.Context
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.HandDetector
import com.yjy.presentation.util.HandDetectorImpl
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageProcessorImpl
import com.yjy.presentation.util.ImageUtils
import com.yjy.presentation.util.ImageUtilsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Module {

    @Singleton
    @Provides
    fun provideHandDetector(@ApplicationContext context: Context, @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher): HandDetector {
        return HandDetectorImpl(context, defaultDispatcher)
    }

    @Singleton
    @Provides
    fun provideImageProcessor(imageUtils: ImageUtils, displayManager: DisplayManager, @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher): ImageProcessor {
        return ImageProcessorImpl(imageUtils, displayManager, defaultDispatcher)
    }

    @Singleton
    @Provides
    @Named("defaultDispatcher")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Singleton
    @Provides
    fun provideImageUtils(): ImageUtils = ImageUtilsImpl()

    @Singleton
    @Provides
    fun provideDisplayManager(@ApplicationContext context: Context): DisplayManager {
        return DisplayManager(context)
    }
}