import 'content_type.dart';
import 'entry.dart';

/// Contract every installed extension satisfies, regardless of whether it's
/// backed by a JS module, a bundled scraper, or a wrapped API client.
abstract class Source {
  String get id;
  String get name;
  String get lang;
  ContentType get type;
  String get iconUrl;
  int get version;

  Future<List<Entry>> search(String query, {int page = 1, String? genre});
  Future<List<Entry>> popular({int page = 1, String? genre});
  Future<List<Entry>> latest({int page = 1, String? genre});
  Future<Entry> getEntryDetails(String entryId);
  Future<List<EntryChunk>> getChunks(String entryId);

  /// Manga: ordered image URLs for a chapter.
  /// Novel: ordered text blocks for a chapter.
  /// Anime: not used (see [getStreamLinks]).
  Future<List<String>> getPages(String chunkId);

  /// Anime only: playable stream URLs/qualities for an episode.
  Future<List<StreamLink>> getStreamLinks(String chunkId);

  /// Genre/tag list for filtering, as [{id, name}]. Not every source
  /// supports this — default is an empty list, meaning "no filter UI."
  Future<List<Map<String, String>>> getGenres() async => [];
}

class StreamLink {
  final String url;
  final String quality;
  final Map<String, String> headers;

  StreamLink({required this.url, required this.quality, this.headers = const {}});
}
