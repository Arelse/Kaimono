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
      id: json['id'] as String,
      sourceId: sourceId,
      type: type,
      title: json['title'] as String,
      coverUrl: json['cover'] as String?,
      description: json['description'] as String?,
      genres: (json['genres'] as List?)?.cast<String>() ?? const [],
      author: json['author'] as String?,
      status: EntryStatus.values.firstWhere(
        (s) => s.name == (json['status'] ?? 'unknown'),
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
