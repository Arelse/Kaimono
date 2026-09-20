import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/content_type.dart';
import '../models/entry.dart';

/// Persists favorited entries to on-device storage (shared_preferences)
/// so the Library survives app restarts. Deliberately not using a
/// database package here — a flat JSON blob is plenty for a favorites
/// list and avoids repeating the Gradle/native-plugin version conflicts
/// isar caused earlier.
class LibraryManager extends StateNotifier<Map<String, Entry>> {
  LibraryManager() : super({}) {
    _load();
  }

  static const _key = 'library_entries';

  String _keyFor(String sourceId, String entryId) => '$sourceId::$entryId';

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw == null) return;
    final list = jsonDecode(raw) as List;
    final map = <String, Entry>{};
    for (final item in list) {
      final entry = _fromStorage(item as Map<String, dynamic>);
      map[_keyFor(entry.sourceId, entry.id)] = entry;
    }
    state = map;
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    final list = state.values.map(_toStorage).toList();
    await prefs.setString(_key, jsonEncode(list));
  }

  bool isFavorite(String sourceId, String entryId) => state.containsKey(_keyFor(sourceId, entryId));

  Future<void> toggle(Entry entry) async {
    final key = _keyFor(entry.sourceId, entry.id);
    final next = {...state};
    if (next.containsKey(key)) {
      next.remove(key);
    } else {
      next[key] = entry;
    }
    state = next;
    await _persist();
  }

  List<Entry> byType(ContentType type) => state.values.where((e) => e.type == type).toList();

  Map<String, dynamic> _toStorage(Entry e) => {
        'id': e.id,
        'sourceId': e.sourceId,
        'type': e.type.name,
        'title': e.title,
        'cover': e.coverUrl,
        'description': e.description,
        'genres': e.genres,
        'author': e.author,
        'status': e.status.name,
      };

  Entry _fromStorage(Map<String, dynamic> j) => Entry(
        id: j['id'],
        sourceId: j['sourceId'],
        type: ContentType.values.firstWhere((t) => t.name == j['type']),
        title: j['title'],
        coverUrl: j['cover'],
        description: j['description'],
        genres: (j['genres'] as List?)?.cast<String>() ?? const [],
        author: j['author'],
        status: EntryStatus.values.firstWhere((s) => s.name == j['status'], orElse: () => EntryStatus.unknown),
      );
}

final libraryManagerProvider =
    StateNotifierProvider<LibraryManager, Map<String, Entry>>((ref) => LibraryManager());
