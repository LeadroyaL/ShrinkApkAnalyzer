/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.apk.analyzer.internal;

import com.android.annotations.NonNull;
import com.android.tools.apk.analyzer.Archive;
import com.android.tools.apk.analyzer.ArchiveContext;
import com.android.tools.apk.analyzer.ArchiveManager;
import com.android.utils.FileUtils;
import com.android.utils.ILogger;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.android.SdkConstants.*;

public class ArchiveManagerImpl implements ArchiveManager {
    /**
     * List of extensions we're likely to find inside APK files (or AIA bundles or AARs) that are to
     * be treated as internal archives that can be browsed in APK analyzer
     */
    private static final List<String> INNER_ZIP_EXTENSIONS =
            ImmutableList.of(".zip", ".apk", ".jar");

    @NonNull private final ILogger logger;
    @NonNull private final Map<Path, Archive> archives = new HashMap<>();

    @NonNull
    private final Map<Archive, Path> tempDirectories = new TreeMap<>(new ArchivePathComparator());

    public ArchiveManagerImpl(@NonNull ILogger logger) {
        this.logger = logger;
    }

    @NonNull
    @Override
    public ArchiveContext openArchive(@NonNull Path path) throws IOException {
        Archive archive = MapUtils.computeIfAbsent(archives, path, this::openArchiveWorker);
        return new ArchiveContextImpl(this, archive);
    }

    @Override
    public void close() throws IOException {
        // Close all archives
        for (Archive archive : archives.values()) {
            logger.info(String.format("Closing archive \"%s\"", archive.getPath()));
            archive.close();
        }
        archives.clear();

        // Delete all temporary directories
        for (Path dir : tempDirectories.values()) {
            logger.info(String.format("Deleting temp directory \"%s\"", dir));
            FileUtils.deleteRecursivelyIfExists(dir.toFile());
        }
        tempDirectories.clear();
    }

    @NonNull
    private Archive openArchiveWorker(@NonNull Path path) throws IOException {
        logger.info(String.format("Opening archive \"%s\"", path));
        if (hasFileExtension(path, EXT_ZIP)) {
            // We assume this is an AIA bundle, which we give special handling
            throw new RuntimeException("Unsupport InstantAppBundleArchive");
        } else if (hasFileExtension(path, EXT_APP_BUNDLE)) {
            // Android App Bundle (.aab) archive
            throw new RuntimeException("Unsupport AppBundleArchive");
        } else if (hasFileExtension(path, EXT_ANDROID_PACKAGE)) {
            // APK file archive
            return new ApkArchive(path);
        } else {
            return new ZipArchive(path);
        }
    }

    private static boolean hasFileExtension(@NonNull Path path, @NonNull String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        //noinspection StringToUpperCaseOrToLowerCaseWithoutLocale
        return path.getFileName().toString().toLowerCase().endsWith(extension);
    }

    private static class ArchivePathComparator implements Comparator<Archive> {
        @Override
        public int compare(Archive o1, Archive o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    }
}
