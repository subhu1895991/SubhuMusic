package com.dsann.music.update;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String GITHUB_OWNER = "subhu1895991";
    private static final String GITHUB_REPO = "SubHuMusic";
    private static final String PREFS_NAME = "update_prefs";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final String KEY_DOWNLOAD_VERSION = "download_version";

    private final Context context;
    private final GitHubReleaseClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = new GitHubReleaseClient(GITHUB_OWNER, GITHUB_REPO);
    }

    /** Checks the repository's latest stable GitHub Release and offers its APK. */
    public void checkForUpdate(View rootView, String currentVersion) {
        showStatus("Checking GitHub for the latest SUMITX update...");

        new Thread(() -> {
            GitHubReleaseClient.ReleaseInfo release = client.getLatestRelease();

            if (release == null) {
                runOnUi(() -> showStatus(
                        "No published SUMITX APK release was found. Check again after a release is published."
                ));
                return;
            }

            if (!release.hasApk()) {
                runOnUi(() -> showStatus(
                        "The latest GitHub release does not contain a SUMITX APK yet."
                ));
                return;
            }

            boolean isNewer = VersionUtils.isNewer(release.tagName, currentVersion);
            runOnUi(() -> {
                if (isNewer) {
                    promptUpdate(rootView, release);
                } else {
                    showStatus("You're already using the latest SUMITX version.");
                }
            });
        }, "SUMITX-UpdateCheck").start();
    }

    private void promptUpdate(View rootView, GitHubReleaseClient.ReleaseInfo release) {
        StringBuilder message = new StringBuilder();
        message.append("Version ").append(release.tagName).append(" is available.");

        if (release.apkSize > 0L) {
            double sizeMb = release.apkSize / (1024.0 * 1024.0);
            message.append(String.format(Locale.getDefault(), "\nAPK size: %.1f MB", sizeMb));
        }

        if (release.changelog != null && !release.changelog.trim().isEmpty()) {
            String changelog = release.changelog.length() > 600
                    ? release.changelog.substring(0, 600) + "..."
                    : release.changelog;
            message.append("\n\nWhat's new:\n").append(changelog);
        }

        new MaterialAlertDialogBuilder(rootView.getContext())
                .setTitle("SUMITX update available")
                .setMessage(message.toString())
                .setPositiveButton("Download & Install", (dialog, which) ->
                        downloadAndInstall(rootView, release))
                .setNegativeButton("Later", null)
                .show();
    }

    /** Starts the actual download for a release already confirmed by the user. */
    public void downloadAndInstall(View rootView, GitHubReleaseClient.ReleaseInfo release) {
        if (release == null || !release.hasApk()) {
            showStatus("The APK download link is missing from this release.");
            return;
        }

        String downloadUrl = release.apkUrl.trim();
        if (!downloadUrl.startsWith("https://")) {
            showStatus("The GitHub APK link is not secure. Update cancelled.");
            return;
        }

        try {
            DownloadManager downloadManager =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                showStatus("Android Download Manager is unavailable.");
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long previousDownloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1L);
            if (previousDownloadId != -1L) {
                try {
                    downloadManager.remove(previousDownloadId);
                } catch (Exception ignored) {
                    // A completed or expired download may no longer be removable.
                }
            }

            String safeVersion = release.tagName.replaceAll("[^A-Za-z0-9._-]", "_");
            String filename = "SUMITX-update-" + safeVersion + ".apk";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("SUMITX " + release.tagName);
            request.setDescription("Downloading the latest release APK...");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalFilesDir(
                    context, Environment.DIRECTORY_DOWNLOADS, filename
            );

            long downloadId = downloadManager.enqueue(request);
            prefs.edit()
                    .putLong(KEY_DOWNLOAD_ID, downloadId)
                    .putString(KEY_DOWNLOAD_VERSION, release.tagName)
                    .apply();

            showStatus("Downloading SUMITX " + release.tagName
                    + ". Android will open the installer when the APK is ready.");
        } catch (Exception e) {
            Log.e(TAG, "Could not start SUMITX update download", e);
            showStatus("Could not start the update download. Please try again.");
        }
    }

    private void runOnUi(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    private void showStatus(String message) {
        runOnUi(() -> com.dsann.music.ui.SnackbarController.INSTANCE.showMessage(message));
    }
}
