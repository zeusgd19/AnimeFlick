import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tag: String,
    val assets: List<GitHubAsset>
)

fun getInstalledVersion(context: Context): String? {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "0.0.0"
    }
}

suspend fun checkForUpdateFromGitHub(context: Context): String? {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val response = client.get("https://api.github.com/repos/zeusgd19/AnimeFlick/releases/latest")
    val release = response.body<GitHubRelease>()

    val currentVersion = getInstalledVersion(context)
    val remoteVersion = release.tag.removePrefix("v")

    return if (currentVersion != remoteVersion) {
        release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl
    } else {
        null
    }
}

@RequiresApi(TIRAMISU)
fun downloadAndInstall(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    request.setTitle("Actualización de AnimeFlick")
    request.setDescription("Descargando nueva versión...")
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AnimeFlick-latest.apk")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.setDataAndType(
                manager.getUriForDownloadedFile(downloadId),
                "application/vnd.android.package-archive"
            )
            installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(installIntent)
        }
    }
    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        Context.RECEIVER_NOT_EXPORTED)
}