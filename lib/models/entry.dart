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
  final String? artist;
  final EntryStatus status;
  final String? sourceUrl; // the entry's page on the source's own website, if the source provides one
  final String? rank; // optional, e.g. "21" or "#21"
  final double? rating; // optional, e.g. 9.81
  final String? saves; // optional, e.g. "76.4K"

  Entry({
    required this.id,
    required this.sourceId,
    required this.type,
    required this.title,
    this.coverUrl,
    this.description,
    this.genres = const [],
    this.author,
    this.artist,
    this.status = EntryStatus.unknown,
    this.sourceUrl,
    this.rank,
    this.rating,
    this.saves,
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
      artist: json['artist']?.toString(),
      status: EntryStatus.values.firstWhere(
        (s) => s.name == (json['status']?.toString() ?? 'unknown'),
        orElse: () => EntryStatus.unknown,
      ),
      sourceUrl: json['url']?.toString(),
      rank: json['rank']?.toString(),
      rating: double.tryParse(json['rating']?.toString() ?? ''),
      saves: json['saves']?.toString(),
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
