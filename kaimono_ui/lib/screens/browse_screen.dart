import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/content_type.dart';
import '../models/entry.dart';
import '../services/content_filter.dart';
import '../services/extension_manager.dart';
import '../theme/app_palette.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

/// Opens a search for [tag]: inside one source, or across all sources of [type].
void openTagSearch(
  BuildContext context, {
  required String sourceId,
  required ContentType type,
  required String tag,
  required bool global,
}) {
  Navigator.push(
    context,
    MaterialPageRoute(
      builder: (_) => global
          ? GlobalSearchScreen(initialQuery: tag, type: type)
          : SourceBrowseScreen(sourceId: sourceId, initialQuery: tag),
    ),
  );
}

/// Pill-shaped search field used across the Discover screens.
class _PillField extends StatelessWidget {
  final AppPalette palette;
  final TextEditingController controller;
  final String hint;
  final IconData icon;
  final bool accented;
  final ValueChanged<String>? onSubmitted;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onClear;
  final Widget? trailing;

  const _PillField({
    required this.palette,
    required this.controller,
    required this.hint,
    required this.icon,
    this.accented = false,
    this.onSubmitted,
    this.onChanged,
    this.onClear,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final p = palette;
    return Container(
      height: 52,
      padding: const EdgeInsets.only(left: 16, right: 6),
      decoration: BoxDecoration(
        color: accented ? p.accent.withOpacity(0.10) : p.surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: accented ? p.accent.withOpacity(0.45) : Colors.white10),
      ),
      child: Row(
        children: [
          Icon(icon, color: accented ? p.accent : Colors.white54, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: controller,
              textInputAction: TextInputAction.search,
              onSubmitted: onSubmitted,
              onChanged: onChanged,
              style: const TextStyle(color: Colors.white, fontSize: 15),
              decoration: InputDecoration(
                isDense: true,
                border: InputBorder.none,
                hintText: hint,
                hintStyle: const TextStyle(color: Colors.white38, fontSize: 15),
              ),
            ),
          ),
          if (onClear != null)
            ValueListenableBuilder<TextEditingValue>(
              valueListenable: controller,
              builder: (context, value, _) => value.text.isEmpty
                  ? const SizedBox.shrink()
                  : IconButton(
                      icon: const Icon(Icons.close, size: 18, color: Colors.white54),
                      onPressed: onClear,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
                    ),
            ),
          if (trailing != null) trailing!,
        ],
      ),
    );
  }
}

class BrowseScreen extends ConsumerStatefulWidget {
  const BrowseScreen({super.key});
  @override
  ConsumerState<BrowseScreen> createState() => _BrowseScreenState();
}

class _BrowseScreenState extends ConsumerState<BrowseScreen> {
  ContentType _type = ContentType.manga;
  final TextEditingController _sourceFilter = TextEditingController();
  final TextEditingController _globalController = TextEditingController();

