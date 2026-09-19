import 'package:flutter/material.dart';

/// Feed of new chapters/episodes/parts across everything in the library,
/// regardless of content type. Populated by a background refresh job that
/// diffs each library entry's `getChunks()` against last-seen state.
/// TODO: wire to Isar + a periodic WorkManager/background_fetch job.
class UpdatesScreen extends StatelessWidget {
  const UpdatesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Updates')),
      body: const Center(child: Text('Nothing new yet — add titles to your library first.')),
    );
  }
}
