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
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.SourceZip;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.*;
import static com.github.javaparser.utils.CodeGenerationUtils.f;
import static com.github.javaparser.utils.CodeGenerationUtils.mavenModuleRoot;
import static com.github.javaparser.utils.SourceRoot.Callback.Result.DONT_SAVE;
import static com.github.javaparser.utils.TestUtils.download;
import static com.github.javaparser.utils.TestUtils.temporaryDirectory;
import static java.util.Comparator.comparing;

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


    /**
     * Running this will download a version of the OpenJDK / lang tools, unzip it, and parse it.
     * If it throws a stack overflow exception, increase the JVM's stack size -- e.g. using -Xss32M
     *
     * Note that there is a lot of duplication (e.g. tip/snapshot will typically resolve to the same version),
     * but this is okay.
     */
    public static void main(String[] args) {
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        downloadUrls_langTools_snapshot.forEach((languageLevel, url) -> {
            try {
                // This contains all kinds of test cases so it will lead to a lot of errors:
                new BulkParseTest().parseOpenJdkLangToolsRepository(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        downloadUrls_langTools_tip.forEach((languageLevel, url) -> {
            try {
                // This contains all kinds of test cases so it will lead to a lot of errors:
                new BulkParseTest().parseOpenJdkLangToolsRepository(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        downloadUrls_jdk_snapshot.forEach((languageLevel, url) -> {
            try {
                // This contains the JDK source code, so it should have zero errors:
                new BulkParseTest().parseJdkSrcZip(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        downloadUrls_jdk_tip.forEach((languageLevel, url) -> {
            try {
                // This contains the JDK source code, so it should have zero errors:
                new BulkParseTest().parseJdkSrcZip(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void setupAndDoBulkTest(ParserConfiguration.LanguageLevel languageLevel, String languageLevelName, String type, String downloadUrl) throws IOException {
        Objects.requireNonNull(downloadUrl);

        // Ensure that working directory is available.
        Path workdir = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get(
                        temporaryDirectory(),
                        "javaparser_bulkparsetest"
                ));
        workdir.toFile().mkdirs();

        //
        String[] split = downloadUrl.split("/");
        String replace = split[split.length - 1].replace(".zip", "");
        String zipName = type + "-" + languageLevelName  + "-" + replace  + ".zip";
        Path zipPath = workdir.resolve(zipName);

        // Download it if it's not already downloaded
        if (Files.notExists(zipPath)) {
            Log.info(String.format("Downloading JDK %s " + type + " from %s to %s", languageLevelName, downloadUrl, zipPath.toString()));
            download(new URL(downloadUrl), zipPath);
        }

        // Do the bulk test
        bulkTest(
                new SourceZip(zipPath),
                "openjdk_"+ languageLevelName +"_" + type + "_" + replace + "_repo_test_results.txt",
                new ParserConfiguration().setLanguageLevel(languageLevel)
        );
    }


    private void parseOpenJdkLangToolsRepository(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
        // Config
        String type = "langtools";
        Map<ParserConfiguration.LanguageLevel, String> lookup = downloadUrls_langTools_snapshot;

        //
        String downloadUrl = lookup.get(languageLevel);
        String languageLevelName = languageLevel.name();

        //
        if(downloadUrl == null) {
            Log.error("Download URL for "+ type + " " + languageLevel + " not specified.");
        } else {
            setupAndDoBulkTest(languageLevel, languageLevelName, type, downloadUrl);
        }
    }

    private void parseJdkSrcZip(ParserConfiguration.LanguageLevel languageLevel) throws IOException {
        // Config
        String type = "openjdk";
        Map<ParserConfiguration.LanguageLevel, String> lookup = downloadUrls_jdk_tip;

        //
        String downloadUrl = lookup.get(languageLevel);
        String languageLevelName = languageLevel.name();

        //
        if(downloadUrl == null) {
            Log.error("Download URL for " + languageLevel + " not specified.");
        } else {
            setupAndDoBulkTest(languageLevel, languageLevelName, type, downloadUrl);
        }
    }

    @BeforeEach
    void startLogging() {
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());
    }

    @AfterEach
    void stopLogging() {
        Log.setAdapter(new Log.SilentAdapter());
    }

    @Test
    void parseOwnSourceCode() throws IOException {
        String[] roots = new String[]{
                "javaparser-core/src/main/java",
                "javaparser-core-testing/src/test/java",
                "javaparser-core-generators/src/main/java",
                "javaparser-core-metamodel-generator/src/main/java",
                "javaparser-symbol-solver-core/src/main/java",
                "javaparser-symbol-solver-testing/src/test/java"
        };
        for (String root : roots) {
            bulkTest(
                    new SourceRoot(mavenModuleRoot(BulkParseTest.class).resolve("..").resolve(root)),
                    "javaparser_test_results_" + root.replace("-", "_").replace("/", "_") + ".txt",
                    new ParserConfiguration().setLanguageLevel(BLEEDING_EDGE));
        }
    }

    public void bulkTest(SourceRoot sourceRoot, String testResultsFileName, ParserConfiguration configuration) throws IOException {
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
        writeResults(results, testResultsFileName);
    }

    public void bulkTest(SourceZip sourceRoot, String testResultsFileName, ParserConfiguration configuration) throws IOException {
        sourceRoot.setParserConfiguration(configuration);
        TreeMap<Path, List<Problem>> results = new TreeMap<>(comparing(o -> o.toString().toLowerCase()));
        sourceRoot.parse((path, result) -> {
            if (!path.toString().contains("target")) {
                if (!result.isSuccessful()) {
                    results.put(path, result.getProblems());
                }
            }
        });
        writeResults(results, testResultsFileName);
    }

    private void writeResults(TreeMap<Path, List<Problem>> results, String testResultsFileName) throws IOException {
        Log.info("Writing results...");

        Path testResults = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get("..", "javaparser-core-testing", "src", "test", "resources", "com", "github", "javaparser", "bulk_test_results"))
                .normalize();

        testResults.toFile().mkdirs();
        testResults = testResults.resolve(testResultsFileName);

        int problemTotal = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(testResults)) {
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

        Path finalTestResults = testResults;
        Log.info("Results are in %s", () -> finalTestResults);
    }
}
