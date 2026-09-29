package eu.kanade.tachiyomi.kaimono

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mihon.domain.extension.interactor.AddExtensionStore
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Bridge between the Kaimono Flutter UI and Wammy's extension engine.
 *
 * Conventions:
 *  - source ids are sent as Strings (they are Longs in Wammy, too big for Dart ints on web-safe paths)
 *  - manga are identified by SManga.url, chapters by SChapter.url
 *  - map keys for manga/chapters match Kaimono's Entry.fromJson / EntryChunk fields
 */
object EngineChannel {

    private const val NAME = "com.kaimono/engine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val extensions: ExtensionManager get() = Injekt.get<ExtensionManager>()
    private val sources: SourceManager get() = Injekt.get<SourceManager>()

    fun register(messenger: BinaryMessenger) {
        MethodChannel(messenger, NAME).setMethodCallHandler { call, result ->
            scope.launch {
                try {
                    val out = handle(call)
                    withContext(Dispatchers.Main) { result.success(out) }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        result.error("ENGINE_ERROR", e.message ?: e.toString(), null)
                    }
                }
            }
        }
    }

    private suspend fun handle(call: MethodCall): Any? = when (call.method) {
        // ---- lifecycle -------------------------------------------------------------
        "init" -> {
            extensions.isInitialized.first { it }
            sources.isInitialized.first { it }
            true
        }

        // ---- extensions ------------------------------------------------------------
        "listInstalled" -> extensions.installedExtensionsFlow.value.map { it.toMap() }

        "listAvailable" -> {
            extensions.findAvailableExtensions()
            extensions.availableExtensionsFlow.value.map { it.toMap() }
        }

        "listUntrusted" -> extensions.untrustedExtensionsFlow.value.map {
            mapOf("pkgName" to it.pkgName, "name" to it.name, "versionName" to it.versionName)
        }

        "trust" -> {
            val pkg = call.arg<String>("pkgName")
            extensions.untrustedExtensionsFlow.value.firstOrNull { it.pkgName == pkg }
                ?.let { extensions.trust(it) }
            true
        }

        "install" -> {
            val pkg = call.arg<String>("pkgName")
            val ext = extensions.availableExtensionsFlow.value.firstOrNull { it.pkgName == pkg }
                ?: error("Extension $pkg is not in any repository")
            // Wammy's installer shows Android's own install prompt; wait for the outcome.
            val step = withTimeoutOrNull(5.minutes) {
                extensions.installExtension(ext)
                    .first { it == InstallStep.Installed || it == InstallStep.Error }
            }
            (step ?: InstallStep.Error).name
        }

        "uninstall" -> {
            val pkg = call.arg<String>("pkgName")
            val ext: Extension = extensions.installedExtensionsFlow.value.firstOrNull { it.pkgName == pkg }
                ?: extensions.untrustedExtensionsFlow.value.firstOrNull { it.pkgName == pkg }
                ?: error("Extension $pkg is not installed")
            extensions.uninstallExtension(ext)
            true
        }

        "addStore" -> {
            Injekt.get<AddExtensionStore>()
                .invoke(call.arg("indexUrl"), call.argument<Boolean>("isNovel") ?: false)
                .getOrThrow()
            true
        }

        // ---- browsing --------------------------------------------------------------
        "popular" -> pageOf(call) { s, p -> s.getPopularManga(p) }
        "latest" -> pageOf(call) { s, p -> s.getLatestUpdates(p) }
        "search" -> {
            val query = call.arg<String>("query")
            pageOf(call) { s, p -> s.getSearchManga(p, query, s.getFilterList()) }
        }

        // details + chapter list in one round trip
        "details" -> {
            val s = source(call)
            val manga = SManga.create().apply {
                url = call.arg("url")
                title = call.argument<String>("title").orEmpty()
            }
            val update = s.getMangaUpdate(
                manga = manga,
                chapters = emptyList(),
                fetchDetails = true,
                fetchChapters = true,
            )
            mapOf(
                "manga" to update.manga.toMap(),
                "chapters" to update.chapters.map { it.toMap() },
            )
        }

        // ---- reading ---------------------------------------------------------------
        "pages" -> {
            val s = source(call)
            val chapter = SChapter.create().apply {
                url = call.arg("chapterUrl")
                name = call.argument<String>("chapterName").orEmpty()
            }
            s.getPageList(chapter).mapIndexed { i, p ->
                mapOf("index" to i, "url" to p.url, "imageUrl" to p.imageUrl)
            }
        }

        // novels: one call per page returns the text/HTML
        "pageText" -> {
            val s = source(call)
            val page = Page(
                call.argument<Int>("index") ?: 0,
                call.argument<String>("url").orEmpty(),
                call.argument<String>("imageUrl"),
            )
            s.fetchPageText(page)
        }

        // covers and page images, fetched with the source's own client/headers/cookies
        "image" -> {
            val http = source(call) as? HttpSource ?: error("Source is not an HttpSource")
            if (call.argument<Boolean>("cover") == true) {
                val url = call.arg<String>("imageUrl")
                http.client.newCall(GET(url, http.headers)).awaitSuccess().use { it.body.bytes() }
            } else {
                val page = Page(
                    call.argument<Int>("index") ?: 0,
                    call.argument<String>("url").orEmpty(),
                    call.argument<String>("imageUrl"),
                )
                if (page.imageUrl.isNullOrEmpty()) page.imageUrl = http.getImageUrl(page)
                http.getImage(page).use { it.body.bytes() }
            }
        }

        else -> throw NotImplementedError("Unknown method ${call.method}")
    }

    // ---- helpers -------------------------------------------------------------------

    private fun <T> MethodCall.arg(key: String): T =
        argument<T>(key) ?: throw IllegalArgumentException("Missing argument: $key")

    private fun source(call: MethodCall): Source {
        val id = call.arg<String>("sourceId").toLong()
        return sources.get(id) ?: error("Source $id is not loaded (extension missing or untrusted?)")
    }

    private suspend fun pageOf(
        call: MethodCall,
        block: suspend (Source, Int) -> MangasPage,
    ): Map<String, Any?> {
        val result = block(source(call), call.argument<Int>("page") ?: 1)
        return mapOf(
            "mangas" to result.mangas.map { it.toMap() },
            "hasNextPage" to result.hasNextPage,
        )
    }

    // ---- mappers (keys line up with Kaimono's Entry.fromJson / EntryChunk) ----------

    private fun SManga.toMap(): Map<String, Any?> = mapOf(
        "id" to url,
        "title" to title,
        "cover" to thumbnail_url,
        "description" to description,
        "genres" to (getGenres() ?: emptyList<String>()),
        "author" to author,
        "artist" to artist,
        "status" to when (status) {
            SManga.ONGOING -> "ongoing"
            SManga.COMPLETED, SManga.PUBLISHING_FINISHED -> "completed"
            SManga.ON_HIATUS -> "hiatus"
            SManga.CANCELLED -> "cancelled"
            else -> "unknown"
        },
    )

    private fun SChapter.toMap(): Map<String, Any?> = mapOf(
        "id" to url,
        "title" to name,
        "number" to chapter_number.toDouble(),
        "uploadDate" to if (date_upload > 0) Instant.ofEpochMilli(date_upload).toString() else null,
        "scanlator" to scanlator,
    )

    private fun Source.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "name" to name,
        "lang" to lang,
        "type" to if (isNovelSource) "novel" else "manga",
        "supportsLatest" to supportsLatest,
    )

    private fun Extension.toMap(): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "pkgName" to pkgName,
            "name" to name,
            "versionName" to versionName,
            "versionCode" to versionCode,
            "lang" to lang,
            "isNsfw" to isNsfw,
            "isNovel" to isNovel,
        )
        when (this) {
            is Extension.Installed -> {
                base["hasUpdate"] = hasUpdate
                base["sources"] = sources.map { it.toMap() }
            }
            is Extension.Available -> {
                base["iconUrl"] = iconUrl
                base["sources"] = sources.map {
                    mapOf(
                        "id" to it.id.toString(),
                        "name" to it.name,
                        "lang" to it.lang,
                        "type" to if (isNovel) "novel" else "manga",
                    )
                }
            }
            else -> Unit
        }
        return base
    }
}
