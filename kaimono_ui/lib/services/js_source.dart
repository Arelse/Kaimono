import 'dart:convert';
import 'package:flutter_js/flutter_js.dart';
import 'package:dio/dio.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../models/source.dart';

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

    _js.onMessage('__httpGetStart', (args) {
      final data = (args as Map).cast<String, dynamic>();
      final url = data['url'] as String;
      final headers = (data['headers'] as Map).cast<String, dynamic>();
      final rid = data['rid'] as String;
      _dio
          .get<String>(url, options: Options(headers: headers, responseType: ResponseType.plain))
          .then((res) {
        final payload = jsonEncode(res.data ?? '');
        _js.evaluate("__pending['$rid'] && __pending['$rid']($payload); delete __pending['$rid'];");
      }).catchError((err) {
        final payload = jsonEncode('');
        _js.evaluate("__pending['$rid'] && __pending['$rid']($payload); delete __pending['$rid'];");
      });
      return null;
    });

    final code = await _dio.get<String>(
      manifest.scriptUrl,
      options: Options(responseType: ResponseType.plain),
    );
    final setup = _js.evaluate('''
      const __pending = {};
      let __id = 0;
      globalThis.httpGet = (url, headers = {}) => new Promise((resolve) => {
        const rid = String(__id++);
        __pending[rid] = resolve;
        sendMessage('__httpGetStart', JSON.stringify({url: url, headers: headers, rid: rid}));
      });
      var module = {};
      ${code.data}
    ''');
    if (setup.isError) {
      throw Exception('Source script failed to load: ${setup.stringResult}');
    }
    _ready = true;
  }

  Future<dynamic> _call(String fn, List<dynamic> args) async {
    await _ensureReady();
    final argsJs = args.map(jsonEncode).join(',');
    final callId = 'call${DateTime.now().microsecondsSinceEpoch}';

    // Wrapped in try/catch + Promise.resolve so a missing/optional
    // function (e.g. a source with no genres() support) fails fast with
    // a real error instead of hanging until the poll loop times out.
    _js.evaluate('''
      (function() {
        try {
          Promise.resolve(module.$fn($argsJs)).then(function(r) {
            globalThis['${callId}_result'] = JSON.stringify(r);
          }).catch(function(e) {
            globalThis['${callId}_error'] = String(e);
          });
        } catch (e) {
          globalThis['${callId}_error'] = String(e);
        }
      })();
    ''');

    String status = 'pending';
    for (int i = 0; i < 500; i++) {
      _js.executePendingJob();
      final check = _js.evaluate(
        "typeof globalThis['${callId}_result'] !== 'undefined' ? 'result' : (typeof globalThis['${callId}_error'] !== 'undefined' ? 'error' : 'pending')",
      );
      status = check.stringResult;
      if (status != 'pending') break;
      await Future.delayed(const Duration(milliseconds: 20));
    }

    if (status == 'pending') {
      throw Exception('Source call "$fn" timed out waiting for a response.');
    }
    if (status == 'error') {
      final err = _js.evaluate("globalThis['${callId}_error']");
      _js.evaluate("delete globalThis['${callId}_error'];");
      throw Exception('Source call "$fn" failed: ${err.stringResult}');
    }

    final res = _js.evaluate("globalThis['${callId}_result']");
    _js.evaluate("delete globalThis['${callId}_result'];");
    return jsonDecode(res.stringResult);
  }

  @override
  Future<List<Entry>> search(String query, {int page = 1, String? genre}) async {
    final list = await _call('search', [query, page, genre]) as List;
    return list.map((e) => Entry.fromJson(e, id, type)).toList();
  }

  @override
  Future<List<Entry>> popular({int page = 1, String? genre}) async {
    final list = await _call('popular', [page, genre]) as List;
    return list.map((e) => Entry.fromJson(e, id, type)).toList();
  }

  @override
  Future<List<Entry>> latest({int page = 1, String? genre}) async {
    final list = await _call('latest', [page, genre]) as List;
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

  @override
  Future<List<Map<String, String>>> getGenres() async {
    try {
      final list = await _call('genres', []) as List;
      return list.map((g) => {'id': g['id'].toString(), 'name': g['name'].toString()}).toList();
    } catch (_) {
      return [];
    }
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
