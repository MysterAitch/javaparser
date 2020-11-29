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
import java.util.TreeMap;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.*;
import static com.github.javaparser.utils.CodeGenerationUtils.f;
import static com.github.javaparser.utils.CodeGenerationUtils.mavenModuleRoot;
import static com.github.javaparser.utils.SourceRoot.Callback.Result.DONT_SAVE;
import static com.github.javaparser.utils.TestUtils.download;
import static com.github.javaparser.utils.TestUtils.temporaryDirectory;
import static java.util.Comparator.comparing;

class BulkParseTest {

    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_langTools = new HashMap<>();
    private static final Map<ParserConfiguration.LanguageLevel, String> downloadUrls_jdk = new HashMap<>();

    static {
        /*
         * These URLs are found by choosing a tag here in the source repo, then copying the "zip" link
         * http://hg.openjdk.java.net/jdk9/jdk9/langtools/tags
         * http://hg.openjdk.java.net/jdk10/jdk10/langtools/tags
         */

        // The langtools directory -- approximately 13 MiB
        downloadUrls_langTools.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/langtools/archive/1ff9d5118aae.zip"); // 13.2 MiB
        downloadUrls_langTools.put(JAVA_9, "http://hg.openjdk.java.net/jdk9/jdk9/langtools/archive/5ecbed313125.zip"); // 13.2 MiB
        downloadUrls_langTools.put(JAVA_10, "http://hg.openjdk.java.net/jdk10/jdk10/langtools/archive/19293ea3999f.zip"); // 11.9 MiB

        // The full java source directory -- approximately 160 MiB
        downloadUrls_jdk.put(JAVA_8, "http://hg.openjdk.java.net/jdk8/jdk8/jdk/archive/687fd7c7986d.zip"); // 163 MiB
        downloadUrls_jdk.put(JAVA_15, "http://hg.openjdk.java.net/jdk-updates/jdk15u/archive/ac639af55573.zip"); // 163 MiB
    }


    /**
     * Running this will download a version of the OpenJDK / lang tools, unzip it, and parse it.
     * If it throws a stack overflow exception, increase the JVM's stack size -- e.g. using -Xss32M
     */
    public static void main(String[] args) throws IOException {
        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());

        downloadUrls_langTools.forEach((languageLevel, url) -> {
            try {
                new BulkParseTest().parseOpenJdkLangToolsRepository(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        downloadUrls_jdk.forEach((languageLevel, url) -> {
            try {
                // This contains the JDK source code, so it should have zero errors:
                new BulkParseTest().parseJdkSrcZip(languageLevel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void parseOpenJdkLangToolsRepository(ParserConfiguration.LanguageLevel languageLevel) throws IOException {

        // Config
        String languageLevelName = languageLevel.name();
        String openJdkZipName = "langtools-" + languageLevelName + ".zip";

        //
        String downloadUrl = downloadUrls_langTools.get(languageLevel);
        if(downloadUrl == null) {
            Log.error("Download URL for " + languageLevel + " not specified.");
            throw new RuntimeException("Download URL for " + languageLevel + " not specified.");
        }

        // Ensure that working directory is available.
        Path workdir = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get(
                        temporaryDirectory(),
                        "javaparser_bulkparsetest"
                ));
        workdir.toFile().mkdirs();

        //
        Path openJdkZipPath = workdir.resolve(openJdkZipName);

        // Download it if it's not already downloaded
        if (Files.notExists(openJdkZipPath)) {
            Log.info(String.format("Downloading JDK %s langtools from %s to %s", languageLevelName, downloadUrl, openJdkZipPath.toString()));
            download(new URL(downloadUrl), openJdkZipPath);
        }

        // Do the bulk test
        bulkTest(
                new SourceZip(openJdkZipPath),
                "openjdk_"+ languageLevelName +"_" + "langtools" + "_repo_test_results.txt",
                new ParserConfiguration().setLanguageLevel(languageLevel)
        );
    }

    private void parseJdkSrcZip(ParserConfiguration.LanguageLevel languageLevel) throws IOException {

        // Config
        String languageLevelName = languageLevel.name();
        String openJdkZipName = "openjdk-" + languageLevelName + ".zip";

        //
        String downloadUrl = downloadUrls_jdk.get(languageLevel);
        if(downloadUrl == null) {
            Log.error("Download URL for " + languageLevel + " not specified.");
            throw new RuntimeException("Download URL for " + languageLevel + " not specified.");
        }

        // Ensure that working directory is available.
        Path workdir = mavenModuleRoot(BulkParseTest.class)
                .resolve(Paths.get(
                        temporaryDirectory(),
                        "javaparser_bulkparsetest"
                ));
        workdir.toFile().mkdirs();

        //
        Path openJdkZipPath = workdir.resolve(openJdkZipName);

        // Download it if it's not already downloaded
        if (Files.notExists(openJdkZipPath)) {
            Log.info(String.format("Downloading JDK %s source code from %s to %s", languageLevelName, downloadUrl, openJdkZipPath));
            download(new URL(downloadUrl), openJdkZipPath);
        }

        // Do the bulk test
        bulkTest(
                new SourceZip(openJdkZipPath),
                "openjdk_"+ languageLevelName +"_" + "src" + "_repo_test_results.txt",
                new ParserConfiguration().setLanguageLevel(languageLevel)
        );
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

        Path testResults = mavenModuleRoot(BulkParseTest.class).resolve(Paths.get("..", "javaparser-core-testing", "src", "test", "resources", "com", "github", "javaparser", "bulk_test_results")).normalize();
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
