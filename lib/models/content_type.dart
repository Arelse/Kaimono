/// The three content domains the app supports. Kept as a first-class
/// enum (rather than separate apps) so the Source, Library, and Browse
/// layers can all stay generic instead of forking into three codebases.
enum ContentType { manga, anime, novel }

extension ContentTypeLabel on ContentType {
  String get label => switch (this) {
        ContentType.manga => 'Manga',
        ContentType.anime => 'Anime',
        ContentType.novel => 'Novel',
      };
}
