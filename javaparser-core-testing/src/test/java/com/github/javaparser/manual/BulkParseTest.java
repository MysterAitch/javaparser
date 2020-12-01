/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2019 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.manual;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.DownloadTest;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.SourceZip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.*;
import static com.github.javaparser.utils.CodeGenerationUtils.f;
import static com.github.javaparser.utils.CodeGenerationUtils.mavenModuleRoot;
import static com.github.javaparser.utils.SourceRoot.Callback.Result.DONT_SAVE;
import static com.github.javaparser.utils.TestUtils.download;
import static com.github.javaparser.utils.TestUtils.temporaryDirectory;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DownloadTest
class BulkParseTest {

    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_langTools_snapshot = new HashMap<>();
    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_langTools_tip = new HashMap<>();
    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_jdk_tip = new HashMap<>();
    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_jdk_snapshot = new HashMap<>();

    static {
        /*
         * These URLs are found by choosing a tag here in the source repo, then copying the "zip" link
         * http://hg.openjdk.java.net/jdk9/jdk9/langtools/tags
         * http://hg.openjdk.java.net/jdk10/jdk10/langtools/tags
         *
         * Note that from Java 11, the langtools are within the test directory of the JDK source code.
         * It is to be confirmed whether the langtools directory can be downloaded in isolation.
         */

        // The langtools directory -- approximately 13 MiB
        downloadUrls_langTools_snapshot.put(JAVA_6, "http://hg.openjdk.java.net/jdk6/jdk6/langtools/archive/779c45081059.zip"); // 4.6 MiB
        downloadUrls_langTools_snapshot.put(JAVA_7, "http://hg.openjdk.java.net/jdk7/jdk7/langtools/archive/ce654f4ecfd8.zip"); // 5.8 MiB
        downloadUrls_langTools_snapshot.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/langtools/archive/1ff9d5118aae.zip"); // 7.9 MiB
        downloadUrls_langTools_snapshot.put(JAVA_9, "http://hg.openjdk.java.net/jdk9/jdk9/langtools/archive/5ecbed313125.zip"); // 13.2 MiB
        downloadUrls_langTools_snapshot.put(JAVA_10, "http://hg.openjdk.java.net/jdk10/jdk10/langtools/archive/19293ea3999f.zip"); // 11.8 MiB

        // The langtools directory -- approximately 13 MiB
        downloadUrls_langTools_tip.put(JAVA_6, "http://hg.openjdk.java.net/jdk6/jdk6/langtools/archive/tip.zip");
        downloadUrls_langTools_tip.put(JAVA_7, "http://hg.openjdk.java.net/jdk7/jdk7/langtools/archive/tip.zip");
        downloadUrls_langTools_tip.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/langtools/archive/tip.zip");
        downloadUrls_langTools_tip.put(JAVA_9, "http://hg.openjdk.java.net/jdk9/jdk9/langtools/archive/tip.zip");
        downloadUrls_langTools_tip.put(JAVA_10, "http://hg.openjdk.java.net/jdk10/jdk10/langtools/archive/tip.zip");

        // The full java source directory -- approximately 160 MiB
        downloadUrls_jdk_snapshot.put(JAVA_6, "http://hg.openjdk.java.net/jdk6/jdk6/jdk/archive/8deef18bb749.zip"); // 2018-12-06
        downloadUrls_jdk_snapshot.put(JAVA_7, "http://hg.openjdk.java.net/jdk7/jdk7/jdk/archive/9b8c96f96a0f.zip"); // 2011-06-27
        downloadUrls_jdk_snapshot.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/jdk/archive/687fd7c7986d.zip"); // 2014-03-04 @ 163 MiB
        downloadUrls_jdk_snapshot.put(JAVA_9, "http://hg.openjdk.java.net/jdk9/jdk9/jdk/archive/65464a307408.zip"); // 2017-08-03
        downloadUrls_jdk_snapshot.put(JAVA_10, "http://hg.openjdk.java.net/jdk-updates/jdk10u/archive/2ba22d2e4ecf.zip"); // 2018-07-17
        downloadUrls_jdk_snapshot.put(JAVA_11, "http://hg.openjdk.java.net/jdk-updates/jdk11u/archive/1356affa5e44.zip"); // 2020-11-25
        downloadUrls_jdk_snapshot.put(JAVA_12, "http://hg.openjdk.java.net/jdk-updates/jdk12u/archive/390566f1850a.zip"); // 2019-07-25
        downloadUrls_jdk_snapshot.put(JAVA_13, "http://hg.openjdk.java.net/jdk-updates/jdk13u/archive/158d79992f86.zip"); // 2020-11-06
        downloadUrls_jdk_snapshot.put(JAVA_14, "http://hg.openjdk.java.net/jdk-updates/jdk14u/archive/680a974138a1.zip"); // 2020-07-09
        downloadUrls_jdk_snapshot.put(JAVA_15, "http://hg.openjdk.java.net/jdk-updates/jdk15u/archive/ac639af55573.zip"); // 2020-11-18 @ 163 MiB

        // The full java source directory -- approximately 160 MiB
        downloadUrls_jdk_tip.put(JAVA_6, "http://hg.openjdk.java.net/jdk6/jdk6/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_7, "http://hg.openjdk.java.net/jdk7/jdk7/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_9, "http://hg.openjdk.java.net/jdk9/jdk9/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_10, "http://hg.openjdk.java.net/jdk-updates/jdk10u/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_11, "http://hg.openjdk.java.net/jdk-updates/jdk11u/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_12, "http://hg.openjdk.java.net/jdk-updates/jdk12u/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_13, "http://hg.openjdk.java.net/jdk-updates/jdk13u/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_14, "http://hg.openjdk.java.net/jdk-updates/jdk14u/archive/tip.zip");
        downloadUrls_jdk_tip.put(JAVA_15, "http://hg.openjdk.java.net/jdk-updates/jdk15u/archive/tip.zip");
    }


    @BeforeEach
    void startLogging() {
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());
    }

