import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/extension_manager.dart';
import 'categories_screen.dart';

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
          _sectionHeader('Library'),
          ListTile(
            leading: const Icon(Icons.label_outline),
            title: const Text('Categories'),
            subtitle: const Text('Organize your library into custom groups'),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CategoriesScreen())),
          ),
          const Divider(),
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
        title: const Text('Add extension repo'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: 'https://.../index.json'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              ref.read(extensionManagerProvider.notifier).addRepo(controller.text.trim());
              Navigator.pop(context);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
}
