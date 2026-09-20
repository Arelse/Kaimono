import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/extension_manager.dart';

/// App-wide settings: source repo management for now, appearance/backup
/// later. Its own tab rather than a buried menu, since repo setup is
/// something people reach for early and often.
class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});
  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  @override
  Widget build(BuildContext context) {
    final manager = ref.read(extensionManagerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          _sectionHeader('Source repositories'),
          ...manager.repos.map((r) => ListTile(
                leading: const Icon(Icons.link),
                title: Text(r.url, maxLines: 1, overflow: TextOverflow.ellipsis),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () => setState(() => manager.removeRepo(r.url)),
                ),
              )),
          ListTile(
            leading: const Icon(Icons.add_link),
            title: const Text('Add repository'),
            onTap: _addRepoDialog,
          ),
          const Divider(),
          _sectionHeader('About'),
          const ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Kaimono'),
            subtitle: Text('v0.1.0 — manga, anime & novel reader'),
          ),
        ],
      ),
    );
  }

  Widget _sectionHeader(String text) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
        child: Text(text, style: Theme.of(context).textTheme.titleSmall),
      );

  void _addRepoDialog() {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Add repository'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: 'https://.../index.json'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) {
                setState(() => ref.read(extensionManagerProvider.notifier).addRepo(controller.text.trim()));
              }
              Navigator.pop(context);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
}
