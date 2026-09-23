import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/entry.dart';
import '../services/extension_manager.dart';

class NovelReaderScreen extends ConsumerStatefulWidget {
  final String sourceId;
  final EntryChunk chunk;
  final List<EntryChunk> allChunks;

  const NovelReaderScreen({super.key, required this.sourceId, required this.chunk, required this.allChunks});

  @override
  ConsumerState<NovelReaderScreen> createState() => _NovelReaderScreenState();
}

class _NovelReaderScreenState extends ConsumerState<NovelReaderScreen> {
  late EntryChunk _chunk;
  List<String> _paragraphs = [];
  bool _loading = true;
  double _fontSize = 16;
  double _progress = 0;
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _chunk = widget.chunk;
    _scrollController.addListener(_onScroll);
    _load();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final max = _scrollController.position.maxScrollExtent;
    if (max <= 0) {
      setState(() => _progress = 1);
      return;
    }
    setState(() => _progress = (_scrollController.offset / max).clamp(0.0, 1.0));
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _progress = 0;
    });
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final paragraphs = await source.getPages(_chunk.id);
    setState(() {
      _paragraphs = paragraphs;
      _loading = false;
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) _scrollController.jumpTo(0);
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
      appBar: AppBar(
        title: Text(_chunk.title),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(3),
          child: LinearProgressIndicator(value: _progress, minHeight: 3),
        ),
        actions: [
          IconButton(icon: const Icon(Icons.text_decrease), onPressed: () => setState(() => _fontSize = (_fontSize - 1).clamp(12, 28))),
          IconButton(icon: const Icon(Icons.text_increase), onPressed: () => setState(() => _fontSize = (_fontSize + 1).clamp(12, 28))),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              controller: _scrollController,
              padding: const EdgeInsets.all(20),
              children: [
                for (final p in _paragraphs) ...[
                  Text(p, style: TextStyle(fontSize: _fontSize, height: 1.6)),
                  const SizedBox(height: 14),
                ],
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    OutlinedButton.icon(onPressed: () => _goToChunk(-1), icon: const Icon(Icons.skip_previous), label: const Text('Prev')),
                    OutlinedButton.icon(onPressed: () => _goToChunk(1), icon: const Icon(Icons.skip_next), label: const Text('Next')),
                  ],
                ),
              ],
            ),
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }
}
