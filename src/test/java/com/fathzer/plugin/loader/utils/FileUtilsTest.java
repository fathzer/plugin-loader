package com.fathzer.plugin.loader.utils;

import static com.fathzer.plugin.loader.Constants.KO_FILE;
import static com.fathzer.plugin.loader.Constants.OK_FILE;
import static com.fathzer.plugin.loader.Constants.PLUGINS_FOLDER;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {
	@Test
	void testGetFiles() throws IOException {
		List<Path> files = FileUtils.getJarFiles(PLUGINS_FOLDER, 1);
		assertEquals(1, files.size());
		assertEquals(OK_FILE, files.get(0));

		files = FileUtils.getJarFiles(PLUGINS_FOLDER, 2);
		assertEquals(2, files.size());
		assertEquals(new HashSet<>(Arrays.asList(OK_FILE,KO_FILE)), files.stream().collect(Collectors.toSet()));

		assertThrows(IllegalArgumentException.class, () -> FileUtils.getFiles(PLUGINS_FOLDER, 0, FileUtils.IS_JAR));
		assertThrows(IllegalArgumentException.class, () -> FileUtils.getFiles(PLUGINS_FOLDER, -1, FileUtils.IS_JAR));
		// Verify it sends IOException if folder does not exists
		assertThrows (IOException.class, () -> FileUtils.getFiles(Paths.get("unknown"), 1, FileUtils.IS_JAR));
	}
	
	
	@Test
	void testJarPredicate(@TempDir Path dir) throws IOException {
		final Path nonJarFile = dir.resolve("toto.txt");
		Files.createFile(nonJarFile);
		final BasicFileAttributes bfa = Files.readAttributes(nonJarFile, BasicFileAttributes.class);
		assertFalse(FileUtils.IS_JAR.test(nonJarFile, bfa));
	}
	
	@Test
	void testGetURL() throws URISyntaxException {
		final Path path = new java.io.File("").toPath();
		final URL url = FileUtils.getURL(path);
		assertEquals(path.toAbsolutePath(), Paths.get(url.toURI()));
	}
}
