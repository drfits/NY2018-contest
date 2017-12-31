package com.drfits.fun;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Class intend to find specified keyword within jetconf.by site
 *
 * how to execute: java -jar [jar name] "[git_archive_url]" "[search_keyword]"
 *
 * "java" - is a pattern for search within site pages
 */
public final class Main {

    private static final String FILES_FOR_SEARCH = ".*html";

    public static void main(final String[] args) throws IOException {
        final Instant start = Instant.now();

        System.out.println("Simply simple.");
        if (args.length < 2) {
            System.err.println("Please specify url and pattern for search.");
            System.err.println(
                "Command line format: java -jar [jar name] \"[git_archive_url]\" \"[search_keyword]\"");
            System.exit(1);
        }

        final String archiveUrl = args[0];
        final Path archivePath = download(archiveUrl);
        final String searchPattern = args[1];
        final int matches = countMatches(archivePath, searchPattern);
        System.out.println("Number of \"" + searchPattern + "\" matches: " + matches);
        Files.deleteIfExists(archivePath);
        System.out.println("File removed: " + archivePath);
        System.out.println("Execution time is (ISO-8601): " + Duration.between(start, Instant.now()));
    }

    /**
     * Download main branch from GIT repository
     */
    private static Path download(final String url) throws IOException {
        final Path archivePath = Files.createTempFile(System.nanoTime() + "", "");
        final URL archiveUrl = new URL(url);
        try (ReadableByteChannel rbc = Channels.newChannel(archiveUrl.openStream())) {
            try (FileOutputStream fos = new FileOutputStream(archivePath.toFile())) {
                System.out.println("Downloading " + url + " as " + archivePath);
                try (FileChannel channel = fos.getChannel()) {
                    final long size = channel.transferFrom(rbc, 0, Long.MAX_VALUE);
                    System.out.println("Archive downloaded. Size is " + size + " bytes.");
                }
            }
        }
        return archivePath;
    }


    private static int countMatches(final Path archivePath, final String searchPattern) {
        final Pattern pattern = Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE);
        System.out.println("Find " + searchPattern + " occurrences");
        int count = 0;
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            count = zipFile.stream()
                .filter(zipEntry -> zipEntry.getName().matches(FILES_FOR_SEARCH))
                .mapToInt(zipEntry -> getMatches(zipFile, zipEntry, pattern))
                .sum();
        } catch (final IOException e) {
            System.err.println("Cannot search within archive: " + e.getMessage());
        }
        return count;
    }

    /**
     * Find matches of the string within file.
     * @param zipFile where locates
     * @param zipEntry where to search.
     * @return number of matches
     */
    private static int getMatches(final ZipFile zipFile, final ZipEntry zipEntry, final Pattern pattern) {
        int count = 0;
        try (
            InputStream is = zipFile.getInputStream(zipEntry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            count = reader.lines().mapToInt(line -> getMatches(line, pattern)).sum();
            System.out.println(zipEntry.getName() + " has " + count + " occurrences.");
        } catch (final IOException e) {
            System.out.println("Error when read archive: " + e.getMessage());
        }
        return count;
    }

    private static int getMatches(final String str, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(str);
        int count = 0;
        int from = 0;
        while (matcher.find(from)) {
            count++;
            from = matcher.start() + 1;
        }
        return count;
    }
}
