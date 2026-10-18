package eu.kanade.tachiyomi.extension.anime.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dalvik.system.PathClassLoader
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.system.ChildFirstPathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Loads installed aniyomi-compatible anime extensions (APKs that declare the
 * `tachiyomi.animeextension` feature). Only system-installed packages are loaded.
 */
object AnimeExtensionLoader {

    private const val EXTENSION_FEATURE = "tachiyomi.animeextension"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.animeextension.class"
    private const val METADATA_NSFW = "tachiyomi.animeextension.nsfw"
    private const val METADATA_NAME = "aniyomix.name"
    private const val METADATA_EXTENSION_LIB = "aniyomix.extensionLib"
    private const val METADATA_CONTENT_WARNING = "aniyomix.contentWarning"

    private val SUPPORTED_LIB_VERSIONS = listOf(14.0, 16.0, 17.0)

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA or
        PackageManager.GET_SIGNATURES or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)

    /**
     * Loads every installed anime extension concurrently.
     */
    @Suppress("DEPRECATION")
    suspend fun loadInstalled(context: Context): List<AnimeLoadResult> {
        val pkgManager = context.packageManager

        val installedPkgs: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val extPkgs = installedPkgs.filter { isPackageAnExtension(it) }
        if (extPkgs.isEmpty()) return emptyList()

        return coroutineScope {
            extPkgs.map { pkg -> async { loadExtension(context, pkg) } }.awaitAll()
        }
    }

    private suspend fun loadExtension(context: Context, pkgInfo: PackageInfo): AnimeLoadResult {
        val trustExtension = Injekt.get<TrustExtension>()
        val sourcePreferences = Injekt.get<SourcePreferences>()
        val pkgManager = context.packageManager

        val appInfo = pkgInfo.applicationInfo ?: return AnimeLoadResult.Error
        val pkgName = pkgInfo.packageName
        val meta = appInfo.metaData ?: return AnimeLoadResult.Error

        val extName = meta.getString(METADATA_NAME)
            ?: pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Aniyomi: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Missing versionName for anime extension $extName" }
            return AnimeLoadResult.Error
        }

        // Validate lib version
        val libVersion = meta.getInt(METADATA_EXTENSION_LIB)
            .takeUnless { it == 0 }
            ?.toString()
            ?.toDoubleOrNull()
            ?: versionName.substringBeforeLast('.').toDoubleOrNull()
        if (libVersion == null || libVersion !in SUPPORTED_LIB_VERSIONS) {
            logcat(LogPriority.WARN) {
                "Anime lib version is $libVersion, while only version(s) ${SUPPORTED_LIB_VERSIONS.joinToString()} are supported"
            }
            return AnimeLoadResult.Error
        }

        val signatures = getSignatures(pkgInfo)
        if (signatures.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Anime package $pkgName isn't signed" }
            return AnimeLoadResult.Error
        } else if (!trustExtension.isTrusted(pkgInfo, signatures)) {
            logcat(LogPriority.WARN) { "Anime extension $pkgName isn't trusted" }
            return AnimeLoadResult.Untrusted(
                AnimeExtension.Untrusted(
                    name = extName,
                    pkgName = pkgName,
                    versionName = versionName,
                    versionCode = versionCode,
                    libVersion = libVersion,
                    signatureHash = signatures.last(),
                ),
            )
        }

        val isNsfw = meta.getInt(METADATA_CONTENT_WARNING) > 0 || meta.getInt(METADATA_NSFW) == 1
        if (isNsfw && !sourcePreferences.showNsfwSource.get()) {
            logcat(LogPriority.WARN) { "NSFW anime extension $pkgName not allowed" }
            return AnimeLoadResult.Error
        }

        val classLoader = try {
            ChildFirstPathClassLoader(appInfo.sourceDir, null, context.classLoader)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Anime extension load error: $extName ($pkgName)" }
            return AnimeLoadResult.Error
        }

        val sourceClasses = meta.getString(METADATA_SOURCE_CLASS).orEmpty()
            .split(";")
            .map { raw ->
                val sourceClass = raw.trim()
                if (sourceClass.startsWith(".")) pkgName + sourceClass else sourceClass
            }

        val sources: List<AnimeSource> = sourceClasses.flatMap { className ->
            try {
                instantiate(className, classLoader)
            } catch (e: LinkageError) {
                try {
                    instantiate(className, PathClassLoader(appInfo.sourceDir, null, context.classLoader))
                } catch (fallbackError: Throwable) {
                    logcat(LogPriority.ERROR, fallbackError) { "Anime extension load error: $extName ($className)" }
                    return AnimeLoadResult.Error
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Anime extension load error: $extName ($className)" }
                return AnimeLoadResult.Error
            }
        }

        val langs = sources.map { it.lang }.toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        return AnimeLoadResult.Success(
            AnimeExtension.Installed(
                name = extName,
                pkgName = pkgName,
                versionName = versionName,
                versionCode = versionCode,
                libVersion = libVersion,
                lang = lang,
                isNsfw = isNsfw,
                sources = sources,
            ),
        )
    }

    private fun instantiate(className: String, classLoader: ClassLoader): List<AnimeSource> {
        return when (val obj = Class.forName(className, false, classLoader).getDeclaredConstructor().newInstance()) {
            is AnimeSource -> listOf(obj)
            is AnimeSourceFactory -> obj.createSources()
            else -> throw Exception("Unknown anime source class type: ${obj.javaClass}")
        }
    }

    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    /**
     * Returns the SHA256 digests of the package signatures, or null if it's not signed.
     */
    @Suppress("DEPRECATION")
    private fun getSignatures(pkgInfo: PackageInfo): List<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            pkgInfo.signatures
        }
            ?.map { Hash.sha256(it.toByteArray()) }
            ?.toList()
    }
}
