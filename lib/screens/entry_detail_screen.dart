import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../services/category_manager.dart';
import '../services/extension_manager.dart';
import '../services/library_manager.dart';
import 'manga_reader_screen.dart';
import 'novel_reader_screen.dart';
import 'player_screen.dart';
import 'webview_screen.dart';

const _bg = Color(0xFF0D0809);
const _card = Color(0xFF1A0F11);
const _accent = Color(0xFFFF7B7B);
const _accentDim = Color(0xFF2A1416);
const _libraryFg = Color(0xFF7C7CFF);
const _libraryBg = Color(0xFF26224A);
const _trackerBg = Color(0xFF3A1A1E);
const _statsBg = Color(0xFF141A28);
const _gold = Color(0xFFFFC83D);
const _btnBg = Color(0xFF211A1B);
const _muted = Color(0xFF9A9092);

String _fmtNum(double n) => n == n.roundToDouble() ? n.toInt().toString() : n.toString();

String _fmtDate(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

String _stripHtml(String s) {
  var t = s
      .replaceAll(RegExp(r'<br\s*/?>', caseSensitive: false), '\n')
      .replaceAll(RegExp(r'</p>', caseSensitive: false), '\n\n')
      .replaceAll(RegExp(r'<[^>]*>'), '')
      .replaceAll('&nbsp;', ' ')
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&#8217;', "'");
  return t.trim();
}

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
  bool _descExpanded = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final details = await source.getEntryDetails(widget.entry.id);
    final chunks = await source.getChunks(widget.entry.id);
    setState(() {
      _details = details;
      _chunks = chunks;
      _loading = false;
    });
  }

  void _comingSoon(String what) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$what is coming soon.')),
    );
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

  void _openWebView(Entry e) {
    final url = e.sourceUrl;
    if (url == null || url.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('This source has no webpage link for this entry.')),
      );
      return;
    }
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => SourceWebViewScreen(url: url, title: e.title)),
    );
  }

  void _openChapterList(Entry e) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => _ChapterListScreen(
          title: e.title,
          isAnime: e.type == ContentType.anime,
          chunks: _chunks,
          onOpen: _openChunk,
          onRefresh: _load,
        ),
      ),
    );
  }

  void _openCategoryPicker() {
    final catState = ref.read(categoryManagerProvider);
    final catManager = ref.read(categoryManagerProvider.notifier);
    if (catState.categories.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No categories yet — create one from Settings → Categories.')),
      );
      return;
    }
    final current = catManager.getAssignments(widget.sourceId, widget.entry.id).toSet();
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Set categories'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: catState.categories
                  .map((c) => CheckboxListTile(
                        title: Text(c.name),
                        value: current.contains(c.id),
                        onChanged: (checked) => setDialogState(() {
                          if (checked == true) {
                            current.add(c.id);
                          } else {
                            current.remove(c.id);
                          }
                        }),
                      ))
                  .toList(),
            ),
          ),
          actions: [
            FilledButton(
              onPressed: () {
                catManager.setAssignments(widget.sourceId, widget.entry.id, current.toList());
                Navigator.pop(context);
              },
              child: const Text('Done'),
            ),
          ],
        ),
      ),
    );
  }

  String _statusLine(Entry e) {
    final parts = <String>[];
    if (e.status != EntryStatus.unknown) {
      final n = e.status.name;
      parts.add(n[0].toUpperCase() + n.substring(1));
    }
    parts.add(widget.sourceId);
    return parts.join(' • ');
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(
        backgroundColor: _bg,
        body: Center(child: CircularProgressIndicator(color: _accent)),
      );
    }
    final e = _details!;
    final chunkWord = e.type == ContentType.anime ? 'Episode' : 'Chapter';
    final desc = (e.description == null) ? '' : _stripHtml(e.description!);
    final hasStats = e.rank != null || e.rating != null || e.saves != null;

    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _bg,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.cloud_download_outlined),
            onPressed: () => _comingSoon('Downloads'),
          ),
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: () => _openChapterList(e),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_horiz),
            onSelected: (value) {
              if (value == 'refresh') _load();
              if (value == 'categories') _openCategoryPicker();
            },
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'refresh', child: Text('Refresh')),
              PopupMenuItem(value: 'categories', child: Text('Set categories')),
            ],
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          // Header: cover + title/meta
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: SizedBox(
                  width: 112,
                  height: 156,
                  child: e.coverUrl != null
                      ? CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover)
                      : Container(color: _card),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      e.title,
                      style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800, height: 1.15),
                    ),
                    const SizedBox(height: 12),
                    if (e.author != null && e.author!.isNotEmpty)
                      _metaRow(Icons.person_outline, e.author!),
                    if (e.artist != null && e.artist!.isNotEmpty)
                      _metaRow(Icons.edit_outlined, e.artist!),
                    _metaRow(Icons.schedule, _statusLine(e)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Stats card (only when the source provides data)
          if (hasStats) ...[
            _statsCard(e),
            const SizedBox(height: 20),
          ],
          // Action row
          Consumer(
            builder: (context, ref, _) {
              final library = ref.watch(libraryManagerProvider.notifier);
              ref.watch(libraryManagerProvider);
              final fav = library.isFavorite(widget.sourceId, widget.entry.id);
              return Row(
                children: [
                  _actionButton(
                    icon: fav ? Icons.menu_book : Icons.menu_book_outlined,
                    label: fav ? 'In Library' : 'Add to Library',
                    fg: fav ? _libraryFg : _muted,
                    bg: fav ? _libraryBg : _btnBg,
                    onTap: () => library.toggle(e),
                  ),
                  _actionButton(
                    icon: Icons.event_note_outlined,
                    label: 'Soon',
                    fg: _muted,
                    bg: _btnBg,
                    onTap: () => _comingSoon('This'),
                  ),
                  _actionButton(
                    icon: Icons.check_circle_outline,
                    label: 'Trackers',
                    fg: _accent,
                    bg: _trackerBg,
                    onTap: () => _comingSoon('Trackers'),
                  ),
                  _actionButton(
                    icon: Icons.explore_outlined,
                    label: 'WebView',
                    fg: _muted,
                    bg: _btnBg,
                    onTap: () => _openWebView(e),
                  ),
                  _actionButton(
                    icon: Icons.call_split,
                    label: 'Merge',
                    fg: _muted,
                    bg: _btnBg,
                    onTap: () => _comingSoon('Merge'),
                  ),
                ],
              );
            },
          ),
          const SizedBox(height: 20),
          // Description (expandable, HTML stripped)
          if (desc.isNotEmpty)
            InkWell(
              onTap: () => setState(() => _descExpanded = !_descExpanded),
              child: Column(
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      desc,
                      maxLines: _descExpanded ? null : 2,
                      overflow: _descExpanded ? TextOverflow.visible : TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 16, height: 1.5, color: _muted),
                    ),
                  ),
                  Icon(_descExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, color: _muted),
                ],
              ),
            ),
          const SizedBox(height: 12),
          // Genre chips (horizontal scroll)
          if (e.genres.isNotEmpty)
            SizedBox(
              height: 42,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: e.genres.length,
                separatorBuilder: (_, __) => const SizedBox(width: 10),
                itemBuilder: (context, i) => Container(
                  padding: const EdgeInsets.symmetric(horizontal: 18),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: _accentDim, borderRadius: BorderRadius.circular(14)),
                  child: Text(e.genres[i], style: const TextStyle(color: _accent, fontSize: 15)),
                ),
              ),
            ),
          const SizedBox(height: 20),
          // Chapters entry point
          InkWell(
            onTap: () => _openChapterList(e),
            borderRadius: BorderRadius.circular(18),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
              decoration: BoxDecoration(color: _card, borderRadius: BorderRadius.circular(18)),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '${_chunks.length} $chunkWord${_chunks.length == 1 ? '' : 's'}',
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                    ),
                  ),
                  const Icon(Icons.arrow_forward, color: _muted),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _statsCard(Entry e) {
    String rankText = '—';
    if (e.rank != null && e.rank!.isNotEmpty) {
      rankText = e.rank!.startsWith('#') ? e.rank! : '#${e.rank}';
    }
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 18),
      decoration: BoxDecoration(color: _statsBg, borderRadius: BorderRadius.circular(24)),
      child: Row(
        children: [
          _stat('RANK', Text(rankText, style: _statValueStyle)),
          _statDivider(),
          _stat(
            'RATING',
            e.rating == null
                ? const Text('—', style: _statValueStyle)
                : Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.star, color: _gold, size: 20),
                      const SizedBox(width: 6),
                      Text(e.rating!.toStringAsFixed(2),
                          style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w800, color: _gold)),
                    ],
                  ),
          ),
          _statDivider(),
          _stat('SAVES', Text(e.saves ?? '—', style: _statValueStyle)),
        ],
      ),
    );
  }

  static const _statValueStyle = TextStyle(fontSize: 24, fontWeight: FontWeight.w800);

  Widget _stat(String label, Widget value) {
    return Expanded(
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 13, letterSpacing: 1.5, fontWeight: FontWeight.w600, color: _muted)),
          const SizedBox(height: 8),
          value,
        ],
      ),
    );
  }

  Widget _statDivider() => Container(width: 1, height: 44, color: Colors.white12);

  Widget _metaRow(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Icon(icon, size: 20, color: _muted),
          const SizedBox(width: 10),
          Expanded(
            child: Text(text, style: const TextStyle(fontSize: 16, color: _muted), maxLines: 2, overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
    );
  }

  Widget _actionButton({
    required IconData icon,
    required String label,
    required Color fg,
    required Color bg,
    required VoidCallback onTap,
  }) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Column(
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(18)),
              child: Icon(icon, color: fg, size: 26),
            ),
            const SizedBox(height: 8),
            Text(
              label,
              style: TextStyle(fontSize: 12, color: fg),
              textAlign: TextAlign.center,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

/// Full chapter list: quick-jump search, unread dots, "New" tag, dimmed read
/// chapters, download button (placeholder), and a resume/start FAB.
class _ChapterListScreen extends StatefulWidget {
  final String title;
  final bool isAnime;
  final List<EntryChunk> chunks;
  final void Function(EntryChunk) onOpen;
  final Future<void> Function() onRefresh;

  const _ChapterListScreen({
    required this.title,
    required this.isAnime,
    required this.chunks,
    required this.onOpen,
    required this.onRefresh,
  });

  @override
  State<_ChapterListScreen> createState() => _ChapterListScreenState();
}

class _ChapterListScreenState extends State<_ChapterListScreen> {
  bool _newestFirst = true;
  String _query = '';

  List<EntryChunk> get _sortedAsc {
    final list = List<EntryChunk>.from(widget.chunks);
    list.sort((a, b) => a.number.compareTo(b.number));
    return list;
  }

  List<EntryChunk> get _visible {
    var list = _sortedAsc;
    if (_newestFirst) list = list.reversed.toList();
    final q = _query.trim();
    if (q.isNotEmpty) {
      list = list.where((c) => _fmtNum(c.number).contains(q) || c.title.toLowerCase().contains(q.toLowerCase())).toList();
    }
    return list;
  }

  bool _isNew(EntryChunk c, int index) {
    if (c.read || c.uploadDate == null) return false;
    if (!_newestFirst || index != 0 || _query.isNotEmpty) return false;
    return DateTime.now().difference(c.uploadDate!).inDays <= 7;
  }

  void _resume() {
    final asc = _sortedAsc;
    if (asc.isEmpty) return;
    final unread = asc.where((c) => !c.read);
    widget.onOpen(unread.isNotEmpty ? unread.first : asc.last);
  }

  @override
  Widget build(BuildContext context) {
    final items = _visible;
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _bg,
        surfaceTintColor: Colors.transparent,
        title: Text(widget.title, style: const TextStyle(fontWeight: FontWeight.w800), overflow: TextOverflow.ellipsis),
        actions: [
          IconButton(
            icon: const Icon(Icons.filter_list),
            tooltip: _newestFirst ? 'Newest first' : 'Oldest first',
            onPressed: () => setState(() => _newestFirst = !_newestFirst),
          ),
          IconButton(icon: const Icon(Icons.refresh), onPressed: widget.onRefresh),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _accent,
        foregroundColor: Colors.black,
        onPressed: widget.chunks.isEmpty ? null : _resume,
        child: const Icon(Icons.play_arrow_rounded, size: 32),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
            child: TextField(
              keyboardType: TextInputType.text,
              onChanged: (v) => setState(() => _query = v),
              decoration: InputDecoration(
                hintText: 'Quick jump to ${widget.isAnime ? 'episode' : 'chapter'} (e.g. 110)...',
                hintStyle: const TextStyle(color: _muted),
                prefixIcon: const Icon(Icons.search, color: _muted),
                suffixIcon: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(color: _btnBg, borderRadius: BorderRadius.circular(10)),
                    child: Text(
                      '${widget.chunks.length} TOTAL',
                      style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: _muted),
                    ),
                  ),
                ),
                filled: true,
                fillColor: _card,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: BorderSide.none),
              ),
            ),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
              itemCount: items.length,
              itemBuilder: (context, i) {
                final c = items[i];
                final isNew = _isNew(c, i);
                final dimmed = c.read;
                final textColor = dimmed ? _muted.withOpacity(0.6) : Colors.white;
                final subParts = <String>[];
                if (c.uploadDate != null) subParts.add(_fmtDate(c.uploadDate!));
                if (isNew) subParts.add('New');
                return Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Material(
                    color: dimmed ? _card.withOpacity(0.5) : _card,
                    borderRadius: BorderRadius.circular(20),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: () => widget.onOpen(c),
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(20, 16, 12, 16),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      if (!c.read) ...[
                                        Container(
                                          width: 8,
                                          height: 8,
                                          decoration: const BoxDecoration(color: _accent, shape: BoxShape.circle),
                                        ),
                                        const SizedBox(width: 10),
                                      ],
                                      Expanded(
                                        child: Text(
                                          c.title,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: textColor),
                                        ),
                                      ),
                                    ],
                                  ),
                                  if (subParts.isNotEmpty) ...[
                                    const SizedBox(height: 6),
                                    Text(
                                      subParts.join(' • '),
                                      style: TextStyle(fontSize: 15, color: _muted.withOpacity(dimmed ? 0.5 : 1)),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                            IconButton(
                              style: IconButton.styleFrom(backgroundColor: _btnBg),
                              icon: Icon(Icons.file_download_outlined, color: textColor),
                              onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('Downloads are not available yet.')),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
