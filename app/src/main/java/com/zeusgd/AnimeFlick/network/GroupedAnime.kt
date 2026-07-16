import com.zeusgd.AnimeFlick.model.AiringAnime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun getAiringAnimesGroupedByWeekday(): Map<String, List<AiringAnime>> = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://animeflick.com/api/airing-animes")
        val json = url.readTextWithUserAgent()
        val root = JSONObject(json)

        val grouped = mutableMapOf<String, List<AiringAnime>>()
        root.keys().forEach { day ->
            val array = root.getJSONArray(day)
            val list = mutableListOf<AiringAnime>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AiringAnime(
                        title = obj.getString("title"),
                        slug = obj.getString("slug"),
                        airingData = obj.optString("airingData", "On Air"),
                        cover = obj.optString("cover", "")
                    )
                )
            }
            grouped[day] = list
        }

        return@withContext grouped
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext emptyMap()
    }
}

fun URL.readTextWithUserAgent(): String {
    val conn = this.openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    return conn.inputStream.bufferedReader().use { it.readText() }
}
