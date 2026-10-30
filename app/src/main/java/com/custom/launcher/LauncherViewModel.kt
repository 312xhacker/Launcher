package com.custom.launcher

import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    private val pm: PackageManager = app.packageManager
    val music = MusicController(app)
    val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dock = MutableStateFlow<List<AppInfo>>(emptyList())
    val packs = MutableStateFlow<List<IconPack>>(emptyList())
    val iconVersion = MutableStateFlow(0)
    val settings = app.settingsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private var packKey = ""
    private var packRes: Resources? = null
    private var packMap: Map<String, String> = emptyMap()
    private val cache = LruCache<String, ImageBitmap>(400)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { refresh() }
    }

    init {
        refresh()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        app.registerReceiver(receiver, f)
        viewModelScope.launch {
            settings.map { it.iconPack }.distinctUntilChanged().collect { loadPack(it) }
        }
    }

    override fun onCleared() { app.unregisterReceiver(receiver); music.release() }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(q, 0)
            .filter { it.activityInfo.packageName != app.packageName }
            .map { AppInfo(it.loadLabel(pm).toString(), ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
            .sortedBy { it.label.lowercase() }
        apps.value = list

        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        )
        dock.value = intents.mapNotNull { i ->
            pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
                ?.takeIf { it.packageName != "android" }
                ?.let { ai -> AppInfo(ai.loadLabel(pm).toString(), ComponentName(ai.packageName, ai.name)) }
        }.distinctBy { it.component }

        val acts = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.anddoes.launcher.THEME", "fr.neamar.kiss.THEME")
        val found = linkedMapOf<String, IconPack>()
        acts.forEach { a ->
            pm.queryIntentActivities(Intent(a), 0).forEach {
                val p = it.activityInfo.packageName
                found[p] = IconPack(p, it.loadLabel(pm).toString())
            }
        }
        packs.value = found.values.toList()
    }

    private fun loadPack(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            packKey = pkg; packRes = null; packMap = emptyMap(); cache.evictAll()
            if (pkg.isNotEmpty()) runCatching {
                val res = pm.getResourcesForApplication(pkg)
                val map = HashMap<String, String>()
                val id = res.getIdentifier("appfilter", "xml", pkg)
                val parser: XmlPullParser = if (id != 0) res.getXml(id) else
                    XmlPullParserFactory.newInstance().newPullParser().also {
                        it.setInput(res.assets.open("appfilter.xml"), "utf-8")
                    }
                var ev = parser.eventType
                while (ev != XmlPullParser.END_DOCUMENT) {
                    if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                        val c = parser.getAttributeValue(null, "component")
                        val d = parser.getAttributeValue(null, "drawable")
                        if (c != null && d != null) {
                            val s = c.substringAfter("{", "").substringBefore("}")
                            if (s.contains("/")) {
                                val p = s.substringBefore("/")
                                val cls = s.substringAfter("/").let { if (it.startsWith(".")) p + it else it }
                                map["$p/$cls"] = d
                            }
                        }
                    }
                    ev = parser.next()
                }
                packRes = res; packMap = map
            }
            iconVersion.value++
        }
    }

    /** Called off the main thread by the UI. Cached, 144px bitmaps keep scrolling smooth. */
    fun iconFor(a: AppInfo): ImageBitmap {
        val key = "$packKey|${a.component.flattenToString()}"
        cache.get(key)?.let { return it }
        var d: Drawable? = null
        val name = packMap["${a.component.packageName}/${a.component.className}"]
        val res = packRes
        if (name != null && res != null) {
            val id = res.getIdentifier(name, "drawable", packKey)
            if (id != 0) d = runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
        }
        if (d == null) d = runCatching { pm.getActivityIcon(a.component) }.getOrNull()
        val bmp = (d ?: pm.defaultActivityIcon).toBitmap(144, 144).asImageBitmap()
        cache.put(key, bmp)
        return bmp
    }

    suspend fun load(a: AppInfo) = withContext(Dispatchers.Default) { iconFor(a) }

    fun launch(ctx: Context, a: AppInfo) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(a.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun setBlur(v: Int) = viewModelScope.launch { app.saveBlur(v) }
    fun setAlpha(v: Float) = viewModelScope.launch { app.saveAlpha(v) }
    fun setPack(v: String) = viewModelScope.launch { app.savePack(v) }
    fun setName(v: String) = viewModelScope.launch { app.saveName(v) }
}
