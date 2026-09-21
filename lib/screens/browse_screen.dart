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
  bool _searching = false;
  List _entries = [];
  String? _error;
  final TextEditingController _searchController = TextEditingController();

  List<Map<String, String>>? _genres;
  String? _selectedGenreId;
  _ListMode _mode = _ListMode.popular;
  int _columns = 3;

  @override
  void initState() {
    super.initState();
    _loadList();
  }

  Future<void> _loadList() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
      final result = _mode == _ListMode.popular
          ? await source.popular(genre: _selectedGenreId).timeout(const Duration(seconds: 20))
          : await source.latest(genre: _selectedGenreId).timeout(const Duration(seconds: 20));
      setState(() {
        _entries = result;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  Future<void> _runSearch(String query) async {
    if (query.trim().isEmpty) {
      _loadList();
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
      final result = await source.search(query.trim(), genre: _selectedGenreId).timeout(const Duration(seconds: 20));
      setState(() {
        _entries = result;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
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
    _loadList();
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
                  _loadList();
                }
                _searching = !_searching;
              });
            },
          ),
        ],
        bottom: PreferredSize(
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
                    _loadList();
                  },
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Latest'),
                  avatar: const Icon(Icons.bolt, size: 16),
                  selected: _mode == _ListMode.latest,
                  onSelected: (_) {
                    setState(() => _mode = _ListMode.latest);
                    _loadList();
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
              : EntryGrid(
                  entries: _entries.cast(),
                  emptyLabel: 'No results',
                  columns: _columns,
                  onTap: (entry) => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => EntryDetailScreen(sourceId: widget.sourceId, entry: entry)),
                  ),
                ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}
