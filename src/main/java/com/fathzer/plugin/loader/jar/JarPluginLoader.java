package com.fathzer.plugin.loader.jar;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PluginInstantiationException;
import com.fathzer.plugin.loader.Plugins;

/** A class able to load plugins from jar files contained in a folder.
 */
public class JarPluginLoader {
	/** A predicate that matches jar files.
	 * @see #getFiles(File, int, BiPredicate)
	 */
	public static final BiPredicate<Path, BasicFileAttributes> JAR_FILE_PREDICATE = (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar");
	
	
	private ClassNameBuilder classNameBuilder;
	private InstanceBuilder instanceBuilder;

	/** Constructor.
	 * <br>By default, the class name of the plugins are searched with a lenient {@link ServiceClassNameBuilder}.
	 * <br>The plugins are instantiated using their public no argument constructor.
	 * <br>This makes the default behaviour quite similar to {@link java.util.ServiceLoader}
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	public JarPluginLoader() {
		this.classNameBuilder = new ServiceClassNameBuilder();
		this.instanceBuilder = InstanceBuilder.DEFAULT;
	}
	
	/** Sets the class name builder.
	 * @param classNameBuilder The new builder
	 * @return this
	 */
	public JarPluginLoader withClassNameBuilder(ClassNameBuilder classNameBuilder) {
		this.classNameBuilder = classNameBuilder;
		return this;
	}

	/** Sets the instance builder.
	 * @param instanceBuilder The new builder
	 * @return this
	 */
	public JarPluginLoader withInstanceBuilder(InstanceBuilder instanceBuilder) {
		this.instanceBuilder = instanceBuilder;
		return this;
	}

	/** Gets the paths of files contained in a folder.
	 * @param folder The folder to scan
	 * @param depth The maximum number of directory levels to search.
	 * <br>A value of 1 means the search is limited to the jars directly under the searched folder.
	 * <br>To set no limit, you should set the depth to Integer.MAX_VALUE
	 * @param matcher A matcher to filter the files. {@link #JAR_FILE_PREDICATE} can be used to retain all jar files.
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
		return getFiles(folder, depth, JAR_FILE_PREDICATE);
	}

	
	/** Gets the plugins contained in a jar file.
	 * @param <T> The interface/class of the plugins (all plugins should implement/extends this interface/class).
	 * @param jarFile The file to scan.
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A {@link JarPlugIns} instance
	 * @throws IOException if a problem occurs while reading the jar.
	 */
	public <T> Plugins<T> getPlugins(Path jarFile, Class<T> aClass) throws IOException {
		final Set<String> classNames = classNameBuilder.get(jarFile, aClass);
		final URLClassLoader loader = classNames.isEmpty() ? null : new URLClassLoader(new URL[]{jarFile.toUri().toURL()});
		final Plugins<T> result = new Plugins<>(loader);
		for (String c:classNames) {
			try {
				final T plugin = build(loader, c, aClass);
				result.add(plugin);
			} catch (PluginInstantiationException ex) {
				result.add(ex);
			} catch (Exception ex) {
				result.add(new PluginInstantiationException(ex));
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T build(URLClassLoader loader, String className, Class<T> aClass) throws Exception {
		final Class<?> pluginClass = loader.loadClass(className);
		if (aClass.isAssignableFrom(pluginClass)) {
			return instanceBuilder.get((Class<T>) pluginClass);
		} else {
			throw new PluginInstantiationException(className+" is not a "+aClass.getCanonicalName()+" instance");
		}
	}
}
