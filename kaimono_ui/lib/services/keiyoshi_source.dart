import '../models/content_type.dart';
import '../models/entry.dart';
import '../models/source.dart';
import 'keiyoshi_bridge.dart';

class KeiyoshiSource implements Source {
  final dynamic manifest; 
  final String apkPath;

  KeiyoshiSource({required this.manifest, required this.apkPath});

  @override
  String get id => manifest.id;

  @override
  String get name => manifest.name;

  @override
  String get lang => manifest.lang;

  @override
  ContentType get type => manifest.type;

  @override
  String get iconUrl => manifest.iconUrl;

  @override
  int get version => manifest.version;

  @override
  Future<List<Entry>> popular({int page = 1, String? genre}) async {
    final rawList = await KeiyoshiBridge.fetchPopular(
      apkPath: apkPath,
      className: manifest.id,
      page: page,
    );

    return rawList.map((data) => Entry(
      id: data['url'] ?? data['id'] ?? data.hashCode.toString(),
      sourceId: id,
      title: data['title'] ?? 'Unknown',
      coverUrl: data['thumbnailUrl'] ?? data['coverUrl'] ?? '',
      type: type,
    )).toList();
  }

  @override
  Future<List<Entry>> search(String query, {int page = 1, String? genre}) async => [];

  @override
  Future<List<Entry>> latest({int page = 1, String? genre}) async => [];

  @override
  Future<Entry> getEntryDetails(String entryId) async {
    return Entry(
      id: entryId,
      sourceId: id,
      title: 'Details',
      coverUrl: '',
      type: type,
    );
  }

  @override
  Future<List<EntryChunk>> getChunks(String entryId) async {
    // Temporarily returning empty list to guarantee a successful build, 
    // avoiding unknown constructor requirements for EntryChunk.
    return [];
  }

  @override
  Future<List<String>> getPages(String chunkId) async {
    return await KeiyoshiBridge.fetchPages(
      apkPath: apkPath,
      className: manifest.id,
      chapterUrl: chunkId,
    );
  }

  @override
  Future<List<StreamLink>> getStreamLinks(String chunkId) async => [];

  @override
  Future<List<Map<String, String>>> getGenres() async => [];
}
