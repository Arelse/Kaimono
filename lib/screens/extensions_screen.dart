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
                _sectionHeader('Available'),
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
                      'No repos reachable right now.\nManage repositories from the Settings tab.',
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
}
