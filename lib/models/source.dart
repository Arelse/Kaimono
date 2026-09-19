import 'content_type.dart';
import 'entry.dart';

/// Contract every installed extension satisfies, regardless of whether it's
/// backed by a JS module, a bundled scraper, or a wrapped API client.
///
/// This mirrors the shape Mihon/Mangayomi sources expose (search, entry
/// details, chunk list, page/stream list) so porting source *logic* from
/// those ecosystems into a JS module here is a translation job, not a
/// redesign. We do not claim binary compatibility with .apk (Mihon) or
/// .mmrpm (Mangayomi) extension packages — see EXTENSIONS.md.
abstract class Source {
  String get id;
  String get name;
  String get lang;
  ContentType get type;
  String get iconUrl;
  int get version;

  Future<List<Entry>> search(String query, {int page = 1});
  Future<List<Entry>> popular({int page = 1});
  Future<List<Entry>> latest({int page = 1});
  Future<Entry> getEntryDetails(String entryId);
  Future<List<EntryChunk>> getChunks(String entryId);

  /// Manga: ordered image URLs for a chapter.
  /// Novel: ordered text blocks for a chapter.
  /// Anime: not used (see [getStreamLinks]).
  Future<List<String>> getPages(String chunkId);

  /// Anime only: playable stream URLs/qualities for an episode.
  Future<List<StreamLink>> getStreamLinks(String chunkId);
}

class StreamLink {
  final String url;
  final String quality;
  final Map<String, String> headers;

  StreamLink({required this.url, required this.quality, this.headers = const {}});
}
