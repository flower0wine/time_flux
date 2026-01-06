package com.example.kotlin_test

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform