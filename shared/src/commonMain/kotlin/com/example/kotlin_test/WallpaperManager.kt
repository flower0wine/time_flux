package com.example.kotlin_test

interface WallpaperManager {
    suspend fun setWallpaper(imageData: ByteArray): Result<Unit>
}

expect fun getWallpaperManager(): WallpaperManager
