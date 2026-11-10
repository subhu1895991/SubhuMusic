package com.dsann.music.update;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GitHubReleaseClient {

    private static final String TAG = "GitHubReleaseClient";
    private static final String[] APK_ASSET_NAMES = {
            "SUMITX-Android.apk",
            "SUMITX.apk",
            "app-release.apk"
    };

    private final String owner;
    private final String repo;

    public GitHubReleaseClient(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
    }

    public ReleaseInfo getLatestRelease() {
        HttpURLConnection connection = null;
        try {
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
            connection = openConnection(apiUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.i(TAG, "No published GitHub release exists yet.");
                return null;
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "GitHub API returned code: " + responseCode);
                return null;
            }

            return parseRelease(new JSONObject(readResponse(connection.getInputStream())));
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch latest release", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public List<ReleaseInfo> getAllReleases() {
        List<ReleaseInfo> releases = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases?per_page=30";
            connection = openConnection(apiUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "GitHub releases API returned code: " + responseCode);
                return releases;
            }

            JSONArray jsonArray = new JSONArray(readResponse(connection.getInputStream()));
            for (int i = 0; i < jsonArray.length(); i++) {
                ReleaseInfo release = parseRelease(jsonArray.getJSONObject(i));
                if (release != null) releases.add(release);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch all releases", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return releases;
    }

    private HttpURLConnection openConnection(String apiUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "SUMITX-UpdateChecker");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        return connection;
    }

    private String readResponse(InputStream inputStream) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        return response.toString();
    }

    private ReleaseInfo parseRelease(JSONObject json) throws Exception {
        String tagName = json.optString("tag_name", "").trim();
        if (tagName.isEmpty()) return null;

        String body = json.optString("body", "");
        JSONArray assets = json.optJSONArray("assets");
        String apkUrl = null;
        String checksumUrl = null;
        String apkAssetName = null;
        long apkSize = 0L;

        if (assets != null) {
            // Prefer the canonical phone APK name, then accept older known names.
            for (String preferredName : APK_ASSET_NAMES) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "");
                    if (preferredName.equalsIgnoreCase(name)) {
                        apkUrl = asset.optString("browser_download_url", null);
                        apkAssetName = name;
                        apkSize = asset.optLong("size", 0L);
                        break;
                    }
                }
                if (apkUrl != null && !apkUrl.isEmpty()) break;
            }

            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name", "");
                String lowerName = name.toLowerCase(Locale.ROOT);
                if (lowerName.equals("sumitx-android.apk.sha256")
                        || lowerName.equals("sumitx-android.apk.sha256sum")
                        || lowerName.equals("checksum.txt")) {
                    checksumUrl = asset.optString("browser_download_url", null);
                    break;
                }
            }
        }

        return new ReleaseInfo(tagName, body, apkUrl, checksumUrl, apkAssetName, apkSize);
    }

    public static class ReleaseInfo {
        public final String tagName;
        public final String changelog;
        public final String apkUrl;
        public final String checksumUrl;
        public final String apkAssetName;
        public final long apkSize;

        public ReleaseInfo(String tagName, String changelog, String apkUrl, String checksumUrl) {
            this(tagName, changelog, apkUrl, checksumUrl, null, 0L);
        }

        public ReleaseInfo(String tagName, String changelog, String apkUrl, String checksumUrl,
                           String apkAssetName, long apkSize) {
            this.tagName = tagName;
            this.changelog = changelog;
            this.apkUrl = apkUrl;
            this.checksumUrl = checksumUrl;
            this.apkAssetName = apkAssetName;
            this.apkSize = apkSize;
        }

        public boolean hasApk() {
            return apkUrl != null && !apkUrl.trim().isEmpty();
        }
    }
}
