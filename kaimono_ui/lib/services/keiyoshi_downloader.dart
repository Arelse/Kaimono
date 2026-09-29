import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';

class KeiyoshiDownloader {
  static final Dio _dio = Dio();

  static Future<String> downloadApk({
    required String downloadUrl,
    required String extensionId,
  }) async {
    try {
      final appDir = await getApplicationSupportDirectory();
      final extDir = Directory('${appDir.path}/keiyoshi_extensions');
      if (!await extDir.exists()) {
        await extDir.create(recursive: true);
      }

      final savePath = '${extDir.path}/$extensionId.apk';
      await _dio.download(downloadUrl, savePath);

      // Android blocks DexClassLoader if the APK file is writable.
      // Setting file permissions to read-only (chmod 444) bypasses this security restriction.
      if (Platform.isAndroid) {
        await Process.run('chmod', ['444', savePath]);
      }

      return savePath;
    } catch (e) {
      throw Exception("Failed to download Keiyoshi APK: $e");
    }
  }
}
