import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/extension_manager.dart';
import '../widgets/entry_grid.dart';
import 'entry_detail_screen.dart';

/// Lets the user pick an installed source and browse its popular/latest
/// lists or search it — the "discover new titles" half of the app,
/// separate from the saved Library.
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
        title: const Text('Browse'),
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
                  'No ${_type.label} sources installed.\nAdd some from the Extensions tab.',
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

/// Popular/Latest/Search view for a single source.
class SourceBrowseScreen extends ConsumerStatefulWidget {
  final String sourceId;
  const SourceBrowseScreen({super.key, required this.sourceId});
  @override
  ConsumerState<SourceBrowseScreen> createState() => _SourceBrowseScreenState();
}

class _SourceBrowseScreenState extends ConsumerState<SourceBrowseScreen> {
  bool _loading = true;
  List _entries = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final source = ref.read(extensionManagerProvider)[widget.sourceId]!;
    final result = await source.popular();
    setState(() {
      _entries = result;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final source = ref.watch(extensionManagerProvider)[widget.sourceId]!;
    return Scaffold(
      appBar: AppBar(title: Text(source.name)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : EntryGrid(
              entries: _entries.cast(),
              emptyLabel: 'No results',
              onTap: (entry) => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => EntryDetailScreen(sourceId: widget.sourceId, entry: entry),
                ),
              ),
            ),
    );
  }
}
