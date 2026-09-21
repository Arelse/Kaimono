import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/entry.dart';
import '../services/library_manager.dart';

class EntryGrid extends ConsumerWidget {
  final List<Entry> entries;
  final String emptyLabel;
  final void Function(Entry)? onTap;
  final int columns;

  const EntryGrid({
    super.key,
    required this.entries,
    this.emptyLabel = 'Nothing here',
    this.onTap,
    this.columns = 3,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (entries.isEmpty) {
      return Center(
        child: Text(emptyLabel, style: Theme.of(context).textTheme.bodyMedium, textAlign: TextAlign.center),
      );
    }
    final library = ref.watch(libraryManagerProvider.notifier);
    ref.watch(libraryManagerProvider); // rebuild when favorites change

    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: columns,
        mainAxisSpacing: 14,
        crossAxisSpacing: 10,
        childAspectRatio: 0.58,
      ),
      itemCount: entries.length,
      itemBuilder: (context, i) {
        final e = entries[i];
        final saved = library.isFavorite(e.sourceId, e.id);
        return GestureDetector(
          onTap: () => onTap?.call(e),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      if (e.coverUrl != null && e.coverUrl!.isNotEmpty)
                        CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover)
                      else
                        Container(color: Theme.of(context).colorScheme.surfaceContainerHighest),
                      if (saved)
                        Positioned(
                          top: 6,
                          left: 6,
                          child: Container(
                            padding: const EdgeInsets.all(4),
                            decoration: BoxDecoration(
                              color: Theme.of(context).colorScheme.primary,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.star, size: 12, color: Colors.white),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                e.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        );
      },
    );
  }
}
