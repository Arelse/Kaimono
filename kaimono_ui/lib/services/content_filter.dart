import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Whether 18+ sources are visible. On by default.
class ShowNsfwNotifier extends StateNotifier<bool> {
  ShowNsfwNotifier() : super(true) {
    _restore();
  }

  static const _key = 'show_nsfw_sources';

  Future<void> _restore() async {
    final prefs = await SharedPreferences.getInstance();
    state = prefs.getBool(_key) ?? true;
  }

  Future<void> set(bool value) async {
    state = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_key, value);
  }
}

final showNsfwProvider = StateNotifierProvider<ShowNsfwNotifier, bool>((ref) => ShowNsfwNotifier());

/// Ids of installed sources the user has marked as 18+.
class NsfwSourcesNotifier extends StateNotifier<Set<String>> {
  NsfwSourcesNotifier() : super(<String>{}) {
    _restore();
  }

  static const _key = 'nsfw_source_ids';

  Future<void> _restore() async {
    final prefs = await SharedPreferences.getInstance();
    state = (prefs.getStringList(_key) ?? const <String>[]).toSet();
  }

  Future<void> setMarked(String sourceId, bool marked) async {
    final next = {...state};
    if (marked) {
      next.add(sourceId);
    } else {
      next.remove(sourceId);
    }
    state = next;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(_key, next.toList());
  }
}

final nsfwSourcesProvider =
    StateNotifierProvider<NsfwSourcesNotifier, Set<String>>((ref) => NsfwSourcesNotifier());
