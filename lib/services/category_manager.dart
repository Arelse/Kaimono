import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/category.dart';
import '../models/entry.dart';

class CategoryState {
  final List<Category> categories;
  final Map<String, List<String>> assignments; // "sourceId::entryId" -> [categoryId, ...]

  CategoryState({required this.categories, required this.assignments});

  CategoryState copyWith({List<Category>? categories, Map<String, List<String>>? assignments}) =>
      CategoryState(categories: categories ?? this.categories, assignments: assignments ?? this.assignments);
}

/// Custom groups for organizing library entries (e.g. "Manhwa", "Action").
/// Stored separately from [LibraryManager] so this feature can't
/// accidentally corrupt the existing favorites storage format.
class CategoryManager extends StateNotifier<CategoryState> {
  CategoryManager() : super(CategoryState(categories: [], assignments: {})) {
    _load();
  }

  static const _key = 'kaimono_categories_v1';

  String _keyFor(String sourceId, String entryId) => '$sourceId::$entryId';

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_key);
    if (raw == null) return;
    final json = jsonDecode(raw) as Map<String, dynamic>;
    final categories = (json['categories'] as List).map((c) => Category.fromJson(c)).toList();
    final assignments = (json['assignments'] as Map<String, dynamic>)
        .map((k, v) => MapEntry(k, (v as List).cast<String>()));
    state = CategoryState(categories: categories, assignments: assignments);
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    final json = {
      'categories': state.categories.map((c) => c.toJson()).toList(),
      'assignments': state.assignments,
    };
    await prefs.setString(_key, jsonEncode(json));
  }

  Future<void> addCategory(String name) async {
    final cat = Category(id: DateTime.now().microsecondsSinceEpoch.toString(), name: name);
    state = state.copyWith(categories: [...state.categories, cat]);
    await _persist();
  }

  Future<void> renameCategory(String id, String newName) async {
    final updated = state.categories.map((c) => c.id == id ? Category(id: c.id, name: newName) : c).toList();
    state = state.copyWith(categories: updated);
    await _persist();
  }

  Future<void> deleteCategory(String id) async {
    final updated = state.categories.where((c) => c.id != id).toList();
    final newAssignments = state.assignments.map((k, v) => MapEntry(k, v.where((c) => c != id).toList()));
    state = CategoryState(categories: updated, assignments: newAssignments);
    await _persist();
  }

  Future<void> reorder(int oldIndex, int newIndex) async {
    final list = [...state.categories];
    if (newIndex > oldIndex) newIndex--;
    final item = list.removeAt(oldIndex);
    list.insert(newIndex, item);
    state = state.copyWith(categories: list);
    await _persist();
  }

  List<String> getAssignments(String sourceId, String entryId) =>
      state.assignments[_keyFor(sourceId, entryId)] ?? [];

  Future<void> setAssignments(String sourceId, String entryId, List<String> categoryIds) async {
    final next = {...state.assignments};
    next[_keyFor(sourceId, entryId)] = categoryIds;
    state = state.copyWith(assignments: next);
    await _persist();
  }

  List<Entry> entriesForCategory(String categoryId, Map<String, Entry> libraryEntries) {
    return libraryEntries.entries
        .where((e) => (state.assignments[e.key] ?? []).contains(categoryId))
        .map((e) => e.value)
        .toList();
  }
}

final categoryManagerProvider = StateNotifierProvider<CategoryManager, CategoryState>((ref) => CategoryManager());
