import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/category_manager.dart';
import '../services/library_manager.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

class LibraryScreen extends ConsumerStatefulWidget {
  const LibraryScreen({super.key});
  @override
  ConsumerState<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends ConsumerState<LibraryScreen> {
  String? _selectedCategoryId;

  @override
  Widget build(BuildContext context) {
    final library = ref.watch(libraryManagerProvider.notifier);
    final libraryState = ref.watch(libraryManagerProvider);
    final categoryState = ref.watch(categoryManagerProvider);
    final categoryManager = ref.watch(categoryManagerProvider.notifier);

    return DefaultTabController(
      length: ContentType.values.length,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Library'),
          bottom: TabBar(
            tabs: ContentType.values.map((t) => Tab(text: t.label)).toList(),
          ),
        ),
        body: Column(
          children: [
            if (categoryState.categories.isNotEmpty)
              SizedBox(
                height: 44,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: ChoiceChip(
                        label: const Text('All'),
                        selected: _selectedCategoryId == null,
                        onSelected: (_) => setState(() => _selectedCategoryId = null),
                      ),
                    ),
                    ...categoryState.categories.map((c) => Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: ChoiceChip(
                            label: Text(c.name),
                            selected: _selectedCategoryId == c.id,
                            onSelected: (_) => setState(() => _selectedCategoryId = c.id),
                          ),
                        )),
                  ],
                ),
              ),
            Expanded(
              child: TabBarView(
                children: ContentType.values.map((type) {
                  var entries = library.byType(type);
                  if (_selectedCategoryId != null) {
                    final inCategory = categoryManager
                        .entriesForCategory(_selectedCategoryId!, libraryState)
                        .map((e) => e.id)
                        .toSet();
                    entries = entries.where((e) => inCategory.contains(e.id)).toList();
                  }
                  return EntryGrid(
                    entries: entries,
                    emptyLabel: 'No ${type.label.toLowerCase()} yet',
                    onTap: (entry) => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => EntryDetailScreen(sourceId: entry.sourceId, entry: entry),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
