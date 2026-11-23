/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.setupcompat.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Installs the V500 product's optional, app-specific HWUI and EGL program binaries. */
public final class SetupShaderCache {
    private static final String TAG = "SetupShaderCache";

    private SetupShaderCache() {}

    public static void initialize(Context context) {
        if (Build.VERSION.SDK_INT < 26 || !"v500".equals(Build.DEVICE)) return;
        String directory;
        switch (context.getPackageName()) {
            case "org.lineageos.setupwizard":
                directory = "setupwizard-shaders";
                break;
            case "com.android.settings":
                directory = "settings-shaders";
                break;
            default:
                return;
        }
        File source = new File("/system_ext/etc", directory);
        if (source.isDirectory()) install(context, source);
    }

    @TargetApi(26)
    private static void install(Context context, File source) {
        File cache = context.createDeviceProtectedStorageContext().getCacheDir();
        if (cache == null) return;
        for (String name : new String[] {"com.android.opengl.shaders_cache",
                "com.android.skia.shaders_cache"}) {
            Path destination = new File(cache, name).toPath();
            if (Files.exists(destination)) continue;
            Path temporary = null;
            try {
                temporary = Files.createTempFile(cache.toPath(), "initial-shaders-", ".tmp");
                Files.copy(new File(source, name).toPath(), temporary, StandardCopyOption.REPLACE_EXISTING);
                // A hard link publishes the whole file and preserves a cache created concurrently.
                Files.createLink(destination, temporary);
                Log.i(TAG, context.getPackageName() + " initialized " + name);
            } catch (FileAlreadyExistsException ignored) {
                // Another process already initialized the cache.
            } catch (IOException e) {
                Log.w(TAG, "Could not initialize " + name, e);
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (IOException e) {
                        Log.w(TAG, "Could not remove temporary shader cache", e);
                    }
                }
            }
        }
    }
}
