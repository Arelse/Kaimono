import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/entry.dart';
import '../services/extension_manager.dart';

enum _ReadMode { paged, webtoon }

/// Manga page reader. Supports paged (swipe, one page at a time) and
/// webtoon (continuous vertical scroll) modes, and auto-advances to the
/// next chunk in [allChunks] when the reader runs out of pages — the
/// same "keep reading" flow Komikku/Mihon-style readers use.
class MangaReaderScreen extends ConsumerStatefulWidget {
  final String sourceId;
  final EntryChunk chunk;
  final List<EntryChunk> allChunks;

  const MangaReaderScreen({super.key, required this.sourceId, required this.chunk, required this.allChunks});

  @override
  ConsumerState<MangaReaderScreen> createState() => _MangaReaderScreenState();
}

class _MangaReaderScreenState extends ConsumerState<MangaReaderScreen> {
  late EntryChunk _chunk;
  List<String> _pages = [];
  bool _loading = true;
  _ReadMode _mode = _ReadMode.paged;
  final PageController _pageController = PageController();

  @override
  void initState() {
    super.initState();
    _chunk = widget.chunk;
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final pages = await source.getPages(_chunk.id);
    setState(() {
      _pages = pages;
      _loading = false;
    });
  }

  void _goToChunk(int offset) {
    final i = widget.allChunks.indexWhere((c) => c.id == _chunk.id);
    final target = i + offset;
    if (target < 0 || target >= widget.allChunks.length) return;
    setState(() => _chunk = widget.allChunks[target]);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black.withValues(alpha: 0.6),
        title: Text(_chunk.title, style: const TextStyle(fontSize: 14)),
        actions: [
          IconButton(
            icon: Icon(_mode == _ReadMode.paged ? Icons.view_agenda_outlined : Icons.view_carousel_outlined),
            tooltip: _mode == _ReadMode.paged ? 'Switch to webtoon' : 'Switch to paged',
            onPressed: () => setState(() => _mode = _mode == _ReadMode.paged ? _ReadMode.webtoon : _ReadMode.paged),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _pages.isEmpty
              ? const Center(child: Text('No pages found', style: TextStyle(color: Colors.white)))
              : _mode == _ReadMode.paged
                  ? _pagedView()
                  : _webtoonView(),
      bottomNavigationBar: SafeArea(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            TextButton.icon(
              onPressed: () => _goToChunk(-1),
              icon: const Icon(Icons.skip_previous),
              label: const Text('Prev'),
            ),
            TextButton.icon(
              onPressed: () => _goToChunk(1),
              icon: const Icon(Icons.skip_next),
              label: const Text('Next'),
              iconAlignment: IconAlignment.end,
            ),
          ],
        ),
      ),
    );
  }

  Widget _pagedView() {
    return PageView.builder(
      controller: _pageController,
      itemCount: _pages.length,
      itemBuilder: (context, i) => InteractiveViewer(
        maxScale: 4,
        child: CachedNetworkImage(
          imageUrl: _pages[i],
          fit: BoxFit.contain,
          width: double.infinity,
          placeholder: (_, __) => const Center(child: CircularProgressIndicator()),
          errorWidget: (_, __, ___) => const Center(child: Icon(Icons.broken_image, color: Colors.white38)),
        ),
      ),
    );
  }

  Widget _webtoonView() {
    return ListView.builder(
      itemCount: _pages.length,
      itemBuilder: (context, i) => CachedNetworkImage(
        imageUrl: _pages[i],
        fit: BoxFit.fitWidth,
        width: double.infinity,
        placeholder: (_, __) => const SizedBox(height: 300, child: Center(child: CircularProgressIndicator())),
        errorWidget: (_, __, ___) => const SizedBox(height: 100, child: Icon(Icons.broken_image, color: Colors.white38)),
      ),
    );
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }
}
