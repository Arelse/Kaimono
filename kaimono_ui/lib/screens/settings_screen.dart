import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/content_filter.dart';
import '../services/extension_manager.dart';
import '../theme/app_palette.dart';
import 'categories_screen.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});
  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  @override
  Widget build(BuildContext context) {
    // Watching (not just reading) makes this screen rebuild when repos change.
    ref.watch(extensionManagerProvider);
    final manager = ref.read(extensionManagerProvider.notifier);
    final showNsfw = ref.watch(showNsfwProvider);
    final markedCount = ref.watch(nsfwSourcesProvider).length;

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          _sectionHeader('Appearance'),
          const ThemeSettingsTile(),
          const Divider(),
          _sectionHeader('Content'),
          SwitchListTile(
            secondary: const Icon(Icons.eighteen_up_rating_outlined),
            title: const Text('Show 18+ sources'),
            subtitle: Text(markedCount == 0
                ? 'Mark sources as 18+ with the chip in the Sources tab.'
                : '$markedCount source${markedCount == 1 ? '' : 's'} marked 18+. Turn off to hide them from Discover.'),
            value: showNsfw,
            onChanged: (v) => ref.read(showNsfwProvider.notifier).set(v),
          ),
          const Divider(),
          _sectionHeader('Library'),
          ListTile(
            leading: const Icon(Icons.label_outline),
            title: const Text('Categories'),
            subtitle: const Text('Organize your library into custom groups'),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CategoriesScreen())),
          ),
          const Divider(),
          _sectionHeader('Source repositories'),
          ...manager.repos.map((r) {
            final err = manager.repoErrors[r.url];
            return ListTile(
              leading: Icon(err == null ? Icons.link : Icons.error_outline,
                  color: err == null ? null : Theme.of(context).colorScheme.error),
              title: Text(r.url, maxLines: 1, overflow: TextOverflow.ellipsis),
              subtitle: err == null
                  ? null
                  : Text(err, maxLines: 3, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              trailing: IconButton(
                icon: const Icon(Icons.delete_outline),
                onPressed: () => manager.removeRepo(r.url),
              ),
            );
          }),
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
    String? error;
    bool busy = false;

    showDialog(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          Future<void> submit() async {
            setDialogState(() {
              busy = true;
              error = null;
            });
            try {
              final count = await ref.read(extensionManagerProvider.notifier).addRepoChecked(controller.text);
              if (!dialogContext.mounted) return;
              Navigator.pop(dialogContext);
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Repository added — $count source${count == 1 ? '' : 's'} found.')),
                );
              }
            } on ExtensionException catch (e) {
              setDialogState(() {
                busy = false;
                error = e.message;
              });
            } catch (e) {
              setDialogState(() {
                busy = false;
                error = 'Unexpected error: $e';
              });
            }
          }

          return AlertDialog(
            title: const Text('Add extension repo'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: controller,
                  autofocus: true,
                  keyboardType: TextInputType.url,
                  decoration: InputDecoration(
                    hintText: 'https://.../index.json',
                    errorText: error,
                    errorMaxLines: 5,
                  ),
                ),
                if (busy)
                  const Padding(
                    padding: EdgeInsets.only(top: 16),
                    child: LinearProgressIndicator(),
                  ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: busy ? null : () => Navigator.pop(dialogContext),
                child: const Text('Cancel'),
              ),
              if (error != null)
                TextButton(
                  onPressed: () {
                    ref.read(extensionManagerProvider.notifier).addRepo(controller.text);
                    Navigator.pop(dialogContext);
                  },
                  child: const Text('Save anyway'),
                ),
              FilledButton(
                onPressed: busy ? null : submit,
                child: const Text('Add'),
              ),
            ],
          );
        },
      ),
    );
  }
}
