package tr.kolayhatli.kuran

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.content.Context
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val Green = Color(0xFF174A3A)
private val Paper = Color(0xFFF8F3E8)
private val Ink = Color(0xFF28271F)
private val Gold = Color(0xFFB58A43)

private data class Ayah(val number: Int, val surah: Int, val inSurah: Int, val arabic: String, val translation: String)

class MainActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuranApp() }
    }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }

    private fun cacheFile(page: Int, edition: String) = File(filesDir, "page_${page}_${edition.replace('.', '_')}.json")
    private fun fetchPage(page: Int, edition: String): String {
        val file = cacheFile(page, edition)
        if (file.exists()) return file.readText(Charsets.UTF_8)
        val url = URL("https://api.alquran.cloud/v1/page/$page/$edition")
        val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 20000; requestMethod = "GET" }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("Sunucu hatası: $code")
            val parsed = JSONObject(body)
            if (parsed.optString("status") != "OK") throw IllegalStateException("İçerik kaynağı geçerli yanıt vermedi.")
            file.writeText(body, Charsets.UTF_8)
            return body
        } finally { conn.disconnect() }
    }

    private suspend fun loadPage(page: Int): List<Ayah> = withContext(Dispatchers.IO) {
        val arabic = JSONObject(fetchPage(page, "quran-uthmani")).getJSONObject("data").getJSONArray("ayahs")
        val turkish = try { JSONObject(fetchPage(page, "tr.diyanet")).getJSONObject("data").getJSONArray("ayahs") } catch (_: Exception) { null }
        (0 until arabic.length()).map { i ->
            val a = arabic.getJSONObject(i)
            val t = if (turkish != null && i < turkish.length()) turkish.getJSONObject(i).optString("text", "") else ""
            Ayah(a.optInt("number"), a.optJSONObject("surah")?.optInt("number") ?: 1, a.optInt("numberInSurah"), a.optString("text"), t)
        }
    }

    private suspend fun downloadOffline(onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        for (p in 1..604) {
            fetchPage(p, "quran-uthmani")
            try { fetchPage(p, "tr.diyanet") } catch (_: Exception) { /* Meal may be unavailable; Arabic is still cached. */ }
            withContext(Dispatchers.Main) { onProgress(p) }
        }
    }

    private fun playAyah(number: Int) {
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource("https://cdn.islamic.network/quran/audio/128/ar.alafasy/$number.mp3")
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, _, _ -> Toast.makeText(this@MainActivity, "Ses yüklenemedi. İnternet bağlantısını kontrol et.", Toast.LENGTH_LONG).show(); true }
            prepareAsync()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun QuranApp() {
        val context = LocalContext.current
        val prefs = remember { getSharedPreferences("quran_progress", Context.MODE_PRIVATE) }
        var page by remember { mutableIntStateOf(prefs.getInt("last_page", 1).coerceIn(1,604)) }
        var bookmark by remember { mutableIntStateOf(prefs.getInt("bookmark_page", 1).coerceIn(1,604)) }
        var tab by remember { mutableStateOf("Mushaf") }
        var ayahs by remember { mutableStateOf<List<Ayah>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableIntStateOf(0) }
        var error by remember { mutableStateOf<String?>(null) }
        var showMeal by remember { mutableStateOf(true) }
        var search by remember { mutableStateOf("") }
        var selectedLesson by remember { mutableIntStateOf(0) }

        fun requestPage(target: Int) {
            page = target.coerceIn(1,604); prefs.edit().putInt("last_page", page).apply()
            loading = true; error = null
            lifecycleScope.launch {
                try { ayahs = loadPage(page) } catch (e: Exception) { error = "Sayfa yüklenemedi: ${e.message ?: "Bağlantıyı kontrol et."}" }
                finally { loading = false }
            }
        }
        LaunchedEffect(Unit) { requestPage(page) }

        MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Paper, surface = Paper, secondary = Gold)) {
            Scaffold(
                topBar = { TopAppBar(title = { Column { Text("Kolay Hatlı Kur'an", fontWeight = FontWeight.Bold); Text("Oku · Dinle · Öğren", fontSize = 12.sp) } }, actions = { IconButton(onClick = { tab = "İndir" }) { Icon(Icons.Default.Download, "Çevrimdışı indir") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, actionIconContentColor = Color.White)) },
                bottomBar = { NavigationBar(containerColor = Color(0xFFEDE5D5)) {
                    listOf("Mushaf" to Icons.Default.MenuBook, "Meal" to Icons.Default.Bookmark, "Öğren" to Icons.Default.School, "Dinle" to Icons.Default.Headphones).forEach { (label, icon) ->
                        NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                } }, containerColor = Paper
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    when (tab) {
                        "Mushaf", "Meal" -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(if (tab == "Mushaf") "MUSHAF SAYFASI" else "AYET VE MEAL", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Sayfa $page / 604", fontSize = 21.sp, color = Ink, fontWeight = FontWeight.Bold) }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { requestPage(bookmark) }) { Icon(Icons.Default.Bookmark, "Yer imine git", tint = Green) }
                                    IconButton(onClick = { bookmark = page; prefs.edit().putInt("bookmark_page", page).apply(); Toast.makeText(context, "$page. sayfa kaydedildi", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.BookmarkAdd, "Yer imi ekle", tint = Green) }
                                }
                            }
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)
                            error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                            if (ayahs.isEmpty() && !loading && error == null) Text("İçerik hazırlanıyor…", Modifier.padding(20.dp))
                            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                                if (ayahs.isNotEmpty()) item {
                                    Column(Modifier.fillMaxWidth().background(Color(0xFFFFFCF5), RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("KUR'AN-I KERİM", color = Green, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                        Text("Sayfa $page", color = Gold, fontSize = 12.sp)
                                        Text("Metin kaynağı: Tanzil Project · Sayfa verisi: AlQuran Cloud", color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center)
                                    }
                                }
                                items(ayahs, key = { "${it.number}-${tab}" }) { a ->
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)), border = BorderStroke(0.5.dp, Color(0xFFE3D7C2)), shape = RoundedCornerShape(12.dp)) {
                                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("${a.surah}:${a.inSurah}", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { playAyah(a.number) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.PlayArrow, "Ayet sesini çal", tint = Green) }
                                            }
                                            Text(a.arabic, Modifier.fillMaxWidth(), color = Ink, fontSize = 27.sp, lineHeight = 48.sp, textAlign = TextAlign.End)
                                            if (showMeal || tab == "Meal") {
                                                if (a.translation.isNotBlank()) Text(a.translation, color = Color(0xFF5D574B), fontSize = 15.sp, lineHeight = 23.sp)
                                                else Text("Meal çevrimdışı değil veya kaynakta bulunamadı.", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { requestPage(page - 1) }, enabled = page > 1, modifier = Modifier.weight(1f)) { Icon(Icons.Default.NavigateBefore, null); Text("Önceki") }
                                OutlinedButton(onClick = { showMeal = !showMeal }, modifier = Modifier.weight(1f)) { Text(if (showMeal) "Meali gizle" else "Meali göster") }
                                Button(onClick = { requestPage(page + 1) }, enabled = page < 604, modifier = Modifier.weight(1f)) { Text("Sonraki"); Icon(Icons.Default.NavigateNext, null) }
                            }
                        }
                        "Öğren" -> LearningScreen(selectedLesson) { selectedLesson = it }
                        "Dinle" -> ListenScreen(onPlay = { playAyah(it) }, onStop = { player?.pause() })
                        else -> {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Çevrimdışı içerik", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Green)
                                Text("İndirme tamamlandığında ziyaret edilen mushaf sayfaları ve mealler cihazda saklanır. Tüm 604 sayfayı indirmek için internet bağlantısı ve yeterli depolama alanı gerekir.")
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5))) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (downloading) "İndiriliyor: $downloadProgress / 604 sayfa" else "Tüm sayfaları indir", fontWeight = FontWeight.Bold)
                                    if (downloading) LinearProgressIndicator(progress = { downloadProgress / 604f }, modifier = Modifier.fillMaxWidth(), color = Green)
                                    Button(enabled = !downloading, onClick = {
                                        downloading = true; downloadProgress = 0
                                        lifecycleScope.launch { try { downloadOffline { downloadProgress = it } ; Toast.makeText(context, "İndirme tamamlandı", Toast.LENGTH_LONG).show() } catch (e: Exception) { Toast.makeText(context, "İndirme kesildi: ${e.message}", Toast.LENGTH_LONG).show() } finally { downloading = false } }
                                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(if (downloading) "İndiriliyor…" else "604 sayfayı çevrimdışı indir") }
                                    Text("İndirme sırasında uygulamayı açık tut. Meal sağlayıcısı yanıt vermezse Arapça metin yine kaydedilir.", fontSize = 12.sp, color = Color.Gray)
                                } }
                                Text("Veri kaynakları", fontWeight = FontWeight.Bold, color = Green)
                                Text("Arapça metin: Tanzil Project (Uthmani). Bu uygulama metni değiştirmez ve kaynak bildirimi sağlar. Veri aktarımı AlQuran Cloud API üzerinden yapılır. Ses: Mishary Rashid Alafasy, CDN üzerinden yayınlanır.", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun LearningScreen(selected: Int, onSelect: (Int) -> Unit) {
    val lessons = listOf(
        "Harfleri tanıma" to "Arapça harfler sağdan sola yazılır. Harfin kelimenin başında, ortasında ve sonunda aldığı biçimler değişebilir. Harfleri tek tek tanıyıp doğru seslendirmeyle çalış.",
        "Hareke ve uzatma" to "Üstün kısa 'a/e', esre kısa 'i', ötre kısa 'u/ü' sesini gösterir. Med harfleri ve ilgili kurallar uzatmayı belirler; süreyi bir öğreticiyle dinleyerek pekiştir.",
        "Nun sakin ve tenvin" to "Temel başlıklar izhar, idgam, iklab ve ihfadır. Hüküm sonraki harfe göre değişir; örnekleri güvenilir bir tecvid hocasından dinleyerek öğren.",
        "Mim sakin" to "İhfa-i şefevi, idgam-ı misleyn ve izhar-ı şefevi başlıklarını çalış. Dudak mahreci ve uygulama dinleyerek öğrenilmelidir.",
        "Kalkale ve vakıf" to "Kalkale harfleri ق ط ب ج د olarak öğretilir. Durak işaretleri okumanın nerede duracağını gösterir; mushaf işaretlerini öğrenirken bir hocaya danış."
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tecvid ve harf öğrenme", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Green)
        Text("Temel çalışma notları. Doğru mahreç ve kıraat için ehil bir öğreticiden dinleyerek öğrenmen önerilir.", color = Color(0xFF625D52))
        lessons.forEachIndexed { i, lesson ->
            Card(onClick = { onSelect(i) }, colors = CardDefaults.cardColors(containerColor = if (selected == i) Color(0xFFE8EEDC) else Color(0xFFFFFCF5))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${i + 1}. ${lesson.first}", color = Green, fontWeight = FontWeight.Bold)
                    if (selected == i) Text(lesson.second, color = Ink, lineHeight = 23.sp) else Text("Dersi açmak için dokun.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable private fun ListenScreen(onPlay: (Int) -> Unit, onStop: () -> Unit) {
    var ayahNumber by remember { mutableStateOf("1") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Sesli Kur'an", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Green)
        Text("Mishary Rashid Alafasy okuyuşu. Sesler internet üzerinden açılır; ilk sürümde ses dosyaları topluca çevrimdışı indirilmez.")
        OutlinedTextField(value = ayahNumber, onValueChange = { ayahNumber = it.filter(Char::isDigit).take(4) }, label = { Text("Ayet numarası (1–6236)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { val n = ayahNumber.toIntOrNull(); if (n != null && n in 1..6236) onPlay(n) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Ayet sesini çal") }
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(8.dp)); Text("Duraklat") }
        Text("Not: Ayet numarası, Kur'an genelindeki sıralı numaradır. Ses akışı bağlantıya bağlıdır.", fontSize = 12.sp, color = Color.Gray)
    }
}
