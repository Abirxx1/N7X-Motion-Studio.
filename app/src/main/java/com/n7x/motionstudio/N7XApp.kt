package com.n7x.motionstudio

import android.app.Application
import androidx.room.Room
import com.n7x.motionstudio.data.AppDatabase
import com.n7x.motionstudio.data.MIGRATION_1_2
import com.n7x.motionstudio.data.MIGRATION_2_3
import com.n7x.motionstudio.data.MIGRATION_3_4
import com.n7x.motionstudio.data.MIGRATION_4_5

class N7XApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "n7x.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
