import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'screens/library_screen.dart';
import 'screens/browse_screen.dart';
import 'screens/extensions_screen.dart';
import 'screens/updates_screen.dart';
import 'screens/settings_screen.dart';

void main() {
  runApp(const ProviderScope(child: KaimonoApp()));
}

class KaimonoApp extends StatelessWidget {
  const KaimonoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Kaimono',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF7C5CFC),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const RootShell(),
    );
  }
}

class RootShell extends StatefulWidget {
  const RootShell({super.key});
  @override
  State<RootShell> createState() => _RootShellState();
}

class _RootShellState extends State<RootShell> {
  int _index = 0;

  final _screens = const [
    LibraryScreen(),
    UpdatesScreen(),
    BrowseScreen(),
    ExtensionsScreen(),
    SettingsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _screens),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.collections_bookmark_outlined), label: 'Library'),
          NavigationDestination(icon: Icon(Icons.new_releases_outlined), label: 'Updates'),
          NavigationDestination(icon: Icon(Icons.explore_outlined), label: 'Discover'),
          NavigationDestination(icon: Icon(Icons.extension_outlined), label: 'Sources'),
          NavigationDestination(icon: Icon(Icons.settings_outlined), label: 'Settings'),
        ],
      ),
    );
  }
}
