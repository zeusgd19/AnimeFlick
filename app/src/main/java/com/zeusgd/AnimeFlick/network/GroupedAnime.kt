import com.zeusgd.AnimeFlick.model.AiringAnime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

suspend fun getAiringAnimesGroupedByWeekday(): Map<String, List<AiringAnime>> = withContext(Dispatchers.IO) {
    val baseUrl = "https://animeflv.ahmedrangel.com/api"
    val grouped = mutableMapOf<String, MutableList<AiringAnime>>()

    try {
        val listUrl = URL("$baseUrl/list/animes-on-air")
        val listResponse = listUrl.readTextWithUserAgent()
        val animesArray = JSONObject(listResponse).getJSONArray("data")

        for (i in 0 until animesArray.length()) {
            val anime = animesArray.getJSONObject(i)
            val slug = anime.getString("slug")
            val title = anime.getString("title")

            val infoUrl = URL("$baseUrl/anime/$slug")
            val infoResponse = infoUrl.readTextWithUserAgent()
            val infoData = JSONObject(infoResponse).getJSONObject("data")

            val airingDate = infoData.optString("next_airing_episode", null)
            val cover = infoData.optString("cover", "")

            if (!airingDate.isNullOrEmpty()) {
                val weekday = getWeekdayFromDate(airingDate)
                val list = grouped.getOrPut(weekday) { mutableListOf() }
                list.add(AiringAnime(title, slug, airingDate, cover))
            }
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

fun getWeekdayFromDate(dateStr: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateStr)
        SimpleDateFormat("EEEE", Locale("es", "ES")).format(date ?: Date())
    } catch (e: Exception) {
        "Desconocido"
    }
}
