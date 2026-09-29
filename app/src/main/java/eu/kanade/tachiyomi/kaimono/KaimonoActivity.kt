package eu.kanade.tachiyomi.kaimono

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import mihon.core.migration.Migrator

/**
 * The only screen the user ever sees. It hosts the Kaimono Flutter UI and exposes
 * Wammy's extension/source engine to it over a MethodChannel.
 */
class KaimonoActivity : FlutterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Same job MainActivity did: wait for Wammy's preference/DB migrations before
        // anything touches extensions or sources.
        Migrator.awaitAndRelease()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        EngineChannel.register(flutterEngine.dartExecutor.binaryMessenger)
    }
}
