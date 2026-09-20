import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/extension_manager.dart';
import '../services/js_source.dart';

class ExtensionsScreen extends ConsumerStatefulWidget {
  const ExtensionsScreen({super.key});
  @override
  ConsumerState<ExtensionsScreen> createState() => _ExtensionsScreenState();
}

class _ExtensionsScreenState extends ConsumerState<ExtensionsScreen> {
  List<ExtensionManifest> _available = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    setState(() => _loading = true);
    final manager = ref.read(extensionManagerProvider.notifier);
    final list = await manager.browseAll();
    setState(() {
      _available = list;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final installed = ref.watch(extensionManagerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Sources'),
        actions: [
          IconButton(icon: const Icon(Icons.add), tooltip: 'Add source by URL', onPressed: _addManualSource),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _refresh),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              children: [
                if (installed.isNotEmpty) _sectionHeader('Installed'),
                ...installed.values.map((s) => ListTile(
                      leading: const Icon(Icons.check_circle, color: Colors.green),
                      title: Text(s.name),
                      subtitle: Text('${s.lang.toUpperCase()} · ${s.type.label} · v${s.version}'),
                      trailing: TextButton(
                        onPressed: () => ref.read(extensionManagerProvider.notifier).uninstall(s.id),
                        child: const Text('Remove'),
                      ),
                    )),
                _sectionHeader('Available from repos'),
                ..._available.where((m) => !installed.containsKey(m.id)).map((m) => ListTile(
                      leading: CircleAvatar(backgroundImage: m.iconUrl.isNotEmpty ? NetworkImage(m.iconUrl) : null),
                      title: Text(m.name),
                      subtitle: Text('${m.lang.toUpperCase()} · ${m.type.label} · v${m.version}'),
                      trailing: FilledButton(
                        onPressed: () => ref.read(extensionManagerProvider.notifier).install(m),
                        child: const Text('Install'),
                      ),
                    )),
                if (_available.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'No repos reachable right now.\nManage repositories from Settings, or tap + above to add a single source by URL.',
                      textAlign: TextAlign.center,
                    ),
                  ),
              ],
            ),
    );
  }

  Widget _sectionHeader(String text) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
        child: Text(text, style: Theme.of(context).textTheme.titleSmall),
      );

  /// Installs a single JS source directly from its script URL, without
  /// needing a repo index. The extension becomes usable in Discover the
  /// moment this dialog closes — no separate "refresh" step needed.
  void _addManualSource() {
    final nameController = TextEditingController();
    final urlController = TextEditingController();
    ContentType selectedType = ContentType.manga;
    String lang = 'en';

    showDialog(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          title: const Text('Add source by URL'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(labelText: 'Source name'),
                ),
                TextField(
                  controller: urlController,
                  decoration: const InputDecoration(labelText: 'Script URL (.js)'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<ContentType>(
                  initialValue: selectedType,
                  decoration: const InputDecoration(labelText: 'Content type'),
                  items: ContentType.values
                      .map((t) => DropdownMenuItem(value: t, child: Text(t.label)))
                      .toList(),
                  onChanged: (v) => setDialogState(() => selectedType = v!),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
            FilledButton(
              onPressed: () {
                if (nameController.text.trim().isEmpty || urlController.text.trim().isEmpty) return;
                final manifest = ExtensionManifest(
                  id: 'manual.${DateTime.now().millisecondsSinceEpoch}',
                  name: nameController.text.trim(),
                  lang: lang,
                  type: selectedType,
                  iconUrl: '',
                  scriptUrl: urlController.text.trim(),
                  version: 1,
                );
                ref.read(extensionManagerProvider.notifier).install(manifest);
                Navigator.pop(dialogContext);
              },
              child: const Text('Add'),
            ),
          ],
        ),
      ),
    );
  }
}
