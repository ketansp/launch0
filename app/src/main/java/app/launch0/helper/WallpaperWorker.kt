package app.launch0.helper

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.launch0.data.Constants
import app.launch0.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WallpaperWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val prefs = Prefs(applicationContext)

    override suspend fun doWork(): Result = coroutineScope {
        val success =
            if (isLaunch0Default(applicationContext).not())
                true
            else if (prefs.dailyWallpaper) {
                val todaySeed = getTodaySeed()
                if (prefs.dailyWallpaperUrl == todaySeed)
                    true
                else {
                    val isDark = checkIsDarkTheme()
                    val generated = generateAndSetWallpaper(isDark, todaySeed)
                    if (generated) prefs.dailyWallpaperUrl = todaySeed
                    generated
                }
            } else
                true

        if (success)
            Result.success()
        else
            Result.retry()
    }

    private suspend fun generateAndSetWallpaper(isDark: Boolean, seed: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val (width, height) = getScreenDimensions(applicationContext)
                val bitmap = WallpaperGenerator.generate(width, height, isDark, seed.hashCode().toLong())
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
                    wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
                bitmap.recycle()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun getTodaySeed(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun checkIsDarkTheme(): Boolean {
        return when (prefs.appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> applicationContext.isDarkThemeOn()
        }
    }
}
