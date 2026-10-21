package com.maprandroid

import org.junit.Test
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ApiTest {
    @Test
    fun testRandomizeApi() {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        // Let's create a dummy 3MB file
        val dummyFile = File("dummy.sfc")
        val out = FileOutputStream(dummyFile)
        val dummyData = ByteArray(3 * 1024 * 1024)
        out.write(dummyData)
        out.close()

        val request = Request.Builder()
            .url("https://maprando.com/api/randomize")
            .post(dummyFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        
        println("Sending request to /api/randomize")
        val response = client.newCall(request).execute()
        println("Response Code: ${response.code}")
        println("Response Body: ${response.body?.string()}")
        
        dummyFile.delete()
    }
}
