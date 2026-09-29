import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/content_type.dart';
import '../services/extension_manager.dart';
import '../services/js_source.dart';
import '../services/keiyoshi_bridge.dart';

class ExtensionsScreen extends ConsumerStatefulWidget {
  const ExtensionsScreen({super.key});
  @override
  ConsumerState<ExtensionsScreen> createState() => _ExtensionsScreenState();
}

class _ExtensionsScreenState extends ConsumerState<ExtensionsScreen> {
  List<ExtensionManifest> _available = [];
  bool _loading = true;
  String _repoSig = '';

  @override
  void initState() {
    super.initState();
    _repoSig = _currentRepoSig();
    _refresh();
  }

  String _currentRepoSig() =>
      ref.read(extensionManagerProvider.notifier).repos.map((r) => r.url).join('|');

  Future<void> _refresh() async {
    setState(() => _loading = true);
    final manager = ref.read(extensionManagerProvider.notifier);
    final list = await manager.browseAll();
    if (!mounted) return;
    setState(() {
      _available = list;
      _loading = false;
    });
  }

  Future<void> testExtensionBridge() async {
    try {
      print("Testing Keiyoshi Bridge...");
      // Sends a fake APK path to the native side to see if they can talk
      final result = await KeiyoshiBridge.fetchPopular(
        apkPath: '/storage/emulated/0/Download/test_extension.apk', 
        className: 'eu.kanade.tachiyomi.extension.en.test.TestExtension',
      );
      print("Success: $result");
    } catch (e) {
      print("Bridge Connected, but failed to load extension: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    final installed = ref.watch(extensionManagerProvider);

    // When the list of repositories changes (added/removed in Settings), reload.
    ref.listen(extensionManagerProvider, (prev, next) {
      final sig = _currentRepoSig();
      if (sig != _repoSig) {
        _repoSig = sig;
        _refresh();
      }
    });

    final errors = ref.read(extensionManagerProvider.notifier).repoErrors;
    final scheme = Theme.of(context).colorScheme;
    final availableToInstall = _available.where((m) => !installed.containsKey(m.id)).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Sources'),
        actions: [
          IconButton(icon: const Icon(Icons.add), tooltip: 'Add source by URL', onPressed: _addManualSource),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _refresh),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: testExtensionBridge,
        child: const Icon(Icons.bug_report),
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
                if (errors.isNotEmpty) _sectionHeader('Repositories with problems'),
                ...errors.entries.map((e) => Card(
                      color: scheme.errorContainer,
                      margin: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                      child: ListTile(
                        leading: Icon(Icons.error_outline, color: scheme.onErrorContainer),
                        title: Text('Could not load repository',
                            style: TextStyle(color: scheme.onErrorContainer, fontWeight: FontWeight.w600)),
                        subtitle: Text('${e.key}\n${e.value}', style: TextStyle(color: scheme.onErrorContainer)),
                        isThreeLine: true,
                      ),
                    )),
                _sectionHeader('Available from repos'),
                ...availableToInstall.map((m) => ListTile(
                      leading: CircleAvatar(backgroundImage: m.iconUrl.isNotEmpty ? NetworkImage(m.iconUrl) : null),
                      title: Text(m.name),
                      subtitle: Text('${m.lang.toUpperCase()} · ${m.type.label} · v${m.version}'),
                      trailing: FilledButton(
                        // UPDATED: Async try-catch to properly handle and display installation errors
                        onPressed: () async {
                          try {
                            // Show a temporary snackbar so the user knows the download started
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text('Downloading ${m.name}...'), duration: const Duration(seconds: 1)),
                            );
                            
                            await ref.read(extensionManagerProvider.notifier).install(m);
                            
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text('Installed ${m.name}')),
                              );
                            }
                          } catch (e) {
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(e.toString()), 
                                  backgroundColor: scheme.error,
                                ),
                              );
                            }
                          }
                        },
                        child: const Text('Install'),
                      ),
                    )),
                if (_available.isEmpty && errors.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'No sources found in your repositories yet.\nAdd a repository in Settings, or tap + above to add a single source by URL.',
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

  String _nameFromUrl(String url) {
    final segs = Uri.parse(url).pathSegments.where((s) => s.isNotEmpty).toList();
    var name = segs.isEmpty ? 'Source' : segs.last;
    name = name.replaceAll(RegExp(r'\.(js|json)$', caseSensitive: false), '');
    name = name.replaceAll(RegExp(r'[-_]+'), ' ').trim();
    return name.isEmpty ? 'Source' : name;
  }

  void _addManualSource() {
    final nameController = TextEditingController();
    final urlController = TextEditingController();
    ContentType selectedType = ContentType.manga;
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
            final manager = ref.read(extensionManagerProvider.notifier);
            try {
              final url = normalizeSourceUrl(urlController.text);
              final body = await fetchText(url);
              final head = body.trimLeft();
              String message;

              if (head.startsWith('{') || head.startsWith('[')) {
                final manifests = parseManifests(body, url);
                for (final m in manifests) {
                  await manager.install(m);
                }
                message = 'Installed ${manifests.map((m) => m.name).join(', ')}';
              } else if (head.startsWith('<')) {
                throw ExtensionException(
                    'That URL returned a web page, not a script. On GitHub, open the file, tap "Raw", and copy that link.');
              } else {
                final name = nameController.text.trim().isEmpty ? _nameFromUrl(url) : nameController.text.trim();
                await manager.install(ExtensionManifest(
                  id: 'manual.${DateTime.now().millisecondsSinceEpoch}',
                  name: name,
                  lang: 'en',
                  type: selectedType,
                  iconUrl: '',
                  scriptUrl: url,
                  version: 1,
                ));
                message = 'Installed $name';
              }

              if (!dialogContext.mounted) return;
              Navigator.pop(dialogContext);
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
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
            title: const Text('Add source by URL'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: urlController,
                    autofocus: true,
                    keyboardType: TextInputType.url,
                    decoration: InputDecoration(
                      labelText: 'Script or source URL',
                      hintText: 'https://.../source.js',
                      errorText: error,
                      errorMaxLines: 5,
                    ),
                  ),
                  TextField(
                    controller: nameController,
                    decoration: const InputDecoration(labelText: 'Source name (optional)'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<ContentType>(
                    initialValue: selectedType,
                    decoration: const InputDecoration(labelText: 'Content type (for scripts)'),
                    items: ContentType.values
                        .map((t) => DropdownMenuItem(value: t, child: Text(t.label)))
                        .toList(),
                    onChanged: (v) => setDialogState(() => selectedType = v!),
                  ),
                  if (busy)
                    const Padding(
                      padding: EdgeInsets.only(top: 16),
                      child: LinearProgressIndicator(),
                    ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: busy ? null : () => Navigator.pop(dialogContext),
                child: const Text('Cancel'),
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
