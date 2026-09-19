import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/entry.dart';

class EntryGrid extends StatelessWidget {
  final List<Entry> entries;
  final String emptyLabel;
  final void Function(Entry)? onTap;

  const EntryGrid({super.key, required this.entries, this.emptyLabel = 'Nothing here', this.onTap});

  @override
  Widget build(BuildContext context) {
    if (entries.isEmpty) {
      return Center(
        child: Text(emptyLabel, style: Theme.of(context).textTheme.bodyMedium),
      );
    }
    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 10,
        crossAxisSpacing: 10,
        childAspectRatio: 0.68,
      ),
      itemCount: entries.length,
      itemBuilder: (context, i) {
        final e = entries[i];
        return GestureDetector(
          onTap: () => onTap?.call(e),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (e.coverUrl != null)
                  CachedNetworkImage(imageUrl: e.coverUrl!, fit: BoxFit.cover)
                else
                  Container(color: Theme.of(context).colorScheme.surfaceContainerHighest),
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 0,
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(6, 14, 6, 6),
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [Colors.transparent, Colors.black87],
                      ),
                    ),
                    child: Text(
                      e.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
