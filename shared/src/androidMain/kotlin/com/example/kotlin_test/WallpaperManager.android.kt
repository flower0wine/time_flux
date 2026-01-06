package com.example.kotlin_test

import android.app.WallpaperManager as AndroidWallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidWallpaperManager(private val context: Context) : WallpaperManager {
    @RequiresPermission(android.Manifest.permission.SET_WALLPAPER)
    override suspend fun setWallpaper(imageData: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = AndroidWallpaperManager.getInstance(context)
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            
            if (bitmap == null) {
                return@withContext Result.failure(Exception("无法解码图片"))
            }
            
            wallpaperManager.setBitmap(bitmap)
            bitmap.recycle()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private var wallpaperManagerInstance: WallpaperManager? = null

fun initWallpaperManager(context: Context) {
    wallpaperManagerInstance = AndroidWallpaperManager(context.applicationContext)
}

actual fun getWallpaperManager(): WallpaperManager {
    return wallpaperManagerInstance ?: throw IllegalStateException("WallpaperManager 未初始化，请先调用 initWallpaperManager()")
}
