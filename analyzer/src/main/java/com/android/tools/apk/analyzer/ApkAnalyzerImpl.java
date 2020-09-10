/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.xml.AndroidManifestParser;
import com.android.ide.common.xml.ManifestData;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tool for getting all kinds of information about an APK, including: - basic package info, sizes
 * and files list - dex code - resources
 */
public class ApkAnalyzerImpl {
    @NonNull private final PrintStream out;

    /** Constructs a new command-line processor. */
    public ApkAnalyzerImpl(@NonNull PrintStream out) {
        this.out = out;
    }

    public void resXml(@NonNull Path apk, @NonNull String filePath) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            Path path = archiveContext.getArchive().getContentRoot().resolve(filePath);
            byte[] bytes = Files.readAllBytes(path);
            if (!archiveContext.getArchive().isBinaryXml(path, bytes)) {
                throw new IOException("The supplied file is not a binary XML resource.");
            }
            out.write(BinaryXmlParser.decodeXml(path.getFileName().toString(), bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @NonNull
    private ManifestData getManifestData(@NonNull Archive archive)
            throws IOException, ParserConfigurationException, SAXException {
        Path manifestPath = archive.getContentRoot().resolve(SdkConstants.ANDROID_MANIFEST_XML);
        byte[] manifestBytes =
                BinaryXmlParser.decodeXml(
                        SdkConstants.ANDROID_MANIFEST_XML, Files.readAllBytes(manifestPath));
        return AndroidManifestParser.parse(new ByteArrayInputStream(manifestBytes));
    }

    public void manifestDebuggable(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            boolean debuggable =
                    manifestData.getDebuggable() != null ? manifestData.getDebuggable() : false;
            out.println(String.valueOf(debuggable));
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestTargetSdk(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.println(String.valueOf(manifestData.getTargetSdkVersion()));
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestMinSdk(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.println(
                    manifestData.getMinSdkVersion() != ManifestData.MIN_SDK_CODENAME
                            ? String.valueOf(manifestData.getMinSdkVersion())
                            : manifestData.getMinSdkVersionString());
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestVersionCode(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.printf("%s", valueToDisplayString(manifestData.getVersionCode())).println();
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestVersionName(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.printf("%s", valueToDisplayString(manifestData.getVersionName())).println();
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestAppId(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.println(manifestData.getPackage());
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void manifestPrint(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            Path path =
                    archiveContext
                            .getArchive()
                            .getContentRoot()
                            .resolve(SdkConstants.ANDROID_MANIFEST_XML);
            byte[] bytes = Files.readAllBytes(path);
            out.write(BinaryXmlParser.decodeXml(path.getFileName().toString(), bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void apkSummary(@NonNull Path apk) {
        try (ArchiveContext archiveContext = Archives.open(apk)) {
            ManifestData manifestData = getManifestData(archiveContext.getArchive());
            out.printf(
                            "%s\t%s\t%s",
                            valueToDisplayString(manifestData.getPackage()),
                            valueToDisplayString(manifestData.getVersionCode()),
                            valueToDisplayString(manifestData.getVersionName()))
                    .println();
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NonNull
    private String valueToDisplayString(Object value) {
        return value == null ? "UNKNOWN" : value.toString();
    }
}
