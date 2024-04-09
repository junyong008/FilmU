package com.yjy.presentation.di

import android.content.Context
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageProcessorImpl
import com.yjy.presentation.util.ImageUtils
import com.yjy.presentation.util.ImageUtilsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Module {

    @Singleton
    @Provides
    fun provideImageProcessor(imageUtils: ImageUtils, displayManager: DisplayManager): ImageProcessor {
        return ImageProcessorImpl(imageUtils, displayManager)
    }

    @Singleton
    @Provides
    fun provideImageUtils(): ImageUtils = ImageUtilsImpl()

    @Singleton
    @Provides
    fun provideDisplayManager(@ApplicationContext context: Context): DisplayManager {
        return DisplayManager(context)
    }
}