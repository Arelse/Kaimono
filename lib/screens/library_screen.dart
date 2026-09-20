import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/library_manager.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

/// Shows favorited entries, split by content type, backed by persistent
/// storage via [libraryManagerProvider] — survives app restarts.
class LibraryScreen extends ConsumerWidget {
  const LibraryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final library = ref.watch(libraryManagerProvider.notifier);
    ref.watch(libraryManagerProvider); // rebuild on changes

    return DefaultTabController(
      length: ContentType.values.length,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Library'),
          bottom: TabBar(
            tabs: ContentType.values.map((t) => Tab(text: t.label)).toList(),
          ),
        ),
        body: TabBarView(
          children: ContentType.values.map((type) {
            final entries = library.byType(type);
            return EntryGrid(
              entries: entries,
              emptyLabel: 'No ${type.label.toLowerCase()} yet\nFavorite something from Discover',
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
    );
  }
}
