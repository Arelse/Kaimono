import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';
import '../models/entry.dart';
import '../models/source.dart';
import '../services/extension_manager.dart';

/// Video playback for anime episodes. Fetches [StreamLink]s from the
/// source (URL + quality label + any required headers, e.g. referer for
/// sites that gate direct video access) and hands the chosen link to
/// video_player/chewie.
class PlayerScreen extends ConsumerStatefulWidget {
  final String sourceId;
  final EntryChunk chunk;
  final List<EntryChunk> allChunks;

  const PlayerScreen({super.key, required this.sourceId, required this.chunk, required this.allChunks});

  @override
  ConsumerState<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends ConsumerState<PlayerScreen> {
  late EntryChunk _chunk;
  List<StreamLink> _streams = [];
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _chunk = widget.chunk;
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final streams = await source.getStreamLinks(_chunk.id);
    if (streams.isEmpty) {
      setState(() {
        _loading = false;
        _error = 'This source returned no playable stream for this episode.';
      });
      return;
    }
    await _play(streams.first);
    setState(() {
      _streams = streams;
      _loading = false;
    });
  }

  Future<void> _play(StreamLink link) async {
    await _chewieController?.dispose();
    await _videoController?.dispose();
    final controller = VideoPlayerController.networkUrl(
      Uri.parse(link.url),
      httpHeaders: link.headers,
    );
    await controller.initialize();
    _videoController = controller;
    _chewieController = ChewieController(
      videoPlayerController: controller,
      autoPlay: true,
      looping: false,
    );
    setState(() {});
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
        backgroundColor: Colors.black,
        title: Text(_chunk.title, style: const TextStyle(fontSize: 14)),
        actions: [
          if (_streams.length > 1)
            PopupMenuButton<StreamLink>(
              icon: const Icon(Icons.hd_outlined),
              onSelected: _play,
              itemBuilder: (_) => _streams
                  .map((s) => PopupMenuItem(value: s, child: Text(s.quality)))
                  .toList(),
            ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: Center(
              child: _loading
                  ? const CircularProgressIndicator()
                  : _error != null
                      ? Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(_error!, style: const TextStyle(color: Colors.white70), textAlign: TextAlign.center),
                        )
                      : _chewieController != null
                          ? AspectRatio(
                              aspectRatio: _videoController!.value.aspectRatio,
                              child: Chewie(controller: _chewieController!),
                            )
                          : const SizedBox(),
            ),
          ),
          SafeArea(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                TextButton.icon(
                  onPressed: () => _goToChunk(-1),
                  icon: const Icon(Icons.skip_previous, color: Colors.white),
                  label: const Text('Prev', style: TextStyle(color: Colors.white)),
                ),
                TextButton.icon(
                  onPressed: () => _goToChunk(1),
                  icon: const Icon(Icons.skip_next, color: Colors.white),
                  label: const Text('Next', style: TextStyle(color: Colors.white)),
                  iconAlignment: IconAlignment.end,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _chewieController?.dispose();
    _videoController?.dispose();
    super.dispose();
  }
}
