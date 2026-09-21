import 'content_type.dart';

/// A single title (a manga series, an anime, or a novel) as returned by
/// a source. This is intentionally generic — chapters, episodes, and
/// novel parts are all modeled as [EntryChunk] so library/history/reader
/// UI can stay shared across the three content types.
class Entry {
  final String id; // source-scoped unique id (usually the source URL slug)
  final String sourceId;
  final ContentType type;
  final String title;
  final String? coverUrl;
  final String? description;
  final List<String> genres;
  final String? author;
  final EntryStatus status;

  Entry({
    required this.id,
    required this.sourceId,
    required this.type,
    required this.title,
    this.coverUrl,
    this.description,
    this.genres = const [],
    this.author,
    this.status = EntryStatus.unknown,
  });

  factory Entry.fromJson(Map<String, dynamic> json, String sourceId, ContentType type) {
    return Entry(
      id: json['id']?.toString() ?? '',
      sourceId: sourceId,
      type: type,
      title: json['title']?.toString() ?? 'Untitled',
      coverUrl: json['cover']?.toString(),
      description: json['description']?.toString(),
      genres: (json['genres'] as List?)?.map((g) => g.toString()).toList() ?? const [],
      author: json['author']?.toString(),
      status: EntryStatus.values.firstWhere(
        (s) => s.name == (json['status']?.toString() ?? 'unknown'),
        orElse: () => EntryStatus.unknown,
      ),
    );
  }
}

enum EntryStatus { ongoing, completed, hiatus, cancelled, unknown }

/// A chapter (manga/novel) or episode (anime).
class EntryChunk {
  final String id;
  final String entryId;
  final String title;
  final double number;
  final DateTime? uploadDate;
  final bool read;

  EntryChunk({
    required this.id,
    required this.entryId,
    required this.title,
    required this.number,
    this.uploadDate,
    this.read = false,
  });
}
