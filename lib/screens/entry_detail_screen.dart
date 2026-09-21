import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../services/extension_manager.dart';
import '../services/library_manager.dart';
import 'manga_reader_screen.dart';
import 'novel_reader_screen.dart';
import 'player_screen.dart';

/// Shows an entry's synopsis/metadata plus its chunk list (chapters or
/// episodes), and routes each chunk to the right consumption screen —
/// paged image reader, text reader, or video player — based on
/// [Entry.type]. This is the single fan-out point that keeps the reader
/// and player screens from needing to know about each other.
class EntryDetailScreen extends ConsumerStatefulWidget {
  final String sourceId;
  final Entry entry;
  const EntryDetailScreen({super.key, required this.sourceId, required this.entry});

  @override
  ConsumerState<EntryDetailScreen> createState() => _EntryDetailScreenState();
}

class _EntryDetailScreenState extends ConsumerState<EntryDetailScreen> {
  Entry? _details;
  List<EntryChunk> _chunks = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final details = await source.getEntryDetails(widget.entry.id);
    final chunks = await source.getChunks(widget.entry.id);
    setState(() {
      _details = details;
      _chunks = chunks;
      _loading = false;
    });
  }

  void _openChunk(EntryChunk chunk) {
    final type = widget.entry.type;
    if (type == ContentType.manga) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => MangaReaderScreen(sourceId: widget.sourceId, chunk: chunk, allChunks: _chunks),
        ),
      );
    } else if (type == ContentType.novel) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => NovelReaderScreen(sourceId: widget.sourceId, chunk: chunk, allChunks: _chunks),
        ),
      );
    } else {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => PlayerScreen(sourceId: widget.sourceId, chunk: chunk, allChunks: _chunks),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    final e = _details!;
    final chunkWord = e.type == ContentType.anime ? 'Episode' : 'Chapter';

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 220,
            pinned: true,
            flexibleSpace: FlexibleSpaceBar(
              background: e.coverUrl != null
                  ? Stack(
                      fit: StackFit.expand,
                      children: [
                        CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover),
                        Container(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [Colors.transparent, Theme.of(context).colorScheme.surface],
                            ),
                          ),
                        ),
                      ],
                    )
                  : Container(color: Theme.of(context).colorScheme.surfaceContainerHighest),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(e.title, style: Theme.of(context).textTheme.headlineSmall),
                  if (e.author != null) Text(e.author!, style: Theme.of(context).textTheme.bodyMedium),
                  const SizedBox(height: 16),
                  Consumer(
                    builder: (context, ref, _) {
                      final library = ref.watch(libraryManagerProvider.notifier);
                      ref.watch(libraryManagerProvider);
                      final fav = library.isFavorite(widget.sourceId, widget.entry.id);
                      return Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _actionButton(
                            icon: fav ? Icons.favorite : Icons.favorite_border,
                            label: fav ? 'In library' : 'Add to library',
                            active: fav,
                            onTap: () => library.toggle(e),
                          ),
                          _actionButton(
                            icon: Icons.public,
                            label: 'Source',
                            active: false,
                            onTap: () {},
                          ),
                        ],
                      );
                    },
                  ),
                  const Divider(height: 32),
                  Wrap(
                    spacing: 6,
                    children: e.genres.map((g) => Chip(label: Text(g), visualDensity: VisualDensity.compact)).toList(),
                  ),
                  const SizedBox(height: 12),
                  if (e.description != null) Text(e.description!),
                  const SizedBox(height: 20),
                  Text('${_chunks.length} $chunkWord${_chunks.length == 1 ? '' : 's'}',
                      style: Theme.of(context).textTheme.titleMedium),
                ],
              ),
            ),
          ),
          SliverList(
            delegate: SliverChildBuilderDelegate(
              (context, i) {
                final c = _chunks[i];
                return ListTile(
                  leading: c.read ? const Icon(Icons.check_circle, size: 18) : null,
                  title: Text(c.title),
                  subtitle: c.uploadDate != null ? Text(c.uploadDate.toString().split(' ').first) : null,
                  onTap: () => _openChunk(c),
                );
              },
              childCount: _chunks.length,
            ),
          ),
        ],
      ),
    );
  }

  Widget _actionButton({required IconData icon, required String label, required bool active, required VoidCallback onTap}) {
    final color = active ? Theme.of(context).colorScheme.primary : null;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Column(
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(fontSize: 12, color: color)),
          ],
        ),
      ),
    );
  }
}
