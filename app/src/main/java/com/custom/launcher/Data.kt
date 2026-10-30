package com.custom.launcher

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

data class AppInfo(val label: String, val component: ComponentName)
data class IconPack(val pkg: String, val label: String)

data class Settings(
    val blur: Int = 60,          // 0..100 (window blur radius in px)
    val glassAlpha: Float = 0.35f, // drawer glass tint opacity
    val iconPack: String = "",   // "" = system icons
    val name: String = "Raj"
)

private val Context.ds by preferencesDataStore("settings")
private val KBlur = intPreferencesKey("blur")
private val KAlpha = floatPreferencesKey("alpha")
private val KPack = stringPreferencesKey("pack")
private val KName = stringPreferencesKey("name")

fun Context.settingsFlow() = ds.data.map {
    Settings(it[KBlur] ?: 60, it[KAlpha] ?: 0.35f, it[KPack] ?: "", it[KName] ?: "Raj")
}

suspend fun Context.saveBlur(v: Int) = ds.edit { it[KBlur] = v }
suspend fun Context.saveAlpha(v: Float) = ds.edit { it[KAlpha] = v }
suspend fun Context.savePack(v: String) = ds.edit { it[KPack] = v }
suspend fun Context.saveName(v: String) = ds.edit { it[KName] = v }
