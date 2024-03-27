package com.yjy.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yjy.data.local.database.dao.GithubRepoDao
import com.yjy.data.model.entity.GithubRepoEntity

@Database(entities = [GithubRepoEntity::class], version = 1, exportSchema = false)
// @TypeConverters(Converter::class)
abstract class ExampleDatabase : RoomDatabase() {

    abstract fun githubRepoDao(): GithubRepoDao

    companion object {
        @Volatile   // 메인 메모리에 할당

        // DB 인스턴스가 존재하면 만들지 않고, 없으면 새로 만든다. SingleTon 패턴으로 구성
        private var INSTANCE: ExampleDatabase? = null
        fun getInstance(context: Context): ExampleDatabase {
            if (INSTANCE == null) {
                synchronized(ExampleDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        ExampleDatabase::class.java,
                        "example_database"
                    ).build()
                }
            }
            return INSTANCE as ExampleDatabase
        }
    }
}