    @AfterEach
    void stopLogging() {
        Log.setAdapter(new Log.SilentAdapter());
    }


    /**
     * Running this will download a version of the OpenJDK / lang tools, unzip it, and parse it.
     * If it throws a stack overflow exception, increase the JVM's stack size -- e.g. using -Xss32M
     * <p>
     * Note that there is a lot of duplication (e.g. tip/snapshot will typically resolve to the same version),
     * but this is okay.
     */

    @ParameterizedTest
    @EnumSource(ParserConfiguration.LanguageLevel.class)
    public void langToolsSnapshot(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
        TreeMap<Path, List<Problem>> results = doTest(languageLevel, BulkParseTest.downloadUrls_langTools_snapshot, "langtools", "snapshot");

//                // Problems are expected -- the langtools are used to test java constructs.
//                results.forEach((path, problems) -> {
//                    assertEquals(0, problems.size(), "Expected....");
//                });
    }

    @ParameterizedTest
    @EnumSource(ParserConfiguration.LanguageLevel.class)
    public void langToolsTip(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
        TreeMap<Path, List<Problem>> results = doTest(languageLevel, BulkParseTest.downloadUrls_langTools_tip, "langtools", "tip");

//                // Problems are expected -- the langtools are used to test java constructs.
//                results.forEach((path, problems) -> {
//                    assertEquals(0, problems.size(), "Expected....");
//                });
    }

//    @ParameterizedTest
//    @EnumSource(ParserConfiguration.LanguageLevel.class)
//    public void jdkSnapshot(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
//        TreeMap<Path, List<Problem>> results = doTest(languageLevel, BulkParseTest.downloadUrls_jdk_snapshot, "openjdk", "snapshot");
//
////        if (languageLevel == JAVA_6 || languageLevel == JAVA_7 || languageLevel == JAVA_8 || languageLevel == JAVA_9) {
////            results.forEach((path, problems) -> {
////                assertEquals(0, problems.size(), "Expected zero errors.");
////            });
////        } else {
////            results.entrySet()
////                    .stream()
////                    .filter(pathListEntry -> {
////                        return !(pathListEntry.getKey().subpath(1, 2).toString().equals("test"));
////                    })
////                    .forEach(pathListEntry -> {
////                        assertEquals(0, pathListEntry.getValue().size(), "Expected zero errors for files outside the test directory.");
////                    });
////        }
//    }
//
//    @ParameterizedTest
//    @EnumSource(ParserConfiguration.LanguageLevel.class)
//    public void jdkTip(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
//        TreeMap<Path, List<Problem>> results = doTest(languageLevel, BulkParseTest.downloadUrls_jdk_tip, "openjdk", "tip");
//
////        if (languageLevel == JAVA_6 || languageLevel == JAVA_7 || languageLevel == JAVA_8 || languageLevel == JAVA_9) {
////            results.forEach((path, problems) -> {
////                assertEquals(0, problems.size(), "Expected zero errors.");
////            });
////        } else {
////            results.entrySet()
////                    .stream()
////                    .filter(pathListEntry -> {
////                        return !(pathListEntry.getKey().subpath(1, 2).toString().equals("test"));
////                    })
////                    .forEach(pathListEntry -> {
////                        assertEquals(0, pathListEntry.getValue().size(), "Expected zero errors for files outside the test directory.");
////                    });
////        }
//    }

    private TreeMap<Path, List<Problem>> doTest(ParserConfiguration.LanguageLevel languageLevel, Map<ParserConfiguration.LanguageLevel, String> urls, String type, String s) throws IOException {
        //
        String message = String.format("bulk testing %s - %s, %s", languageLevel, type, s);
        assumeTrue(urls.containsKey(languageLevel), message + " -- SKIPPED, no download url");

        //
        Log.info(message);
        TreeMap<Path, List<Problem>> results = testZip(languageLevel, urls.get(languageLevel), type);
        return results;
    }


