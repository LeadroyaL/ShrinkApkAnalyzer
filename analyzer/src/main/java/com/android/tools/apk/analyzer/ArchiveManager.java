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

package com.android.tools.apk.analyzer;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Manages temporary files and directories required for browsing through the entries of an {@link
 * Archive}. Use the {@link #close()} method to delete all temporary files and directories, as well
 * as close file handles of the root and inner archives.
 */
public interface ArchiveManager extends Closeable {
    /**
     * Opens a {@link ArchiveContext} given a {@link Path}. The returned archive is valid until the
     * {@link #close()} method is called.
     */
    @NonNull
    ArchiveContext openArchive(@NonNull Path path) throws IOException;
}
