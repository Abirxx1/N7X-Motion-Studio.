package com.n7x.motionstudio

import android.content.Context
import android.util.Log
import java.io.File

object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = "Thread: ${thread.name}\n" + Log.getStackTraceString(throwable)
                file(app).writeText(text.take(6000))
            } catch (e: Exception) {
                // ignore
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        val f = file(context)
        return if (f.exists()) f.readText() else null
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
