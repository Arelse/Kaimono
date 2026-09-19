import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/entry.dart';
import '../services/extension_manager.dart';

/// Text reader for novel chunks. Per the [Source] contract, `getPages()`
/// on a novel source returns ordered text blocks (paragraphs/sections)
/// rather than image URLs — same method, different payload shape,
/// which is why novel sources don't need a separate interface method.
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

  @override
  void initState() {
    super.initState();
    _chunk = widget.chunk;
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final paragraphs = await source.getPages(_chunk.id);
    setState(() {
      _paragraphs = paragraphs;
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
      appBar: AppBar(
        title: Text(_chunk.title),
        actions: [
          IconButton(icon: const Icon(Icons.text_decrease), onPressed: () => setState(() => _fontSize = (_fontSize - 1).clamp(12, 28))),
          IconButton(icon: const Icon(Icons.text_increase), onPressed: () => setState(() => _fontSize = (_fontSize + 1).clamp(12, 28))),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
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
}
