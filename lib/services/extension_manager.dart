import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/source.dart';
import 'js_source.dart';

/// A repo is just a URL hosting an `index.json` listing available
/// extensions (id, name, script url, icon, version) — the same shape
/// Mihon/Mangayomi/Keiyoshi repos use for their index files, so pointing
/// this at a *compatible* community repo (one already publishing JS
/// sources in the shape [JsSource] expects) works without extra glue.
/// Repos that only publish compiled .apk sources are not installable here.
class ExtensionRepo {
  final String url;
  ExtensionRepo(this.url);

  Future<List<ExtensionManifest>> fetchIndex() async {
    final res = await Dio().get<String>(url);
    final list = jsonDecode(res.data!) as List;
    return list.map((e) => ExtensionManifest.fromJson(e)).toList();
  }
}

class ExtensionManager extends StateNotifier<Map<String, Source>> {
  ExtensionManager() : super({});

  final List<ExtensionRepo> repos = [
    // Placeholder — point this at your own JSON index. See
    // assets/sample_repo/index.json in this project for the expected shape.
    ExtensionRepo('https://example.com/kaimono-extensions/index.json'),
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

  Future<void> install(ExtensionManifest manifest) async {
    final source = JsSource(manifest);
    state = {...state, manifest.id: source};
    // TODO: persist manifest to Isar so installed extensions survive restart.
  }

  void uninstall(String sourceId) {
    final next = {...state}..remove(sourceId);
    state = next;
  }
}

final extensionManagerProvider =
    StateNotifierProvider<ExtensionManager, Map<String, Source>>((ref) => ExtensionManager());
