import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/entry.dart';
import '../models/source.dart';
import '../models/content_type.dart';
import '../services/library_manager.dart';
import 'manga_reader_screen.dart';
import 'novel_reader_screen.dart';
import 'player_screen.dart';
import 'webview_screen.dart';

class EntryDetailScreen extends ConsumerStatefulWidget {
  final Entry entry;
  final Source source;

  const EntryDetailScreen({super.key, required this.entry, required this.source});

  @override
  ConsumerState<EntryDetailScreen> createState() => _EntryDetailScreenState();
}

class _EntryDetailScreenState extends ConsumerState<EntryDetailScreen> {
  bool _isLoading = true;
  List<EntryChunk> _chunks = [];
  String? _error;
  bool _isExpandedSynopsis = false;
  String _searchQuery = '';
  bool _sortAscending = false;

  @override
  void initState() {
    super.initState();
    _loadDetails();
  }

  Future<void> _loadDetails() async {
    try {
      setState(() {
        _isLoading = true;
        _error = null;
      });
      final chunks = await widget.source.getChunks(widget.entry);
      if (mounted) {
        setState(() {
          _chunks = chunks;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final libraryState = ref.watch(libraryManagerProvider);
    final isFavorite = libraryState.any((e) => e.url == widget.entry.url);

    final filteredChunks = _chunks.where((c) {
      if (_searchQuery.isEmpty) return true;
      return c.title.toLowerCase().contains(_searchQuery.toLowerCase());
    }).toList();

    if (_sortAscending) {
      filteredChunks.sort((a, b) => a.number.compareTo(b.number));
    } else {
      filteredChunks.sort((a, b) => b.number.compareTo(a.number));
    }

    final totalCount = _chunks.length;

    return Scaffold(
      backgroundColor: const Color(0xFF120406),
      body: SafeArea(
        child: Stack(
          children: [
            CustomScrollView(
              slivers: [
                SliverAppBar(
                  pinned: true,
                  backgroundColor: const Color(0xFF120406).withOpacity(0.85),
                  elevation: 0,
                  automaticallyImplyLeading: false,
                  title: Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.arrow_back, color: Colors.white, size: 22),
                        onPressed: () => Navigator.pop(context),
                      ),
                      Expanded(
                        child: Text(
                          widget.entry.title,
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            letterSpacing: -0.3,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  actions: [
                    IconButton(
                      icon: const Icon(Icons.cloud_download_outlined, color: Colors.grey, size: 20),
                      onPressed: () {},
                    ),
                    IconButton(
                      icon: const Icon(Icons.filter_list, color: Colors.grey, size: 20),
                      onPressed: () => setState(() => _sortAscending = !_sortAscending),
                    ),
                    IconButton(
                      icon: const Icon(Icons.more_horiz, color: Colors.grey, size: 20),
                      onPressed: () {},
                    ),
                  ],
                ),
                SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 115,
                              height: 160,
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(12),
                                color: Colors.grey[900],
                                border: Border.all(color: Colors.white.withOpacity(0.1)),
                              ),
                              clipBehavior: Clip.antiAlias,
                              child: widget.entry.coverUrl != null
                                  ? CachedNetworkImage(
                                      imageUrl: widget.entry.coverUrl!,
                                      fit: BoxFit.cover,
                                      placeholder: (context, url) => Container(color: Colors.grey[900]),
                                      errorWidget: (context, url, error) => const Icon(Icons.broken_image, color: Colors.grey),
                                    )
                                  : const Icon(Icons.book, color: Colors.grey),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    widget.entry.title,
                                    style: const TextStyle(
                                      fontSize: 22,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.white,
                                      height: 1.2,
                                      letterSpacing: -0.5,
                                    ),
                                  ),
                                  const SizedBox(height: 12),
                                  if (widget.entry.author != null)
                                    _buildMetaRow(Icons.person_outline, widget.entry.author!),
                                  const SizedBox(height: 6),
                                  if (widget.entry.artist != null)
                                    _buildMetaRow(Icons.edit_outlined, widget.entry.artist!),
                                  const SizedBox(height: 6),
                                  _buildMetaRow(
                                    Icons.access_time,
                                    "${widget.entry.status ?? 'Ongoing'} • ${widget.source.name}",
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                                              Container(
                        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        decoration: BoxDecoration(
                          color: const Color(0xFF1a1d24),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: [
                            _buildStatItem("RANK", "#21"),
                            Container(width: 1, height: 40, color: Colors.white12),
                            _buildStatItem("RATING", "9.81", isRating: true),
                            Container(width: 1, height: 40, color: Colors.white12),
                            _buildStatItem("SAVES", "76.4K"),
                          ],
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            _buildActionButton(
                              isFavorite ? Icons.bookmark : Icons.bookmark_border,
                              isFavorite ? "In Library" : "Library",
                              isFavorite ? const Color(0xFF8B7FF9) : const Color(0xFFa19d9e),
                              isFavorite ? const Color(0xFF232135) : const Color(0xFF201c1e),
                              () => ref.read(libraryManagerProvider.notifier).toggleFavorite(widget.entry),
                            ),
                            _buildActionButton(Icons.calendar_today_outlined, "Soon", const Color(0xFFa19d9e), const Color(0xFF201c1e), () {}),
                            _buildActionButton(Icons.check_circle_outline, "Trackers", const Color(0xFFff7b7b), const Color(0xFF2c1c21), () {}),
                            if (widget.entry.sourceUrl != null)
                              _buildActionButton(Icons.explore_outlined, "WebView", const Color(0xFFa19d9e), const Color(0xFF201c1e), () {
                                Navigator.push(context, MaterialPageRoute(builder: (_) => WebViewScreen(url: widget.entry.sourceUrl!, title: widget.entry.title)));
                              }),
                            _buildActionButton(Icons.merge_type, "Merge", const Color(0xFFa19d9e), const Color(0xFF201c1e), () {}),
                          ],
                        ),
                      ),
                      if (widget.entry.description != null && widget.entry.description!.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                widget.entry.description!,
                                maxLines: _isExpandedSynopsis ? null : 2,
                                overflow: _isExpandedSynopsis ? TextOverflow.visible : TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 14, color: Color(0xFF9ca3af), height: 1.5),
                              ),
                              GestureDetector(
                                onTap: () => setState(() => _isExpandedSynopsis = !_isExpandedSynopsis),
                                child: Container(
                                  width: double.infinity,
                                  alignment: Alignment.center,
                                  padding: const EdgeInsets.only(top: 8),
                                  child: Icon(_isExpandedSynopsis ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, color: Colors.grey),
                                ),
                              ),
                            ],
                          ),
                        ),
                      if (widget.entry.genres != null && widget.entry.genres!.isNotEmpty)
                        SizedBox(
                          height: 40,
                          child: ListView.separated(
                            scrollDirection: Axis.horizontal,
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                            itemCount: widget.entry.genres!.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 8),
                            itemBuilder: (context, index) {
                              return Container(
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                                decoration: BoxDecoration(
                                  color: const Color(0xFF291717),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Text(
                                  widget.entry.genres![index],
                                  style: const TextStyle(color: Color(0xFFd88787), fontSize: 13, fontWeight: FontWeight.w500),
                                ),
                              );
                            },
                          ),
                        ),
                      const SizedBox(height: 16),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        child: Row(
                          children: [
                            Expanded(
                              child: Container(
                                decoration: BoxDecoration(
                                  color: const Color(0xFF1b0b0e),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: Colors.white.withOpacity(0.05)),
                                ),
                                child: TextField(
                                  onChanged: (val) => setState(() => _searchQuery = val),
                                  style: const TextStyle(color: Colors.white, fontSize: 13),
                                  decoration: const InputDecoration(
                                    hintText: "Quick jump to chapter (e.g. 110)...",
                                    hintStyle: TextStyle(color: Colors.grey, fontSize: 13),
                                    prefixIcon: Icon(Icons.search, color: Colors.grey, size: 18),
                                    border: InputBorder.none,
                                    contentPadding: EdgeInsets.symmetric(vertical: 12),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                              decoration: BoxDecoration(
                                color: const Color(0xFF281318),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                "$totalCount Total",
                                style: const TextStyle(color: Colors.grey, fontSize: 10, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                              ),
                            ),
                          ],
                        ),
                      ),
                                              if (_isLoading)
                        const Padding(padding: EdgeInsets.all(40), child: Center(child: CircularProgressIndicator(color: Color(0xFFff7b7b))))
                      else if (_error != null)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text("Failed to load chapters: $_error", style: const TextStyle(color: Colors.redAccent, fontSize: 13), textAlign: TextAlign.center)),
                        )
                      else if (filteredChunks.isEmpty)
                        const Padding(padding: EdgeInsets.all(40), child: Center(child: Text("No chapters found", style: TextStyle(color: Colors.grey))))
                      else
                        ListView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: filteredChunks.length,
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          itemBuilder: (context, index) {
                            final chunk = filteredChunks[index];
                            final isUnread = index < 4;
                            return GestureDetector(
                              onTap: () {
                                if (widget.entry.contentType == ContentType.novel) {
                                  Navigator.push(context, MaterialPageRoute(builder: (_) => NovelReaderScreen(entry: widget.entry, chunk: chunk, source: widget.source)));
                                } else if (widget.entry.contentType == ContentType.anime) {
                                  Navigator.push(context, MaterialPageRoute(builder: (_) => PlayerScreen(entry: widget.entry, chunk: chunk, source: widget.source)));
                                } else {
                                  Navigator.push(context, MaterialPageRoute(builder: (_) => MangaReaderScreen(entry: widget.entry, initialChunk: chunk, source: widget.source, allChunks: _chunks)));
                                }
                              },
                              child: Container(
                                margin: const EdgeInsets.only(bottom: 8),
                                padding: const EdgeInsets.all(14),
                                decoration: BoxDecoration(
                                  color: isUnread ? const Color(0xFF1a0a0e) : const Color(0xFF15070a).withOpacity(0.6),
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(color: isUnread ? Colors.white.withOpacity(0.04) : Colors.white.withOpacity(0.02)),
                                ),
                                child: Row(
                                  children: [
                                    if (isUnread)
                                      Container(
                                        width: 4, height: 28, margin: const EdgeInsets.only(right: 12),
                                        decoration: BoxDecoration(color: const Color(0xFFff7b7b), borderRadius: const BorderRadius.horizontal(right: Radius.circular(4)), boxShadow: [BoxShadow(color: const Color(0xFFff7b7b).withOpacity(0.9), blurRadius: 10)]),
                                      ),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Row(
                                            children: [
                                              if (isUnread) ...[
                                                Container(width: 6, height: 6, decoration: const BoxDecoration(color: Color(0xFFff7b7b), shape: BoxShape.circle)),
                                                const SizedBox(width: 8),
                                              ],
                                              Expanded(
                                                child: Text(chunk.title, style: TextStyle(color: isUnread ? Colors.white : Colors.grey[400], fontSize: 14, fontWeight: isUnread ? FontWeight.w600 : FontWeight.w500, letterSpacing: -0.2), overflow: TextOverflow.ellipsis),
                                              ),
                                            ],
                                          ),
                                          const SizedBox(height: 4),
                                          Padding(
                                            padding: EdgeInsets.only(left: isUnread ? 14.0 : 0.0),
                                            child: Text(isUnread ? "15/09/2026 • New" : "14/09/2026", style: TextStyle(color: isUnread ? Colors.grey : Colors.grey[700], fontSize: 11, fontWeight: FontWeight.w500)),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Container(
                                      width: 36, height: 36,
                                      decoration: BoxDecoration(shape: BoxShape.circle, color: isUnread ? const Color(0xFF2c151a) : const Color(0xFF1e0a0d), border: Border.all(color: Colors.white.withOpacity(0.05))),
                                      child: const Icon(Icons.file_download_outlined, size: 16, color: Colors.grey),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      const SizedBox(height: 100),
                    ],
                  ),
                ),
              ],
            ),
            Positioned(
              bottom: 24, right: 24,
              child: InkWell(
                onTap: () {
                  if (_chunks.isNotEmpty) {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => MangaReaderScreen(entry: widget.entry, initialChunk: _chunks.first, source: widget.source, allChunks: _chunks)));
                  }
                },
                borderRadius: BorderRadius.circular(16),
                child: Container(
                  width: 56, height: 56,
                  decoration: BoxDecoration(color: const Color(0xFFff7b7b), borderRadius: BorderRadius.circular(16), boxShadow: [BoxShadow(color: const Color(0xFFff7b7b).withOpacity(0.35), blurRadius: 15, offset: const Offset(0, 4))]),
                  child: const Icon(Icons.play_arrow, color: Color(0xFF120406), size: 28),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetaRow(IconData icon, String text) {
    return Row(
      children: [
        Icon(icon, size: 16, color: Colors.grey[500]),
        const SizedBox(width: 8),
        Expanded(child: Text(text, style: const TextStyle(color: Color(0xFF9ca3af), fontSize: 13, fontWeight: FontWeight.w500), overflow: TextOverflow.ellipsis)),
      ],
    );
  }

  Widget _buildStatItem(String label, String value, {bool isRating = false}) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Color(0xFF828797), fontSize: 10, fontWeight: FontWeight.bold, letterSpacing: 1)),
        const SizedBox(height: 4),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isRating) ...[const Icon(Icons.star, size: 14, color: Color(0xFFfbbf24)), const SizedBox(width: 4)],
            Text(value, style: TextStyle(color: isRating ? const Color(0xFFfbbf24) : Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          ],
        ),
      ],
    );
  }

  Widget _buildActionButton(IconData icon, String label, Color iconColor, Color bgColor, VoidCallback onTap) {
    return InkWell(
      onTap: onTap, borderRadius: BorderRadius.circular(16),
      child: Column(
        children: [
          Container(width: 54, height: 54, decoration: BoxDecoration(color: bgColor, borderRadius: BorderRadius.circular(18)), child: Icon(icon, color: iconColor, size: 22)),
          const SizedBox(height: 6),
          Text(label, style: TextStyle(color: iconColor, fontSize: 10, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

