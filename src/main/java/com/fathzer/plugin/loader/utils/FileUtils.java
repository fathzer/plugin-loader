package com.fathzer.plugin.loader.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Some utilities about files.
 */
public class FileUtils {
    private static final String JAR_EXTENSION = ".jar";
    
	/** A predicate that matches jar files.
	 * @see #getFiles(Path, int, BiPredicate)
	 */
	public static final BiPredicate<Path, BasicFileAttributes> IS_JAR = (p, bfa) -> bfa.isRegularFile() && p.toString().endsWith(JAR_EXTENSION);

    private FileUtils() {
		super();
	}
    
	/** Gets the URL of a file
	 * <br>The main difference with Path.toUri().toURL() is it encapsulate the 'more than unlikely' MalformedURLException
	 * thrown by toURL() in a UncheckedIOException, making it easy to use in a lambda expression.
	 * @param file a File
	 * @return an url
	 */
	public static URL getURL(Path file) {
		try {
			return file.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/** Gets the paths of files contained in a folder.
	 * @param folder The folder to scan
	 * @param depth The maximum number of directory levels to search.
	 * <br>A value of 1 means the search is limited to the jars directly under the searched folder.
	 * <br>To set no limit, you should set the depth to Integer.MAX_VALUE
	 * @param matcher A matcher to filter the files. {@link #IS_JAR} can be used to retain all jar files.
	 * @return A list of path that matches the matcher
	 * @throws IOException if a problem occurs while browsing the folder.
	 * @throws IllegalArgumentException if depth is &lt; 1
	 */
	public static List<Path> getFiles(Path folder, int depth,  BiPredicate<Path, BasicFileAttributes> matcher) throws IOException {
		if (depth<1) {
			throw new IllegalArgumentException();
		}
		try (Stream<Path> files = Files.find(folder, depth, matcher)) {
			return files.collect(Collectors.toList());
	    }
	}
	
	/** Gets the paths of jar files contained in a folder.
	 * @param folder The folder to scan
	 * @param depth The maximum number of directory levels to search.
	 * <br>A value of 1 means the search is limited to the jars directly under the searched folder.
	 * <br>To set no limit, you should set the depth to Integer.MAX_VALUE
	 * @return A list of jar files
	 * @throws IOException if a problem occurs while browsing the folder.
	 * @throws IllegalArgumentException if depth is &lt; 1
	 * @see #getFiles(Path, int, BiPredicate)
	 */
	public static List<Path> getJarFiles(Path folder, int depth) throws IOException {
		return getFiles(folder, depth, IS_JAR);
	}
}
