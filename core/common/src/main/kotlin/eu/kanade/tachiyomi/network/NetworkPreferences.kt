package eu.kanade.tachiyomi.network

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class NetworkPreferences(
    preferenceStore: PreferenceStore,
    verboseLoggingDefault: Boolean = false,
) {

    val verboseLogging: Preference<Boolean> = preferenceStore.getBoolean("verbose_logging", verboseLoggingDefault)

    // 1 = Cloudflare DNS-over-HTTPS (PREF_DOH_CLOUDFLARE). Was -1 (off). Turn it off in Settings > Advanced.
    val dohProvider: Preference<Int> = preferenceStore.getInt("doh_provider", 1)

    val defaultUserAgent: Preference<String> = preferenceStore.getString(
        "default_user_agent",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36",
    )
}
