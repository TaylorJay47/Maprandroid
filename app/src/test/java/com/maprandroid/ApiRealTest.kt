package com.maprandroid

import com.maprandroid.data.local.ConfigProfileEntity
import com.maprandroid.data.remote.NetworkModule
import com.maprandroid.data.repository.MapRandoRepository
import org.junit.Test
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ApiRealTest {
    @Test
    fun testRandomizeAndCustomizeApiReal() {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        
        val dummyFile = File("dummy.sfc")
        val out = FileOutputStream(dummyFile)
        val dummyData = ByteArray(3 * 1024 * 1024) { 0.toByte() }
        out.write(dummyData)
        out.close()

        val repo = MapRandoRepository(NetworkModule.mapRandoApiService)
        val profile = ConfigProfileEntity(
            name = "Test Custom",
            mapRandomization = "Vanilla",
            samusSprite = "samus_dread",
            controlShot = "A",
            controlJump = "B",
            controlDash = "X",
            controlItemSelect = "Select",
            controlItemCancel = "Y",
            controlAngleUp = "R",
            controlAngleDown = "L",
            quickReloadL = true,
            quickReloadR = true,
            quickReloadSelect = true,
            quickReloadStart = true
        )

        val settingsJson = repo.buildSettingsJson(profile)
        println("Generated JSON from Repo:\n$settingsJson")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("spoiler_token", "")
            .addFormDataPart("settings", settingsJson)
            .addFormDataPart("rom", "dummy.sfc", dummyFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val req = Request.Builder().url("https://maprando.com/randomize").post(requestBody).build()
        val res = client.newCall(req).execute()
        val body = res.body?.string() ?: ""
        println("Repo Generated JSON Response Code: ${res.code}, Body: $body")

        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val mapAdapter = moshi.adapter(Map::class.java)
        val jsonMap = mapAdapter.fromJson(body)
        val seedUrl = jsonMap?.get("seed_url") as? String ?: ""
        println("Seed URL: $seedUrl")

        if (seedUrl.isNotEmpty()) {
            val customizeUrl = "https://maprando.com${seedUrl}customize"
            println("Testing Customize URL: $customizeUrl")

            fun makeCustomizeCall(shot: String, jump: String, dash: String, quickReloadList: List<String>, saveStateList: List<String>, loadStateList: List<String>): ByteArray {
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("rom", "dummy.sfc", dummyFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                    .addFormDataPart("samus_sprite", "samus_dread")
                    .addFormDataPart("etank_color", "96de38")
                    .addFormDataPart("reserve_hud_style", "true")
                    .addFormDataPart("transition_letters", "true")
                    .addFormDataPart("map_theme", "Light")
                    .addFormDataPart("palette_theme", "Vanilla")
                    .addFormDataPart("room_palettes", "Vanilla")
                    .addFormDataPart("tile_theme", "Vanilla")
                    .addFormDataPart("door_theme", "Vanilla")
                    .addFormDataPart("music", "AreaThemed")
                    .addFormDataPart("shaking", "Vanilla")
                    .addFormDataPart("flashing", "Vanilla")
                    .addFormDataPart("disable_beeping", "false")
                    .addFormDataPart("item_dot_change", "Fade")
                    .addFormDataPart("room_names", "true")
                    .addFormDataPart("boss_icons", "true")
                    .addFormDataPart("miniboss_icons", "true")
                    .addFormDataPart("save_icons", "true")
                    .addFormDataPart("statues_hallway_tiling", "Default")
                    .addFormDataPart("statues_hallway_audio", "Enabled")
                    .addFormDataPart("vanilla_screw_attack_animation", "false")
                    // Test both field names: "shot" and "control_shot"
                    .addFormDataPart("shot", shot)
                    .addFormDataPart("jump", jump)
                    .addFormDataPart("dash", dash)
                    .addFormDataPart("item_select", "Select")
                    .addFormDataPart("item_cancel", "Y")
                    .addFormDataPart("angle_up", "R")
                    .addFormDataPart("angle_down", "L")
                    .addFormDataPart("control_shot", shot)
                    .addFormDataPart("control_jump", jump)
                    .addFormDataPart("control_dash", dash)
                    .addFormDataPart("control_item_select", "Select")
                    .addFormDataPart("control_item_cancel", "Y")
                    .addFormDataPart("control_angle_up", "R")
                    .addFormDataPart("control_angle_down", "L")
                    .addFormDataPart("moonwalk", "true")

                for (btn in quickReloadList) {
                    builder.addFormDataPart("quick_reload_buttons", btn)
                }
                for (btn in saveStateList) {
                    builder.addFormDataPart("save_state_buttons", btn)
                }
                for (btn in loadStateList) {
                    builder.addFormDataPart("load_state_buttons", btn)
                }

                val customizeReq = Request.Builder().url(customizeUrl).post(builder.build()).build()
                val customizeRes = client.newCall(customizeReq).execute()
                return customizeRes.body?.bytes() ?: byteArrayOf()
            }

            val rom1 = makeCustomizeCall("X", "A", "B", listOf("L", "R", "Select", "Start"), listOf("X", "L", "R", "Select"), listOf("Y", "L", "R", "Select"))
            val rom2 = makeCustomizeCall("A", "B", "X", listOf("L", "R", "Select", "Start"), listOf("X", "L", "R", "Select"), listOf("Y", "L", "R", "Select"))
            val rom3 = makeCustomizeCall("X", "A", "B", emptyList(), listOf("Select", "R"), listOf("Select", "L"))

            println("ROM 1 (X, A, B | QReload=L,R,Sel,Start | Save=X,L,R,Sel | Load=Y,L,R,Sel) size: ${rom1.size}, SHA1: ${com.maprandroid.util.RomPatcher.computeSha1(rom1)}")
            println("ROM 2 (A, B, X | QReload=L,R,Sel,Start | Save=X,L,R,Sel | Load=Y,L,R,Sel) size: ${rom2.size}, SHA1: ${com.maprandroid.util.RomPatcher.computeSha1(rom2)}")
            println("ROM 3 (X, A, B | QReload=none | Save=Select,R | Load=Select,L) size: ${rom3.size}, SHA1: ${com.maprandroid.util.RomPatcher.computeSha1(rom3)}")

            val diff12 = rom1.zip(rom2).count { (a, b) -> a != b }
            val diff13 = rom1.zip(rom3).count { (a, b) -> a != b }

            println("Diff between ROM 1 (X,A,B) and ROM 2 (A,B,X): $diff12 bytes different!")
            println("Diff between ROM 1 (QReload 4 btns + Save X,L,R,Sel + Load Y,L,R,Sel) and ROM 3 (QReload none + Save Sel,R + Load Sel,L): $diff13 bytes different!")
        }

        dummyFile.delete()
        if (res.code != 200) {
            org.junit.Assert.fail("Failed: HTTP ${res.code}: $body")
        }
    }
}
