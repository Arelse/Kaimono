import 'package:flutter/services.dart';

class KeiyoshiBridge {
  static const MethodChannel _channel = MethodChannel('com.kaimono/keiyoshi');

  /// Fetches popular manga/entries from the native APK source
  static Future<List<Map<String, dynamic>>> fetchPopular({
    required String apkPath,
    required String className,
    int page = 1,
  }) async {
    try {
      final result = await _channel.invokeMethod('fetchPopular', {
        'apkPath': apkPath,
        'className': className,
        'page': page,
      });
      return List<Map<String, dynamic>>.from(result ?? []);
    } catch (e) {
      throw Exception('Bridge error in fetchPopular: $e');
    }
  }

  /// Fetches the chapter list (chunks) for a specific entry/manga
  static Future<List<Map<String, dynamic>>> fetchChapters({
    required String apkPath,
    required String className,
    required String entryUrl,
  }) async {
    try {
      final result = await _channel.invokeMethod('fetchChapters', {
        'apkPath': apkPath,
        'className': className,
        'entryUrl': entryUrl,
      });
      return List<Map<String, dynamic>>.from(result ?? []);
    } catch (e) {
      throw Exception('Bridge error in fetchChapters: $e');
    }
  }

  /// Fetches the image page URLs for a specific chapter
  static Future<List<String>> fetchPages({
    required String apkPath,
    required String className,
    required String chapterUrl,
  }) async {
    try {
      final result = await _channel.invokeMethod('fetchPages', {
        'apkPath': apkPath,
        'className': className,
        'chapterUrl': chapterUrl,
      });
      return List<String>.from(result ?? []);
    } catch (e) {
      throw Exception('Bridge error in fetchPages: $e');
    }
  }
}
