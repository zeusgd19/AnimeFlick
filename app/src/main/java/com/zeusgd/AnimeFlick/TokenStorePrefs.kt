import android.content.Context

class TokenStorePrefs(context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    override  fun saveTokens(access: String, refresh: String?) {
        prefs.edit()
            .putString("access_token", access)
            .apply()
        if (refresh != null) {
            prefs.edit().putString("refresh_token", refresh).apply()
        }
    }

    override  fun getAccessToken(): String? = prefs.getString("access_token", null)

    override  fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    override  fun getUserId(): String? = prefs.getString("user_id", null)

    override  fun clear() {
        prefs.edit().clear().apply()
    }
}

interface TokenStore {
     fun saveTokens(access: String, refresh: String?)
     fun getAccessToken(): String?
     fun getRefreshToken(): String?
     fun getUserId(): String?

     fun clear()
}
