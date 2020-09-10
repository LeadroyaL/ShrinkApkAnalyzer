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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import joptsimple.*;
import joptsimple.internal.Rows;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ApkAnalyzerCli {
    private static final String FLAG_FILE_PATH = "file";
    private static final String APKANALYZER = "apkanalyzer";
    private static final String SUBJECT_APK = "apk";
    private static final String SUBJECT_MANIFEST = "manifest";
    private static final String SUBJECT_RESOURCES = "resources";
    private static final String ACTION_SUMMARY = "summary";
    private static final String ACTION_PRINT = "print";
    private static final String ACTION_APPLICATION_ID = "application-id";
    private static final String ACTION_VERSION_NAME = "version-name";
    private static final String ACTION_VERSION_CODE = "version-code";
    private static final String ACTION_MIN_SDK = "min-sdk";
    private static final String ACTION_TARGET_SDK = "target-sdk";
    private static final String ACTION_DEBUGGABLE = "debuggable";
    private static final String ACTION_XML = "xml";

    private final PrintStream out;
    private final PrintStream err;
    private final ApkAnalyzerImpl impl;

    private static final class HelpFormatter extends BuiltinHelpFormatter {
        public HelpFormatter() {
            super(120, 2);
        }

        @Override
        protected boolean shouldShowNonOptionArgumentDisplay(OptionDescriptor nonOptionDescriptor) {
            return false;
        }
    }

    public ApkAnalyzerCli(
            @NonNull PrintStream out, @NonNull PrintStream err, ApkAnalyzerImpl impl) {
        this.out = out;
        this.err = err;
        this.impl = impl;
    }

    public static void main(String[] args) {
        ApkAnalyzerCli instance =
                new ApkAnalyzerCli(
                        System.out,
                        System.err,
                        new ApkAnalyzerImpl(System.out));
        instance.run(args);
    }

    void run(String... args) {
        OptionParser verbParser = new OptionParser();
        verbParser.posixlyCorrect(true);
        verbParser.allowsUnrecognizedOptions();
        NonOptionArgumentSpec<String> verbSpec = verbParser.nonOptions().ofType(String.class);
        verbParser.formatHelpWith(new HelpFormatter());

        OptionSet parsed = verbParser.parse(args);
        List<String> list = parsed.valuesOf(verbSpec);

        if (list.isEmpty()) {
            printArgsList(null);
        } else if (list.size() == 1) {
            printArgsList(list.get(0));
        } else {
            List<Action> actions = Action.findActions(list.get(0), list.get(1));
            if (actions.isEmpty()) {
                actions = Action.findActions(list.get(0), null);
                if (actions.isEmpty()) {
                    printArgsList(null);
                } else {
                    printArgsList(list.get(0));
                }
            } else {
                try {
                    actions.get(0)
                            .execute(out, err, impl, Arrays.copyOfRange(args, 2, args.length));
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof OptionException) {
                        err.println();
                        err.println("ERROR: " + e.getCause().getMessage());
                    } else {
                        err.println();
                        err.println("ERROR: " + e.getMessage());
                    }
                    exit(1);
                }
                return;
            }
        }

        try {
            err.println(
                    "Usage:"
                            + System.lineSeparator()
                            + APKANALYZER
                            + " [global options] <subject> <verb> [options] <apk> [<apk2>]"
                            + System.lineSeparator());
            verbParser.printHelpOn(err);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void exit(int code) {
        System.exit(code);
    }

    private void printArgsList(@Nullable String subject) {
        if (subject == null) {
            String subjects =
                    Arrays.stream(Action.values())
                            .map(action -> action.getSubject())
                            .distinct()
                            .collect(Collectors.joining(", "));
            err.println("Subject must be one of: " + subjects);
            err.println();
            Rows rows = new Rows(120, 2);
            for (Action action : Action.values()) {
                rows.add(action.getSubject() + " " + action.getVerb(), action.getDescription());
            }
            rows.fitToWidth();
            err.println(rows.render());
        } else {
            List<Action> actions = Action.findActions(subject, null);
            String verbs =
                    actions.stream()
                            .map(action -> action.getVerb())
                            .collect(Collectors.joining(", "));
            err.println("Verb must be one of: " + verbs);
            err.println();
            for (Action action : actions) {
                err.println("==============================");
                err.println(action.getSubject() + " " + action.getVerb() + ":");
                err.println(action.getDescription());
                err.println();
                try {
                    action.getParser().printHelpOn(err);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                err.println();
            }
        }
    }

    enum Action {
        APK_SUMMARY(
                SUBJECT_APK,
                ACTION_SUMMARY,
                "Prints the application Id, version code and version name.") {

            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.apkSummary(opts.valueOf(getFileSpec()).toPath());
            }
        },

        MANIFEST_PRINT(SUBJECT_MANIFEST, ACTION_PRINT, "Prints the manifest in XML format") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestPrint(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_APPLICATION_ID(
                SUBJECT_MANIFEST, ACTION_APPLICATION_ID, "Prints the application id.") {

            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestAppId(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_VERSION_NAME(SUBJECT_MANIFEST, ACTION_VERSION_NAME, "Prints the version name.") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestVersionName(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_VERSION_CODE(SUBJECT_MANIFEST, ACTION_VERSION_CODE, "Prints the version code.") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestVersionCode(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_MIN_SDK(SUBJECT_MANIFEST, ACTION_MIN_SDK, "Prints the minimum sdk.") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestMinSdk(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_TARGET_SDK(SUBJECT_MANIFEST, ACTION_TARGET_SDK, "Prints the target sdk") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestTargetSdk(opts.valueOf(getFileSpec()).toPath());
            }
        },
        MANIFEST_DEBUGGABLE(
                SUBJECT_MANIFEST, ACTION_DEBUGGABLE, "Prints if the app is debuggable") {
            @Override
            public void execute(
                    PrintStream out,
                    PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                impl.manifestDebuggable(opts.valueOf(getFileSpec()).toPath());
            }
        },
        RESOURCES_XML(
                SUBJECT_RESOURCES, ACTION_XML, "Prints the human readable form of a binary XML") {
            @Nullable public OptionParser parser;
            @Nullable private ArgumentAcceptingOptionSpec<String> filePathSpec;

            @NonNull
            @Override
            public OptionParser getParser() {
                if (parser == null) {
                    parser = super.getParser();
                    filePathSpec = parser
                            .accepts(FLAG_FILE_PATH, "File path within the APK.")
                            .withRequiredArg()
                            .ofType(String.class);
                }
                return parser;

            }

            @Override
            public void execute(PrintStream out, PrintStream err,
                    @NonNull ApkAnalyzerImpl impl,
                    @NonNull String... args) {
                OptionParser parser = getParser();
                OptionSet opts = parseOrPrintHelp(parser, err, args);
                assert filePathSpec != null;
                impl.resXml(
                        opts.valueOf(getFileSpec()).toPath(), opts.valueOf(filePathSpec));
            }
        },
        ;

        private final String description;
        private final String verb;
        private final String subject;
        private OptionParser parser;
        private NonOptionArgumentSpec<File> fileSpec;

        Action(String subject, String verb, String description) {
            this.subject = subject;
            this.verb = verb;
            this.description = description;
        }

        private void initParser(){
            parser = new OptionParser();
            parser.formatHelpWith(new HelpFormatter());
            fileSpec =
                    parser.nonOptions("apk").describedAs("APK file path").ofType(File.class);
        }

        @NonNull
        public OptionParser getParser(){
            if (parser == null){
                initParser();
            }
            return parser;
        }

        @NonNull
        public NonOptionArgumentSpec<File> getFileSpec(){
            if (parser == null){
                initParser();
            }
            return fileSpec;
        }

        public abstract void execute(
                PrintStream out, PrintStream err, @NonNull ApkAnalyzerImpl impl,
                @NonNull String... args);

        @NonNull
        public String getVerb() {
            return verb;
        }

        @NonNull
        public String getSubject() {
            return subject;
        }

        @NonNull
        public static List<Action> findActions(@NonNull String subject, @Nullable String verb) {
            ArrayList<Action> actions = new ArrayList<>();
            for (Action action : Action.values()) {
                if (subject.equals(action.subject) && (verb == null || verb.equals(action.verb))) {
                    actions.add(action);
                }
            }
            return actions;
        }

        public String getDescription() {
            return description;
        }

        private static OptionSet parseOrPrintHelp(@NonNull OptionParser parser, @NonNull PrintStream err, String... args) {
            try {
                OptionSet opts = parser.parse(args);
                List<?> files = opts.nonOptionArguments();
                if (files.isEmpty()) {
                    try {
                        parser.printHelpOn(err);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    throw new RuntimeException("You must specify an apk file.");
                }
                return opts;
            } catch (OptionException e) {
                try {
                    parser.printHelpOn(err);
                } catch (IOException e1) {
                    throw new UncheckedIOException(e1);
                }
                throw new RuntimeException(e);
            }
        }
    }
}