  void _openGlobal(String text) {
    final q = text.trim();
    if (q.isEmpty) return;
    FocusScope.of(context).unfocus();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => GlobalSearchScreen(initialQuery: q, type: _type)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final p = ref.watch(paletteProvider);
    final showNsfw = ref.watch(showNsfwProvider);
    final nsfwIds = ref.watch(nsfwSourcesProvider);
    final allOfType = ref.watch(extensionManagerProvider).values.where((s) => s.type == _type).toList();
    final visible = allOfType.where((s) => showNsfw || !nsfwIds.contains(s.id)).toList();
    final filterText = _sourceFilter.text.trim().toLowerCase();
    final sources = visible
        .where((s) =>
            filterText.isEmpty || s.name.toLowerCase().contains(filterText) || s.lang.toLowerCase().contains(filterText))
        .toList();
    final hiddenCount = allOfType.length - visible.length;

    String? emptyMessage;
    if (allOfType.isEmpty) {
      emptyMessage = 'No ${_type.label} sources installed.\nAdd some from the Sources tab.';
    } else if (visible.isEmpty) {
      emptyMessage =
          '$hiddenCount 18+ ${_type.label} source${hiddenCount == 1 ? ' is' : 's are'} hidden.\nTurn them on in Settings → Content.';
    } else if (sources.isEmpty) {
      emptyMessage = 'No sources match "${_sourceFilter.text.trim()}".';
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            child: SegmentedButton<ContentType>(
              segments: ContentType.values
                  .map((t) => ButtonSegment(value: t, label: Text(t.label)))
                  .toList(),
              selected: {_type},
              onSelectionChanged: (s) => setState(() => _type = s.first),
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: _PillField(
              palette: p,
              controller: _globalController,
              hint: 'Search all ${_type.label.toLowerCase()} sources…',
              icon: Icons.travel_explore,
              accented: true,
              onSubmitted: _openGlobal,
              trailing: Material(
                color: p.accent,
                shape: const CircleBorder(),
                child: InkWell(
                  customBorder: const CircleBorder(),
                  onTap: () => _openGlobal(_globalController.text),
                  child: Padding(
                    padding: const EdgeInsets.all(9),
                    child: Icon(Icons.arrow_forward, size: 18, color: p.onAccent),
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: _PillField(
              palette: p,
              controller: _sourceFilter,
              hint: 'Search sources',
              icon: Icons.search,
              onChanged: (_) => setState(() {}),
              onClear: () => setState(() => _sourceFilter.clear()),
            ),
          ),
          Expanded(
            child: emptyMessage != null
                ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(emptyMessage, textAlign: TextAlign.center),
                    ),
                  )
                : ListView.builder(
                    itemCount: sources.length,
                    itemBuilder: (context, i) {
                      final s = sources[i];
                      return ListTile(
                        leading: CircleAvatar(backgroundImage: s.iconUrl.isNotEmpty ? NetworkImage(s.iconUrl) : null),
                        title: Text(s.name),
                        subtitle: Text(nsfwIds.contains(s.id) ? '${s.lang.toUpperCase()} · 18+' : s.lang.toUpperCase()),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => SourceBrowseScreen(sourceId: s.id)),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _sourceFilter.dispose();
    _globalController.dispose();
    super.dispose();
  }
}

class _SourceResult {
  bool loading = true;
  String? error;
  List<Entry> entries = const [];
}

/// Searches every installed source of one content type at once.
class GlobalSearchScreen extends ConsumerStatefulWidget {
  final String initialQuery;
  final ContentType type;
  const GlobalSearchScreen({super.key, required this.initialQuery, required this.type});

  @override
  ConsumerState<GlobalSearchScreen> createState() => _GlobalSearchScreenState();
}

class _GlobalSearchScreenState extends ConsumerState<GlobalSearchScreen> {
  late final TextEditingController _controller;
  final Map<String, _SourceResult> _results = {};
  int _searchId = 0;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialQuery);
    WidgetsBinding.instance.addPostFrameCallback((_) => _search(widget.initialQuery));
  }

  Future<void> _search(String raw) async {
    final q = raw.trim();
    if (q.isEmpty) return;
    final myId = ++_searchId;
    final showNsfw = ref.read(showNsfwProvider);
    final nsfw = ref.read(nsfwSourcesProvider);
    final sources = ref
        .read(extensionManagerProvider)
        .values
        .where((s) => s.type == widget.type && (showNsfw || !nsfw.contains(s.id)))
        .toList();

    setState(() {
      _results
        ..clear()
        ..addEntries(sources.map((s) => MapEntry(s.id, _SourceResult())));
    });

    for (final s in sources) {
      () async {
        try {
          final r = await s.search(q, page: 1, genre: null).timeout(const Duration(seconds: 20));
          if (!mounted || myId != _searchId) return;
          setState(() {
            final res = _results[s.id];
            if (res != null) {
              res.loading = false;
              res.entries = (r as List).cast<Entry>();
            }
          });
        } catch (e) {
          if (!mounted || myId != _searchId) return;
          setState(() {
            final res = _results[s.id];
            if (res != null) {
              res.loading = false;
              res.error = e.toString();
            }
          });
        }
      }();
    }
  }

  Widget _section(AppPalette p, String sourceId, _SourceResult res) {
    final name = ref.read(extensionManagerProvider)[sourceId]?.name ?? sourceId;
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Row(
            children: [
              Expanded(
                child: Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
              ),
              if (res.loading)
                const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
              else if (res.error != null)
                Icon(Icons.error_outline, size: 18, color: scheme.error)
              else
                Text('${res.entries.length}', style: const TextStyle(color: Colors.white54)),
            ],
          ),
        ),
        if (res.error != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
            child: Text(res.error!, maxLines: 3, overflow: TextOverflow.ellipsis, style: TextStyle(color: scheme.error, fontSize: 12)),
          )
        else if (!res.loading && res.entries.isEmpty)
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 0, 16, 4),
            child: Text('No results', style: TextStyle(color: Colors.white38)),
          )
        else if (res.entries.isNotEmpty)
          SizedBox(
            height: 204,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: res.entries.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (context, i) {
                final entry = res.entries[i];
                return InkWell(
                  borderRadius: BorderRadius.circular(10),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => EntryDetailScreen(sourceId: sourceId, entry: entry)),
                  ),
                  child: SizedBox(
                    width: 110,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: SizedBox(
                            width: 110,
                            height: 150,
                            child: entry.coverUrl != null
                                ? CachedNetworkImage(imageUrl: entry.coverUrl!, fit: BoxFit.cover)
                                : Container(color: p.surface),
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text(entry.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 12, color: Colors.white70)),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final p = ref.watch(paletteProvider);
    return Scaffold(
      appBar: AppBar(title: Text('Search all ${widget.type.label}')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: _PillField(
              palette: p,
              controller: _controller,
              hint: 'Search titles…',
              icon: Icons.travel_explore,
              accented: true,
              onSubmitted: _search,
              onClear: () => _controller.clear(),
            ),
          ),
          Expanded(
            child: _results.isEmpty
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.all(24),
                      child: Text('No sources to search.\nInstall some from the Sources tab.', textAlign: TextAlign.center),
                    ),
                  )
                : ListView(
                    padding: const EdgeInsets.only(bottom: 24),
                    children: _results.entries.map((e) => _section(p, e.key, e.value)).toList(),
                  ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
}

enum _ListMode { popular, latest }

class SourceBrowseScreen extends ConsumerStatefulWidget {
  final String sourceId;
  final String? initialQuery;
  const SourceBrowseScreen({super.key, required this.sourceId, this.initialQuery});
  @override
  ConsumerState<SourceBrowseScreen> createState() => _SourceBrowseScreenState();
}

class _SourceBrowseScreenState extends ConsumerState<SourceBrowseScreen> {
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 1;
  List _entries = [];
  String? _error;
  String? _activeQuery; // null = browsing popular/latest, non-null = searching
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  List<Map<String, String>>? _genres;
  String? _selectedGenreId;
  _ListMode _mode = _ListMode.popular;
  int _columns = 3;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    final initial = widget.initialQuery?.trim();
    if (initial != null && initial.isNotEmpty) {
      _activeQuery = initial;
      _searchController.text = initial;
    }
    _resetAndLoad();
  }

  void _onScroll() {
    if (!_scrollController.hasClients || _loadingMore || !_hasMore || _loading) return;
    final threshold = _scrollController.position.maxScrollExtent - 400;
    if (_scrollController.position.pixels >= threshold) {
      _loadMore();
    }
  }

  Future<void> _resetAndLoad() async {
    setState(() {
      _page = 1;
      _hasMore = true;
      _entries = [];
      _loading = true;
      _error = null;
    });
    await _fetchPage(1, append: false);
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    await _fetchPage(_page + 1, append: true);
  }

  Future<void> _fetchPage(int page, {required bool append}) async {
    try {
      final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
      final result = _activeQuery != null
          ? await source.search(_activeQuery!, page: page, genre: _selectedGenreId).timeout(const Duration(seconds: 20))
          : _mode == _ListMode.popular
              ? await source.popular(page: page, genre: _selectedGenreId).timeout(const Duration(seconds: 20))
              : await source.latest(page: page, genre: _selectedGenreId).timeout(const Duration(seconds: 20));

      setState(() {
        if (append) {
          _entries = [..._entries, ...result];
        } else {
          _entries = result;
        }
        _page = page;
        _hasMore = result.length >= 15; // heuristic: a short page means we hit the end
        _loading = false;
        _loadingMore = false;
      });
    } catch (e) {
      setState(() {
        _error = append ? _error : e.toString();
        _loading = false;
        _loadingMore = false;
        if (append) _hasMore = false; // stop retrying on a failed "load more"
      });
    }
  }

  Future<void> _runSearch(String query) async {
    setState(() => _activeQuery = query.trim().isEmpty ? null : query.trim());
    _resetAndLoad();
  }

  Future<void> _openGenreFilter() async {
    if (_genres == null) {
      final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
      final fetched = await source.getGenres();
      setState(() => _genres = fetched);
    }
    if (_genres!.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('This source has no genre filters.')),
      );
      return;
    }
    if (!mounted) return;
    final chosen = await showDialog<String?>(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('Filter by genre'),
        children: [
          SimpleDialogOption(onPressed: () => Navigator.pop(context, null), child: const Text('All')),
          ..._genres!.map((g) => SimpleDialogOption(
                onPressed: () => Navigator.pop(context, g['id']),
                child: Text(g['name'] ?? ''),
              )),
        ],
      ),
    );
    setState(() => _selectedGenreId = chosen);
    _resetAndLoad();
  }

  @override
  Widget build(BuildContext context) {
    final p = ref.watch(paletteProvider);
    final source = ref.watch(extensionManagerProvider)[widget.sourceId]!;

    return Scaffold(
      appBar: AppBar(
        title: Text(source.name),
        actions: [
          IconButton(
            icon: Icon(_columns == 3 ? Icons.grid_view : Icons.grid_3x3),
            tooltip: 'Toggle grid density',
            onPressed: () => setState(() => _columns = _columns == 3 ? 2 : 3),
          ),
          IconButton(
            icon: Icon(_selectedGenreId != null ? Icons.tune : Icons.tune_outlined),
            onPressed: _openGenreFilter,
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 6),
            child: _PillField(
              palette: p,
              controller: _searchController,
              hint: 'Search ${source.name}…',
              icon: Icons.search,
              accented: _activeQuery != null,
              onSubmitted: _runSearch,
              onClear: () {
                _searchController.clear();
                _runSearch('');
              },
            ),
          ),
          if (_activeQuery == null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              child: Row(
                children: [
                  ChoiceChip(
                    label: const Text('Popular'),
                    avatar: const Icon(Icons.local_fire_department, size: 16),
                    selected: _mode == _ListMode.popular,
                    onSelected: (_) {
                      setState(() => _mode = _ListMode.popular);
                      _resetAndLoad();
                    },
                  ),
                  const SizedBox(width: 8),
                  ChoiceChip(
                    label: const Text('Latest'),
                    avatar: const Icon(Icons.bolt, size: 16),
                    selected: _mode == _ListMode.latest,
                    onSelected: (_) {
                      setState(() => _mode = _ListMode.latest);
                      _resetAndLoad();
                    },
                  ),
                ],
              ),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Padding(
                        padding: const EdgeInsets.all(24),
                        child: Center(child: Text('Error loading this source:\n\n$_error', textAlign: TextAlign.center)),
                      )
                    : Stack(
                        children: [
                          EntryGrid(
                            entries: _entries.cast(),
                            emptyLabel: 'No results',
                            columns: _columns,
                            controller: _scrollController,
                            onTap: (entry) => Navigator.push(
                              context,
                              MaterialPageRoute(
                                  builder: (_) => EntryDetailScreen(sourceId: widget.sourceId, entry: entry)),
                            ),
                          ),
                          if (_loadingMore)
                            const Positioned(
                              bottom: 12,
                              left: 0,
                              right: 0,
                              child: Center(child: CircularProgressIndicator()),
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
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }
}
