import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/extension_manager.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

class BrowseScreen extends ConsumerStatefulWidget {
  const BrowseScreen({super.key});
  @override
  ConsumerState<BrowseScreen> createState() => _BrowseScreenState();
}

class _BrowseScreenState extends ConsumerState<BrowseScreen> {
  ContentType _type = ContentType.manga;

  @override
  Widget build(BuildContext context) {
    final sources = ref.watch(extensionManagerProvider).values.where((s) => s.type == _type).toList();

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
      body: sources.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'No ${_type.label} sources installed.\nAdd some from the Sources tab.',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : ListView.builder(
              itemCount: sources.length,
              itemBuilder: (context, i) {
                final s = sources[i];
                return ListTile(
                  leading: CircleAvatar(backgroundImage: s.iconUrl.isNotEmpty ? NetworkImage(s.iconUrl) : null),
                  title: Text(s.name),
                  subtitle: Text(s.lang.toUpperCase()),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => SourceBrowseScreen(sourceId: s.id)),
                  ),
                );
              },
            ),
    );
  }
}

enum _ListMode { popular, latest }

class SourceBrowseScreen extends ConsumerStatefulWidget {
  final String sourceId;
  const SourceBrowseScreen({super.key, required this.sourceId});
  @override
  ConsumerState<SourceBrowseScreen> createState() => _SourceBrowseScreenState();
}

class _SourceBrowseScreenState extends ConsumerState<SourceBrowseScreen> {
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 1;
  bool _searching = false;
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
    final source = ref.watch(extensionManagerProvider)[widget.sourceId]!;
    return Scaffold(
      appBar: AppBar(
        title: _searching
            ? TextField(
                controller: _searchController,
                autofocus: true,
                decoration: const InputDecoration(hintText: 'Search this source…', border: InputBorder.none),
                style: const TextStyle(fontSize: 16),
                onSubmitted: _runSearch,
              )
            : Text(source.name),
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
          IconButton(
            icon: Icon(_searching ? Icons.close : Icons.search),
            onPressed: () {
              setState(() {
                if (_searching) {
                  _searchController.clear();
                  _activeQuery = null;
                  _resetAndLoad();
                }
                _searching = !_searching;
              });
            },
          ),
        ],
        bottom: _activeQuery != null
            ? null
            : PreferredSize(
                preferredSize: const Size.fromHeight(44),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
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
              ),
      ),
      body: _loading
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
                        MaterialPageRoute(builder: (_) => EntryDetailScreen(sourceId: widget.sourceId, entry: entry)),
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
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }
}
