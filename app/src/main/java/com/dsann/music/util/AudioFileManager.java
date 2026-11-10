package com.dsann.music.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AudioFileManager {

    private static final String TAG = "AudioFileManager";

    public static String getRealPathFromURI(Context context, Uri uri) {
        if (uri == null) return null;

        try {
            String fileName = getFileName(context, uri);
            File tempFile = new File(context.getCacheDir(),
                    "temp_" + System.currentTimeMillis() + "_" + fileName);

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                if (inputStream == null) {
                    Log.e(TAG, "Cannot open input stream from URI");
                    return null;
                }

                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }

            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error extracting file from URI: " + e.getMessage(), e);
            return null;
        }
    }

    public static File createOutputFile(Context context, String fileName) {
        String lowerName = fileName.toLowerCase(java.util.Locale.ROOT);
        if (!(lowerName.endsWith(".m4a") || lowerName.endsWith(".mp3") ||
                lowerName.endsWith(".webm") || lowerName.endsWith(".ogg") ||
                lowerName.endsWith(".opus") || lowerName.endsWith(".aac") ||
                lowerName.endsWith(".flac") || lowerName.endsWith(".wav"))) {
            fileName = fileName + ".m4a";
        }

        String cleanName = fileName.replaceAll("[^a-zA-Z0-9._\\s()\\[\\]-]", "_");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Create temp file in cache for FFmpeg processing
            File tempDir = new File(context.getCacheDir(), "SUMITX_output");
            if (!tempDir.exists()) tempDir.mkdirs();

            File tempFile = new File(tempDir, cleanName);
            Log.d(TAG, "Temp output file for processing: " + tempFile.getAbsolutePath());
            return tempFile;
        } else {
            // Android 9 and below - Direct file access to Downloads/SUMITX
            File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "SUMITX");
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File outputFile = new File(downloadsDir, cleanName);
            Log.d(TAG, "Output file (legacy): " + outputFile.getAbsolutePath());
            return outputFile;
        }
    }

    public static boolean scanFile(Context context, File file) {
        return scanFile(context, file, null, null, null);
    }

    public static boolean scanFile(Context context, File file, String title, String artist, String album) {
        if (file == null || !file.isFile()) {
            Log.e(TAG, "Download file does not exist");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return copyToMediaStore(context, file, title, artist, album);
        }
        try {
            context.sendBroadcast(
                    new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)));
            Log.d(TAG, "Media scan requested: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "scanFile failed: " + e.getMessage(), e);
            return false;
        }
    }

    private static boolean copyToMediaStore(Context context, File sourceFile, String suppliedTitle, String suppliedArtist, String suppliedAlbum) {
        if (!sourceFile.isFile() || sourceFile.length() <= 0L) {
            Log.e(TAG, "Source audio file is missing or empty: " + sourceFile.getAbsolutePath());
            return false;
        }

        String displayName = sourceFile.getName();
        int extensionIndex = displayName.lastIndexOf('.');
        String nameWithoutExtension = extensionIndex > 0
                ? displayName.substring(0, extensionIndex)
                : displayName;

        String title = suppliedTitle != null && !suppliedTitle.trim().isEmpty()
                ? suppliedTitle.trim() : nameWithoutExtension;
        String artist = suppliedArtist != null && !suppliedArtist.trim().isEmpty()
                ? suppliedArtist.trim() : "Unknown Artist";
        if ((suppliedTitle == null || suppliedTitle.trim().isEmpty() ||
                suppliedArtist == null || suppliedArtist.trim().isEmpty()) && nameWithoutExtension.contains(" - ")) {
            String[] parts = nameWithoutExtension.split(" - ", 2);
            if (suppliedTitle == null || suppliedTitle.trim().isEmpty()) title = parts[0].trim();
            if (suppliedArtist == null || suppliedArtist.trim().isEmpty()) artist = parts[1].trim();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.getName());
        values.put(MediaStore.Audio.Media.TITLE, title);
        values.put(MediaStore.Audio.Media.ARTIST, artist);
        values.put(MediaStore.Audio.Media.ALBUM_ARTIST, artist);
        if (suppliedAlbum != null && !suppliedAlbum.trim().isEmpty()) {
            values.put(MediaStore.Audio.Media.ALBUM, suppliedAlbum.trim());
        }

        String lowerFileName = sourceFile.getName().toLowerCase(java.util.Locale.ROOT);
        String mimeType;
        if (lowerFileName.endsWith(".mp3")) mimeType = "audio/mpeg";
        else if (lowerFileName.endsWith(".m4a") || lowerFileName.endsWith(".aac")) mimeType = "audio/mp4";
        else if (lowerFileName.endsWith(".webm")) mimeType = "audio/webm";
        else if (lowerFileName.endsWith(".ogg") || lowerFileName.endsWith(".opus")) mimeType = "audio/ogg";
        else if (lowerFileName.endsWith(".flac")) mimeType = "audio/flac";
        else if (lowerFileName.endsWith(".wav")) mimeType = "audio/wav";
        else mimeType = "application/octet-stream";
        values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SUMITX/");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri itemUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (itemUri == null) {
            Log.e(TAG, "Failed to create MediaStore entry for " + sourceFile.getName());
            return false;
        }

        try {
            long copiedBytes = 0L;
            try (InputStream in = new java.io.FileInputStream(sourceFile);
                 OutputStream out = resolver.openOutputStream(itemUri)) {
                if (out == null) throw new IOException("MediaStore returned no output stream");
                byte[] buffer = new byte[64 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    copiedBytes += bytesRead;
                }
                out.flush();
            }

            long sourceBytes = sourceFile.length();
            if (copiedBytes != sourceBytes) {
                throw new IOException("MediaStore copy was incomplete: " + copiedBytes + " of " + sourceBytes + " bytes");
            }

            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Audio.Media.IS_PENDING, 0);
            if (resolver.update(itemUri, ready, null, null) <= 0) {
                throw new IOException("Could not publish the completed audio file");
            }
            Log.d(TAG, "File copied to MediaStore successfully: Music/SUMITX/" + sourceFile.getName());
            return true;
        } catch (Exception e) {
            try {
                resolver.delete(itemUri, null, null);
            } catch (Exception cleanupError) {
                Log.w(TAG, "Could not remove incomplete MediaStore entry", cleanupError);
            }
            Log.e(TAG, "Error copying to MediaStore: " + e.getMessage(), e);
            return false;
        }
    }

    private static String getFileName(Context context, Uri uri) {
        String name = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename: " + e.getMessage());
            }
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        return name != null ? name : "audio.m4a";
    }
}
