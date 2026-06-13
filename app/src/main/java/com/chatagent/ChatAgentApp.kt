package com.chatagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatAgentApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val crashFile = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ),
            "chatagent_crash.txt"
        )
        if (crashFile.exists()) crashFile.delete()
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                pw.flush()
                crashFile.writeText(sw.toString())
            } catch (_: Exception) {}
            prevHandler?.uncaughtException(thread, throwable)
        }
    }
}
