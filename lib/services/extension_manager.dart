import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/source.dart';
import 'js_source.dart';

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

  // Default repo: this project's own sample repo, served straight off
  // GitHub's raw file host — no separate server needed. Manage repos
  // (add/remove more) from the Settings tab.
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

  Future<void> install(ExtensionManifest manifest) async {
    final source = JsSource(manifest);
    state = {...state, manifest.id: source};
  }

  void uninstall(String sourceId) {
    final next = {...state}..remove(sourceId);
    state = next;
  }
}

final extensionManagerProvider =
    StateNotifierProvider<ExtensionManager, Map<String, Source>>((ref) => ExtensionManager());
