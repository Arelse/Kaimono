import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/source.dart';
import 'js_source.dart';

class ExtensionRepo {
  final String url;
  ExtensionRepo(this.url);

  Future<List<ExtensionManifest>> fetchIndex() async {
    final res = await Dio().get<String>(
      url,
      options: Options(responseType: ResponseType.plain),
    );
    final list = jsonDecode(res.data!) as List;
    return list.map((e) => ExtensionManifest.fromJson(e)).toList();
  }
}

class ExtensionManager extends StateNotifier<Map<String, Source>> {
  ExtensionManager() : super({}) {
    _loadInstalled();
  }

  static const _installedKey = 'installed_sources';

  final List<ExtensionRepo> repos = [
    ExtensionRepo('https://raw.githubusercontent.com/Arelse/Kaimono/main/assets/sample_repo/index.json'),
  ];

  Future<List<ExtensionManifest>> browseAll() async {
    final results = <ExtensionManifest>[];
    for (final repo in repos) {
      try {
        results.addAll(await repo.fetchIndex());
      } catch (_) {
        // Repo unreachable/offline — skip, don't crash the browse screen.
      }
    }
    return results;
  }

  void addRepo(String url) {
    repos.add(ExtensionRepo(url));
  }

  void removeRepo(String url) {
    repos.removeWhere((r) => r.url == url);
  }

  Future<void> _loadInstalled() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList(_installedKey);
    if (raw == null) return;
    final map = <String, Source>{};
    for (final item in raw) {
      final manifest = ExtensionManifest.fromJson(jsonDecode(item));
      map[manifest.id] = JsSource(manifest);
    }
    state = map;
  }

  Future<void> _persistInstalled() async {
    final prefs = await SharedPreferences.getInstance();
    final list = state.values.map((s) {
      final m = (s as JsSource).manifest;
      return jsonEncode({
        'id': m.id,
        'name': m.name,
        'lang': m.lang,
        'type': m.type.name,
        'icon': m.iconUrl,
        'script': m.scriptUrl,
        'version': m.version,
      });
    }).toList();
    await prefs.setStringList(_installedKey, list);
  }

  Future<void> install(ExtensionManifest manifest) async {
    final source = JsSource(manifest);
    state = {...state, manifest.id: source};
    await _persistInstalled();
  }

  Future<void> uninstall(String sourceId) async {
    final next = {...state}..remove(sourceId);
    state = next;
    await _persistInstalled();
  }
}

final extensionManagerProvider =
    StateNotifierProvider<ExtensionManager, Map<String, Source>>((ref) => ExtensionManager());
