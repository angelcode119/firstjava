package com.example.test

object Constants {
    // ? ???? ???? ?? Firebase Remote Config (deprecated - use ServerConfig.getBaseUrl())
    @Deprecated("Use ServerConfig.getBaseUrl() instead", ReplaceWith("ServerConfig.getBaseUrl()"))
    const val BASE_URL = "https://zeroday.cyou"  // Fallback only
    
    const val USER_ID = "8f41bc5eec42e34209a801a7fa8b2d94d1c3d983"
}