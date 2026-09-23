import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/category_manager.dart';

class CategoriesScreen extends ConsumerWidget {
  const CategoriesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(categoryManagerProvider);
    final manager = ref.read(categoryManagerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Categories')),
      body: state.categories.isEmpty
          ? const Center(child: Text('No categories yet.\nTap + to create one.', textAlign: TextAlign.center))
          : ReorderableListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: state.categories.length,
              onReorder: (oldIndex, newIndex) => manager.reorder(oldIndex, newIndex),
              itemBuilder: (context, i) {
                final cat = state.categories[i];
                return Card(
                  key: ValueKey(cat.id),
                  margin: const EdgeInsets.symmetric(vertical: 4),
                  child: ListTile(
                    leading: const Icon(Icons.drag_indicator),
                    title: Text(cat.name),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.edit_outlined),
                          onPressed: () => _renameDialog(context, manager, cat.id, cat.name),
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () => _deleteConfirm(context, manager, cat.id, cat.name),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _addDialog(context, manager),
        icon: const Icon(Icons.add),
        label: const Text('Add'),
      ),
    );
  }

  void _addDialog(BuildContext context, CategoryManager manager) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('New category'),
        content: TextField(controller: controller, decoration: const InputDecoration(hintText: 'Name')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) manager.addCategory(controller.text.trim());
              Navigator.pop(context);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }

  void _renameDialog(BuildContext context, CategoryManager manager, String id, String currentName) {
    final controller = TextEditingController(text: currentName);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Rename category'),
        content: TextField(controller: controller),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) manager.renameCategory(id, controller.text.trim());
              Navigator.pop(context);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  void _deleteConfirm(BuildContext context, CategoryManager manager, String id, String name) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Delete category?'),
        content: Text('"$name" will be removed. Entries stay in your library.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () {
              manager.deleteCategory(id);
              Navigator.pop(context);
            },
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}
