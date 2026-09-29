import 'dart:math';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../services/category_manager.dart';
import '../services/extension_manager.dart';
import '../services/library_manager.dart';
import '../theme/app_palette.dart';
import 'browse_screen.dart';
import 'manga_reader_screen.dart';
import 'novel_reader_screen.dart';
import 'player_screen.dart';
import 'webview_screen.dart';

// Neutral colors (theme-independent)
const _statLabel = Color(0xFF828797);
const _gold = Color(0xFFFBBF24);
const _btnFg = Color(0xFFA19D9E);
const _gray400 = Color(0xFF9CA3AF);
const _gray500 = Color(0xFF6B7280);
const _iconBar = Color(0xFFD4D4D8);

// ---------- Exact icon SVGs ----------
const _svgBack = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
''';
const _svgCloudDownload = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242"/><path d="M12 12v9"/><path d="m8 17 4 4 4-4"/></svg>
''';
const _svgFilter = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
''';
const _svgMoreDots = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
''';
const _svgPerson = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
''';
const _svgPencil = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
''';
const _svgClock = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
''';
const _svgStar = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="white"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
''';
const _svgBookOpen = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
''';
const _svgCalendarClock = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 7.5V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h3.5"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h5"/><path d="M17.5 17.5 16 16.25V14"/><circle cx="16" cy="16" r="6"/></svg>
''';
const _svgCheckCircle = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
''';
const _svgCompass = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>
''';
const _svgGitMerge = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="18" r="3"/><circle cx="6" cy="6" r="3"/><path d="M6 21V9a9 9 0 0 0 9 9"/></svg>
''';
const _svgChevronDown = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 9l-7 7-7-7"/></svg>
''';
const _svgDownloadTray = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
''';
const _svgPlay = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white"><path d="M5.536 21.886a1.004 1.004 0 0 0 1.033-.064l13-9a1 1 0 0 0 0-1.644l-13-9A1 1 0 0 0 5 3v18a1 1 0 0 0 .536.886z"/></svg>
''';
const _svgSearch = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.35-4.35"/></svg>
''';

Widget _svgIcon(String svg, {double size = 24, required Color color}) {
  return SvgPicture.string(
    svg,
    width: size,
    height: size,
    colorFilter: ColorFilter.mode(color, BlendMode.srcIn),
  );
}

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

/// Removes markdown link/bold syntax, keeping the visible text.
String _cleanInline(String s) {
  return s
      .replaceAllMapped(RegExp(r'\[([^\]]+)\]\([^)]*\)'), (m) => m[1]!)
      .replaceAllMapped(RegExp(r'\*\*(.+?)\*\*'), (m) => m[1]!)
      .trim();
}

final _bulletRe = RegExp(r'^[-*•]\s+');

String _plainDescription(String desc) {
  final parts = <String>[];
  for (final raw in desc.split('\n')) {
    final line = raw.trim();
    if (line.isEmpty) continue;
    parts.add(_cleanInline(line.replaceFirst(_bulletRe, '')));
  }
  return parts.join(' ');
}

class _BarIcon extends StatelessWidget {
  final String svg;
  final Color color;
  final VoidCallback onTap;
  final String? tooltip;
  const _BarIcon({required this.svg, required this.color, required this.onTap, this.tooltip});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: _svgIcon(svg, size: 20, color: color),
      tooltip: tooltip,
      onPressed: onTap,
      padding: const EdgeInsets.all(6),
      constraints: const BoxConstraints(minWidth: 34, minHeight: 34),
      splashRadius: 20,
    );
  }
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
  bool _newestFirst = true;
  String _query = '';
  late AppPalette _p;

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

  List<EntryChunk> get _sortedAsc {
    final list = List<EntryChunk>.from(_chunks);
    list.sort((a, b) => a.number.compareTo(b.number));
    return list;
  }

  List<EntryChunk> get _visibleChunks {
    var list = _sortedAsc;
    if (_newestFirst) list = list.reversed.toList();
    final q = _query.trim();
    if (q.isNotEmpty) {
      list = list
          .where((c) => _fmtNum(c.number).contains(q) || c.title.toLowerCase().contains(q.toLowerCase()))
          .toList();
    }
    return list;
  }

  bool _isNew(EntryChunk c, int index) {
    if (c.read || c.uploadDate == null) return false;
    if (!_newestFirst || index != 0 || _query.isNotEmpty) return false;
    return DateTime.now().difference(c.uploadDate!).inDays <= 7;
  }

  EntryChunk? get _resumeTarget {
    final asc = _sortedAsc;
    if (asc.isEmpty) return null;
    final unread = asc.where((c) => !c.read);
    return unread.isNotEmpty ? unread.first : asc.last;
  }

  void _resume() {
    final t = _resumeTarget;
    if (t != null) _openChunk(t);
  }

  String _chapterLabel(EntryChunk c) {
    final t = c.title;
    if (RegExp(r'^(ch\.?|chapter|ep\.?|episode)\s', caseSensitive: false).hasMatch(t)) return t;
    return 'Ch. ${_fmtNum(c.number)}: $t';
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

  Future<void> _showTagMenu(Offset pos, String tag) async {
    final size = MediaQuery.of(context).size;
    const style = TextStyle(color: Colors.white);
    final choice = await showMenu<String>(
      context: context,
      position: RelativeRect.fromLTRB(pos.dx, pos.dy, size.width - pos.dx, size.height - pos.dy),
      color: _p.panel,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      items: const [
        PopupMenuItem(value: 'search', child: Text('Search', style: style)),
        PopupMenuItem(value: 'global', child: Text('Global search', style: style)),
        PopupMenuItem(value: 'copy', child: Text('Copy to clipboard', style: style)),
      ],
    );
    if (!mounted || choice == null) return;
    if (choice == 'copy') {
      await Clipboard.setData(ClipboardData(text: tag));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Copied "$tag"')));
    } else {
      openTagSearch(
        context,
        sourceId: widget.sourceId,
        type: widget.entry.type,
        tag: tag,
        global: choice == 'global',
      );
    }
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
    _p = ref.watch(paletteProvider);
    final p = _p;

    if (_loading) {
      return Scaffold(
        backgroundColor: p.bg,
        body: Center(child: CircularProgressIndicator(color: p.accent)),
      );
    }
    final e = _details!;
    final chunkWord = e.type == ContentType.anime ? 'Episode' : 'Chapter';
    final desc = e.description == null ? '' : _stripHtml(e.description!);
    final items = _visibleChunks;

    return Scaffold(
      backgroundColor: p.bg,
      floatingActionButton: SizedBox(
        width: 58,
        height: 58,
        child: Material(
          color: p.accent,
          borderRadius: BorderRadius.circular(19),
          child: InkWell(
            borderRadius: BorderRadius.circular(19),
            onTap: _chunks.isEmpty ? null : _resume,
            child: Padding(
              padding: const EdgeInsets.only(left: 4),
              child: Center(child: _svgIcon(_svgPlay, size: 26, color: p.onAccent)),
            ),
          ),
        ),
      ),
      body: Stack(
        children: [
          _blurBackground(e),
          SafeArea(
            child: CustomScrollView(
              slivers: [
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(4, 4, 4, 0),
                    child: Row(
                      children: [
                        _BarIcon(svg: _svgBack, color: Colors.white, onTap: () => Navigator.pop(context)),
                        const Spacer(),
                        _BarIcon(svg: _svgCloudDownload, color: _iconBar, onTap: () => _comingSoon('Downloads')),
                        _BarIcon(
                          svg: _svgFilter,
                          color: _iconBar,
                          tooltip: _newestFirst ? 'Newest first' : 'Oldest first',
                          onTap: () => setState(() => _newestFirst = !_newestFirst),
                        ),
                        PopupMenuButton<String>(
                          icon: _svgIcon(_svgMoreDots, size: 20, color: _iconBar),
                          padding: const EdgeInsets.all(6),
                          onSelected: (value) {
                            if (value == 'refresh') _load();
                            if (value == 'categories') _openCategoryPicker();
                            if (value == 'theme') showThemePicker(context);
                          },
                          itemBuilder: (context) => const [
                            PopupMenuItem(value: 'refresh', child: Text('Refresh')),
                            PopupMenuItem(value: 'categories', child: Text('Set categories')),
                            PopupMenuItem(value: 'theme', child: Text('Theme')),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                // Header
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: 115,
                          height: 160,
                          clipBehavior: Clip.antiAlias,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: Colors.white10),
                          ),
                          child: e.coverUrl != null
                              ? CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover)
                              : Container(color: p.surface),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Padding(
                            padding: const EdgeInsets.only(top: 2),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  e.title,
                                  style: const TextStyle(
                                    fontSize: 22,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                    height: 1.2,
                                    letterSpacing: -0.3,
                                  ),
                                ),
                                const SizedBox(height: 12),
                                if (e.author != null && e.author!.isNotEmpty) _metaRow(_svgPerson, e.author!),
                                if (e.artist != null && e.artist!.isNotEmpty) _metaRow(_svgPencil, e.artist!),
                                _metaRow(_svgClock, _statusLine(e)),
                                _startReadingButton(),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Stats
                SliverToBoxAdapter(
                  child: Container(
                    margin: const EdgeInsets.fromLTRB(16, 24, 16, 0),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    decoration: BoxDecoration(color: p.statsBg, borderRadius: BorderRadius.circular(20)),
                    child: Row(
                      children: [
                        _stat('RANK', Text(_rankText(e), style: _statValueStyle)),
                        _statDivider(),
                        _stat(
                          'RATING',
                          e.rating == null
                              ? const Text('—', style: _statValueStyle)
                              : Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    _svgIcon(_svgStar, size: 14, color: _gold),
                                    const SizedBox(width: 4),
                                    Text(e.rating!.toStringAsFixed(2),
                                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: _gold)),
                                  ],
                                ),
                        ),
                        _statDivider(),
                        _stat('SAVES', Text(e.saves ?? '—', style: _statValueStyle)),
                      ],
                    ),
                  ),
                ),
                // Actions
                SliverToBoxAdapter(
                  child: Consumer(
                    builder: (context, ref, _) {
                      final library = ref.watch(libraryManagerProvider.notifier);
                      ref.watch(libraryManagerProvider);
                      final fav = library.isFavorite(widget.sourceId, widget.entry.id);
                      return Padding(
                        padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            _actionButton(
                              svg: _svgBookOpen,
                              label: fav ? 'In Library' : 'Add to Library',
                              fg: fav ? p.secondary : _btnFg,
                              bg: fav ? p.libraryBg : p.btnBg,
                              onTap: () => library.toggle(e),
                            ),
                            _actionButton(
                              svg: _svgCalendarClock,
                              label: 'Soon',
                              fg: _btnFg,
                              bg: p.btnBg,
                              onTap: () => _comingSoon('This'),
                            ),
                            _actionButton(
                              svg: _svgCheckCircle,
                              label: 'Trackers',
                              fg: p.accent,
                              bg: p.trackerBg,
                              onTap: () => _comingSoon('Trackers'),
                            ),
                            _actionButton(
                              svg: _svgCompass,
                              label: 'WebView',
                              fg: _btnFg,
                              bg: p.btnBg,
                              onTap: () => _openWebView(e),
                            ),
                            _actionButton(
                              svg: _svgGitMerge,
                              label: 'Merge',
                              fg: _btnFg,
                              bg: p.btnBg,
                              onTap: () => _comingSoon('Merge'),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
                // Synopsis
                if (desc.isNotEmpty)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 24, 16, 0),
                      child: InkWell(
                        onTap: () => setState(() => _descExpanded = !_descExpanded),
                        child: Column(
                          children: [
                            if (_descExpanded)
                              _expandedDescription(desc)
                            else
                              Align(
                                alignment: Alignment.centerLeft,
                                child: Text(
                                  _plainDescription(desc),
                                  maxLines: 3,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(fontSize: 15, color: Colors.white.withOpacity(0.7), height: 1.5),
                                ),
                              ),
                            const SizedBox(height: 6),
                            Transform.rotate(
                              angle: _descExpanded ? pi : 0,
                              child: _svgIcon(_svgChevronDown, size: 22, color: _gray500),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                // Tags
                if (e.genres.isNotEmpty)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: SizedBox(
                        height: 38,
                        child: ListView.separated(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          scrollDirection: Axis.horizontal,
                          itemCount: e.genres.length,
                          separatorBuilder: (_, __) => const SizedBox(width: 10),
                          itemBuilder: (context, i) {
                            final tag = e.genres[i];
                            return GestureDetector(
                              onTapUp: (d) => _showTagMenu(d.globalPosition, tag),
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                                alignment: Alignment.center,
                                decoration: BoxDecoration(color: p.tagBg, borderRadius: BorderRadius.circular(8)),
                                child: Text(tag,
                                    style: TextStyle(color: p.tagFg, fontSize: 13, fontWeight: FontWeight.w500)),
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                  ),
                // Chapters header
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 26, 16, 12),
                    child: Text('${_chunks.length} $chunkWord${_chunks.length == 1 ? '' : 's'}',
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                  ),
                ),
                // Search
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                    child: Container(
                      height: 52,
                      decoration: BoxDecoration(color: p.surface, borderRadius: BorderRadius.circular(22)),
                      child: Row(
                        children: [
                          const SizedBox(width: 16),
                          _svgIcon(_svgSearch, size: 17, color: _gray500),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextField(
                              style: const TextStyle(color: Colors.white, fontSize: 14.5),
                              onChanged: (v) => setState(() => _query = v),
                              decoration: InputDecoration(
                                isDense: true,
                                hintText:
                                    'Quick jump to ${e.type == ContentType.anime ? 'episode' : 'chapter'} (e.g. 110)...',
                                hintStyle: const TextStyle(color: _gray500, fontSize: 14.5),
                                border: InputBorder.none,
                              ),
                            ),
                          ),
                          Container(
                            margin: const EdgeInsets.only(right: 6),
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(color: p.btnBg, borderRadius: BorderRadius.circular(12)),
                            child: Text('${_chunks.length} TOTAL',
                                style: const TextStyle(
                                    fontSize: 11.5, fontWeight: FontWeight.w700, color: _gray500, letterSpacing: 0.4)),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                SliverList.builder(
                  itemCount: items.length,
                  itemBuilder: (context, i) => _chapterRow(items[i], i),
                ),
                const SliverToBoxAdapter(child: SizedBox(height: 96)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _blurBackground(Entry e) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      height: 520,
      child: ClipRect(
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (e.coverUrl != null)
              ImageFiltered(
                imageFilter: ui.ImageFilter.blur(sigmaX: 28, sigmaY: 28),
                child: CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover),
              ),
            Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [_p.bg.withOpacity(0.35), _p.bg.withOpacity(0.78), _p.bg],
                  stops: const [0.0, 0.6, 1.0],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _startReadingButton() {
    final target = _resumeTarget;
    if (target == null) return const SizedBox.shrink();
    final anyRead = _chunks.any((c) => c.read);
    return Padding(
      padding: const EdgeInsets.only(top: 6),
      child: Material(
        color: Color.alphaBlend(Colors.white.withOpacity(0.10), _p.bg),
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: () => _openChunk(target),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                _svgIcon(_svgPlay, size: 18, color: Colors.white),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(anyRead ? 'Continue Reading' : 'Start Reading',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                      const SizedBox(height: 2),
                      Text(_chapterLabel(target),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(color: Colors.white70, fontSize: 13)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _expandedDescription(String desc) {
    final bodyStyle = TextStyle(fontSize: 15, color: Colors.white.withOpacity(0.78), height: 1.5);
    final headingStyle = TextStyle(fontSize: 15, color: Colors.white.withOpacity(0.95), height: 1.5);
    final children = <Widget>[];
    for (final raw in desc.split('\n')) {
      final line = raw.trim();
      if (line.isEmpty) continue;
      final isBullet = _bulletRe.hasMatch(line);
      final text = _cleanInline(line.replaceFirst(_bulletRe, ''));
      final isHeading = !isBullet && RegExp(r'^\*\*.+\*\*:?$').hasMatch(line);
      if (isBullet) {
        children.add(Padding(
          padding: const EdgeInsets.only(left: 12, top: 6),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('•  ', style: bodyStyle),
              Expanded(child: Text(text, style: bodyStyle)),
            ],
          ),
        ));
      } else {
        children.add(Padding(
          padding: EdgeInsets.only(top: children.isEmpty ? 0 : 14),
          child: Align(
            alignment: Alignment.centerLeft,
            child: Text(text, style: isHeading ? headingStyle : bodyStyle),
          ),
        ));
      }
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: children);
  }

  Widget _chapterRow(EntryChunk c, int index) {
    final p = _p;
    final unread = !c.read;
    final isNew = _isNew(c, index);
    final titleColor = unread ? Colors.white : Colors.white.withOpacity(0.4);
    final subColor = unread ? const Color(0xFF8B8B93) : Colors.white.withOpacity(0.3);
    final sub = <String>[];
    if (c.uploadDate != null) sub.add(_fmtDate(c.uploadDate!));
    if (isNew) sub.add('New');

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Material(
        color: unread ? p.surface : p.surface.withOpacity(0.55),
        borderRadius: BorderRadius.circular(18),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => _openChunk(c),
          child: Stack(
            children: [
              if (unread)
                Positioned.fill(
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      width: 4,
                      height: 24,
                      decoration: BoxDecoration(
                        color: p.accent,
                        borderRadius: const BorderRadius.horizontal(right: Radius.circular(4)),
                        boxShadow: [BoxShadow(color: p.accent.withOpacity(0.5), blurRadius: 8)],
                      ),
                    ),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 10, 8),
                child: Row(
                  children: [
                    if (unread) ...[
                      Container(width: 7, height: 7, decoration: BoxDecoration(color: p.accent, shape: BoxShape.circle)),
                      const SizedBox(width: 10),
                    ],
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(c.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: titleColor)),
                          if (sub.isNotEmpty)
                            Padding(
                              padding: const EdgeInsets.only(top: 2),
                              child: Text(sub.join(' • '),
                                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: subColor)),
                            ),
                        ],
                      ),
                    ),
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.05),
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white.withOpacity(0.08)),
                      ),
                      child: IconButton(
                        padding: EdgeInsets.zero,
                        icon: _svgIcon(_svgDownloadTray, size: 16, color: unread ? _iconBar : subColor),
                        onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Downloads are not available yet.')),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _rankText(Entry e) {
    if (e.rank == null || e.rank!.isEmpty) return '—';
    return e.rank!.startsWith('#') ? e.rank! : '#${e.rank}';
  }

  static const _statValueStyle = TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white);

  Widget _stat(String label, Widget value) {
    return Expanded(
      child: Column(
        children: [
          Text(label,
              style: const TextStyle(fontSize: 10, letterSpacing: 1.2, fontWeight: FontWeight.bold, color: _statLabel)),
          const SizedBox(height: 4),
          value,
        ],
      ),
    );
  }

  Widget _statDivider() => Container(width: 1, height: 40, color: Colors.white10);

  Widget _metaRow(String svg, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          _svgIcon(svg, size: 16, color: _gray500),
          const SizedBox(width: 8),
          Expanded(
            child: Text(text,
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: _gray400),
                maxLines: 2,
                overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
    );
  }

  Widget _actionButton({
    required String svg,
    required String label,
    required Color fg,
    required Color bg,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(19),
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(19)),
            child: _svgIcon(svg, size: 24, color: fg),
          ),
          const SizedBox(height: 8),
          Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: fg)),
        ],
      ),
    );
  }
}
