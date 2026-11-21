package com.n7x.motionstudio

import android.app.Application
import androidx.room.Room
import com.n7x.motionstudio.data.AppDatabase

class N7XApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "n7x.db").build()
    }
}
