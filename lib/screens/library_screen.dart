import 'package:flutter/material.dart';
import '../models/content_type.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

/// Shows saved (favorited) entries, split by content type. This is the
/// screen that differentiates the app from a single-medium reader like
/// Komikku: the same library shell hosts manga, anime, and novels side
/// by side instead of three separate apps.
class LibraryScreen extends StatelessWidget {
  const LibraryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: ContentType.values.length,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Library'),
          bottom: TabBar(
            tabs: ContentType.values.map((t) => Tab(text: t.label)).toList(),
          ),
          actions: [
            IconButton(icon: const Icon(Icons.search), onPressed: () {}),
          ],
        ),
        body: TabBarView(
          children: ContentType.values.map((type) {
            // TODO: wire to Isar-backed library provider filtered by `type`.
            return EntryGrid(
              entries: const [],
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
    );
  }
}
