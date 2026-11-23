package com.soulbrowser.app

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashLogCollector {
    private const val FILE_NAME = "last_crash.log"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
                File(appContext.filesDir, FILE_NAME).writeText(
                    "Thread: ${thread.name}\nTime: ${System.currentTimeMillis()}\n\n$stack"
                )
            }
            if (previous != null) previous.uncaughtException(thread, throwable)
            else thread.threadGroup.uncaughtException(thread, throwable)
        }
    }

    fun readLastCrash(context: Context): String? =
        runCatching { File(context.filesDir, FILE_NAME).takeIf { it.isFile }?.readText() }.getOrNull()

    fun clearLastCrash(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
