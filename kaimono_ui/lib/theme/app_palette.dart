import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppPalette {
  final String id;
  final String name;
  final Color bg;
  final Color surface;
  final Color statsBg;
  final Color accent;
  final Color secondary;

  const AppPalette({
    required this.id,
    required this.name,
    required this.bg,
    required this.surface,
    required this.statsBg,
    required this.accent,
    required this.secondary,
  });

  Color get onAccent => accent.computeLuminance() > 0.5 ? Colors.black : Colors.white;
  Color get tagBg => Color.alphaBlend(accent.withOpacity(0.12), bg);
  Color get tagFg => Color.lerp(accent, bg, 0.15)!;
  Color get btnBg => Color.alphaBlend(Colors.white.withOpacity(0.07), bg);
  Color get trackerBg => Color.alphaBlend(accent.withOpacity(0.16), bg);
  Color get libraryBg => Color.alphaBlend(secondary.withOpacity(0.18), bg);
  Color get panel => Color.alphaBlend(Colors.white.withOpacity(0.05), bg);
}

const kPalettes = <AppPalette>[
  AppPalette(
    id: 'crimson', name: 'Crimson',
    bg: Color(0xFF0E0909), surface: Color(0xFF1C1315), statsBg: Color(0xFF1A1D24),
    accent: Color(0xFFFF7B7B), secondary: Color(0xFF8B7FF9),
  ),
  AppPalette(
    id: 'graphite', name: 'Graphite',
    bg: Color(0xFF0B0B0C), surface: Color(0xFF161617), statsBg: Color(0xFF1B1D24),
    accent: Color(0xFFE5E7EB), secondary: Color(0xFFC7C9D1),
  ),
  AppPalette(
    id: 'amethyst', name: 'Amethyst',
    bg: Color(0xFF0D0B14), surface: Color(0xFF1A1626), statsBg: Color(0xFF191A2E),
    accent: Color(0xFFA78BFA), secondary: Color(0xFFF0ABFC),
  ),
  AppPalette(
    id: 'ocean', name: 'Ocean',
    bg: Color(0xFF07111A), surface: Color(0xFF0F1F2E), statsBg: Color(0xFF0F2233),
    accent: Color(0xFF4FC3F7), secondary: Color(0xFF818CF8),
  ),
  AppPalette(
    id: 'emerald', name: 'Emerald',
    bg: Color(0xFF08110D), surface: Color(0xFF12211A), statsBg: Color(0xFF10241D),
    accent: Color(0xFF4ADE80), secondary: Color(0xFF2DD4BF),
  ),
  AppPalette(
    id: 'sunset', name: 'Sunset',
    bg: Color(0xFF140A06), surface: Color(0xFF22140C), statsBg: Color(0xFF2A1810),
    accent: Color(0xFFFF9A5C), secondary: Color(0xFFFBBF24),
  ),
  AppPalette(
    id: 'rose', name: 'Rose',
    bg: Color(0xFF130810), surface: Color(0xFF22111C), statsBg: Color(0xFF2B1424),
    accent: Color(0xFFF472B6), secondary: Color(0xFFC084FC),
  ),
  AppPalette(
    id: 'midnight', name: 'Midnight',
    bg: Color(0xFF0F131C), surface: Color(0xFF171C28), statsBg: Color(0xFF1B2233),
    accent: Color(0xFFA5B4FC), secondary: Color(0xFF7DD3FC),
  ),
  AppPalette(
    id: 'gold', name: 'Gold',
    bg: Color(0xFF120F06), surface: Color(0xFF1F1A0D), statsBg: Color(0xFF271F0E),
    accent: Color(0xFFFBBF24), secondary: Color(0xFFFB923C),
  ),
  AppPalette(
    id: 'teal', name: 'Teal',
    bg: Color(0xFF06120F), surface: Color(0xFF0F201C), statsBg: Color(0xFF0E2A26),
    accent: Color(0xFF2DD4BF), secondary: Color(0xFF38BDF8),
  ),
];

class PaletteNotifier extends StateNotifier<AppPalette> {
  PaletteNotifier() : super(kPalettes.first) {
    _restore();
  }

  static const _key = 'kaimono_theme_id';

  Future<void> _restore() async {
    final prefs = await SharedPreferences.getInstance();
    final id = prefs.getString(_key);
    if (id == null) return;
    state = kPalettes.firstWhere((p) => p.id == id, orElse: () => kPalettes.first);
  }

  Future<void> select(AppPalette p) async {
    state = p;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, p.id);
  }
}

final paletteProvider = StateNotifierProvider<PaletteNotifier, AppPalette>((ref) => PaletteNotifier());

void showThemePicker(BuildContext context) {
  showModalBottomSheet(
    context: context,
    backgroundColor: Colors.transparent,
    isScrollControlled: true,
    builder: (_) => const _ThemePickerSheet(),
  );
}

class _ThemePickerSheet extends ConsumerWidget {
  const _ThemePickerSheet();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(paletteProvider);
    return Container(
      decoration: BoxDecoration(
        color: current.panel,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: EdgeInsets.fromLTRB(20, 14, 20, 20 + MediaQuery.of(context).padding.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(color: Colors.white24, borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Theme', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          GridView.count(
            crossAxisCount: 5,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 14,
            crossAxisSpacing: 6,
            childAspectRatio: 0.8,
            children: kPalettes.map((p) {
              final selected = p.id == current.id;
              return InkWell(
                borderRadius: BorderRadius.circular(14),
                onTap: () => ref.read(paletteProvider.notifier).select(p),
                child: Column(
                  children: [
                    Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: p.surface,
                        shape: BoxShape.circle,
                        border: Border.all(color: selected ? Colors.white : Colors.white24, width: selected ? 2 : 1),
                      ),
                      child: Center(
                        child: Container(
                          width: 24,
                          height: 24,
                          decoration: BoxDecoration(color: p.accent, shape: BoxShape.circle),
                          child: selected ? Icon(Icons.check, size: 15, color: p.onAccent) : null,
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(p.name, style: const TextStyle(color: Colors.white70, fontSize: 11)),
                  ],
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}

/// Drop this into your Settings list: `const ThemeSettingsTile()`.
class ThemeSettingsTile extends ConsumerWidget {
  const ThemeSettingsTile({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final p = ref.watch(paletteProvider);
    return ListTile(
      leading: const Icon(Icons.palette_outlined),
      title: const Text('Theme'),
      subtitle: Text(p.name),
      trailing: Container(
        width: 22,
        height: 22,
        decoration: BoxDecoration(color: p.accent, shape: BoxShape.circle),
      ),
      onTap: () => showThemePicker(context),
    );
  }
}
