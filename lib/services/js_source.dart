import 'dart:convert';
import 'package:flutter_js/flutter_js.dart';
import 'package:dio/dio.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../models/source.dart';

/// A [Source] whose logic lives in a downloaded JS file rather than
/// compiled Dart. This is the "extension" mechanism: installing an
/// extension = downloading a `.js` module + a `manifest.json` and
/// registering both here.
///
/// The JS module contract (what an extension author implements):
///
/// ```js
/// class Extension {
///   async search(query, page) { ... return [{id,title,cover,...}] }
///   async popular(page) { ... }
///   async latest(page) { ... }
///   async details(id) { ... return {id,title,description,genres,...} }
///   async chunks(id) { ... return [{id,title,number,uploadDate}] }
///   async pages(chunkId) { ... return ["https://...jpg", ...] }
///   async streams(chunkId) { ... return [{url,quality,headers}] }
/// }
/// register(new Extension());
/// ```
///
/// This shape is deliberately close to Mangayomi's JS source API and to
/// Mihon's per-source method set (popularMangaRequest/searchMangaRequest/
/// chapterListRequest/pageListRequest), so porting an existing open-source
/// extension's *scraping logic* here is mostly a syntax translation, not a
/// rewrite of the approach. Actual porting is still per-source manual work
/// — there is no automatic loader for .apk or .mmrpm packages.
class JsSource implements Source {
  final ExtensionManifest manifest;
  late final JavascriptRuntime _js;
  final Dio _dio = Dio();
  bool _ready = false;

  JsSource(this.manifest);

  @override
  String get id => manifest.id;
  @override
  String get name => manifest.name;
  @override
  String get lang => manifest.lang;
  @override
  ContentType get type => manifest.type;
  @override
  String get iconUrl => manifest.iconUrl;
  @override
  int get version => manifest.version;

  Future<void> _ensureReady() async {
    if (_ready) return;
    _js = getJavascriptRuntime();

    // Bridge: gives the JS module an async-capable HTTP client backed by
    // Dio, since flutter_js has no native fetch(). The JS side calls
    // `__httpGet(url, headersJson)` and awaits a promise we resolve here.
    _js.onMessage('__httpGet', (args) async {
      final url = args[0] as String;
      final headers = jsonDecode(args[1] as String) as Map<String, dynamic>;
      final res = await _dio.get<String>(url,
          options: Options(headers: headers, responseType: ResponseType.plain));
      return res.data;
    });

    final code = await _dio.get<String>(
      manifest.scriptUrl,
      options: Options(responseType: ResponseType.plain),
    );
    _js.evaluate('''
      const __pending = {};
      let __id = 0;
      globalThis.httpGet = (url, headers = {}) => new Promise((resolve) => {
        const rid = __id++;
        __pending[rid] = resolve;
        sendMessage('__httpGet', url, JSON.stringify(headers))
          .then(r => { __pending[rid](r); delete __pending[rid]; });
      });
      var module = {};
      ${code.data}
    ''');
    _ready = true;
  }

  Future<dynamic> _call(String fn, List<dynamic> args) async {
    await _ensureReady();
    final argsJs = args.map(jsonEncode).join(',');
    final result = await _js.evaluateAsync('JSON.stringify(await module.$fn($argsJs))');
    return jsonDecode(result.stringResult);
  }

  @override
  Future<List<Entry>> search(String query, {int page = 1}) async {
    final list = await _call('search', [query, page]) as List;
    return list.map((e) => Entry.fromJson(e, id, type)).toList();
  }

  @override
  Future<List<Entry>> popular({int page = 1}) async {
    final list = await _call('popular', [page]) as List;
    return list.map((e) => Entry.fromJson(e, id, type)).toList();
  }

  @override
  Future<List<Entry>> latest({int page = 1}) async {
    final list = await _call('latest', [page]) as List;
    return list.map((e) => Entry.fromJson(e, id, type)).toList();
  }

  @override
  Future<Entry> getEntryDetails(String entryId) async {
    final json = await _call('details', [entryId]);
    return Entry.fromJson(json, id, type);
  }

  @override
  Future<List<EntryChunk>> getChunks(String entryId) async {
    final list = await _call('chunks', [entryId]) as List;
    return list
        .map((c) => EntryChunk(
              id: c['id'],
              entryId: entryId,
              title: c['title'],
              number: (c['number'] as num).toDouble(),
              uploadDate: c['uploadDate'] != null ? DateTime.tryParse(c['uploadDate']) : null,
            ))
        .toList();
  }

  @override
  Future<List<String>> getPages(String chunkId) async {
    final list = await _call('pages', [chunkId]) as List;
    return list.cast<String>();
  }

  @override
  Future<List<StreamLink>> getStreamLinks(String chunkId) async {
    final list = await _call('streams', [chunkId]) as List;
    return list
        .map((s) => StreamLink(
              url: s['url'],
              quality: s['quality'] ?? 'default',
              headers: (s['headers'] as Map?)?.cast<String, String>() ?? const {},
            ))
        .toList();
  }
}

class ExtensionManifest {
  final String id;
  final String name;
  final String lang;
  final ContentType type;
  final String iconUrl;
  final String scriptUrl;
  final int version;

  ExtensionManifest({
    required this.id,
    required this.name,
    required this.lang,
    required this.type,
    required this.iconUrl,
    required this.scriptUrl,
    required this.version,
  });

  factory ExtensionManifest.fromJson(Map<String, dynamic> j) => ExtensionManifest(
        id: j['id'],
        name: j['name'],
        lang: j['lang'] ?? 'en',
        type: ContentType.values.firstWhere((t) => t.name == j['type']),
        iconUrl: j['icon'] ?? '',
        scriptUrl: j['script'],
        version: j['version'] ?? 1,
      );
}