    private TreeMap<Path, List<Problem>> testZip(ParserConfiguration.LanguageLevel languageLevel, String downloadUrl, String type) throws IOException {
        //
        String languageLevelName = languageLevel.name();

        // Ensure that working directory is available.
        Path workdir = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get(
                        temporaryDirectory(),
                        "javaparser_bulkparsetest"
                ));
        workdir.toFile().mkdirs();
        assumeTrue(workdir.toFile().exists(), "Cannot perform test unless the temporary download directory exists.");

        //
        String[] split = downloadUrl.split("/");
        String replace = split[split.length - 1].replace(".zip", "");
        String zipName = type + "-" + languageLevelName + "-" + replace + ".zip";
        Path zipPath = workdir.resolve(zipName);

        // Download it if it's not already downloaded
        if (Files.notExists(zipPath)) {
            Log.info(String.format(
                    "Downloading JDK %s %s from %s to %s",
                    languageLevelName,
                    type,
                    downloadUrl,
                    zipPath.toString()
            ));
            download(new URL(downloadUrl), zipPath);
        }
        assumeTrue(zipPath.toFile().exists(), "Cannot perform test unless the downloaded zip file exists.");

        // Do the bulk test
        TreeMap<Path, List<Problem>> results = new TreeMap<>();
//        results = bulkTest(
//                new SourceZip(zipPath),
//                "openjdk_" + languageLevelName + "_" + type + "_" + replace + "_repo_test_results.txt",
//                new ParserConfiguration().setLanguageLevel(languageLevel)
//        );

        return results;
    }

    @Test
    void parseOwnSourceCode() throws IOException {
        final String[] roots = new String[]{
                "javaparser-core/src/main/java",
                "javaparser-core-testing/src/test/java",
                "javaparser-core-generators/src/main/java",
                "javaparser-core-metamodel-generator/src/main/java",
                "javaparser-symbol-solver-core/src/main/java",
                "javaparser-symbol-solver-testing/src/test/java"
        };

        for (String root : roots) {
            TreeMap<Path, List<Problem>> result = bulkTest(
                    new SourceRoot(mavenModuleRoot(BulkParseTest.class).resolve("..").resolve(root)),
                    String.format(
                            "javaparser_test_results_%s.txt",
                            root.replace("-", "_").replace("/", "_")
                    ),
                    new ParserConfiguration().setLanguageLevel(BLEEDING_EDGE)
            );

//            result.forEach((key, value) -> {
//                assertEquals(0, value.size(), "Expected zero errors when parsing JavaParser's own source code. ");
//            });
        }
    }


    public TreeMap<Path, List<Problem>> bulkTest(SourceRoot sourceRoot, String testResultsFileName, ParserConfiguration configuration) throws IOException {
        sourceRoot.setParserConfiguration(configuration);
        TreeMap<Path, List<Problem>> results = new TreeMap<>(comparing(o -> o.toString().toLowerCase()));
        sourceRoot.parseParallelized((localPath, absolutePath, result) -> {
            if (!localPath.toString().contains("target")) {
                if (!result.isSuccessful()) {
                    results.put(localPath, result.getProblems());
                }
            }
            return DONT_SAVE;
        });
        return writeResults(results, testResultsFileName);
    }

    public TreeMap<Path, List<Problem>> bulkTest(SourceZip sourceRoot, String testResultsFileName, ParserConfiguration configuration) throws IOException {
        sourceRoot.setParserConfiguration(configuration);
        TreeMap<Path, List<Problem>> results = new TreeMap<>(comparing(o -> o.toString().toLowerCase()));
        sourceRoot.parse((path, result) -> {
            if (!path.toString().contains("target")) {
                if (!result.isSuccessful()) {
                    results.put(path, result.getProblems());
                }
            }
        });
        return writeResults(results, testResultsFileName);
    }

    private TreeMap<Path, List<Problem>> writeResults(TreeMap<Path, List<Problem>> results, String testResultsFileName) throws IOException {
        Log.info("Writing results...");

        Path testResultsDirectory = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get("..", "javaparser-core-testing", "src", "test", "resources", "com", "github", "javaparser", "bulk_test_results"))
                .normalize();
        testResultsDirectory.toFile().mkdirs();

        Path testResultsFile = testResultsDirectory.resolve(testResultsFileName);

        int problemTotal = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(testResultsFile)) {
            for (Map.Entry<Path, List<Problem>> file : results.entrySet()) {
                writer.write(file.getKey().toString().replace("\\", "/"));
                writer.newLine();
                for (Problem problem : file.getValue()) {
                    writer.write(problem.getVerboseMessage());
                    writer.newLine();
                    problemTotal++;
                }
                writer.newLine();
            }
            writer.write(f("%s problems in %s files", problemTotal, results.size()));
        }

        Log.info("Results are in %s", () -> testResultsFile);

        return results;
    }
}
