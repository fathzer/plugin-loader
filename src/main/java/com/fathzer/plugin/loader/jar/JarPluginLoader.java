package com.fathzer.plugin.loader.jar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.PluginInstantiationException;

/** A class able to load plugins from jar files contained in a folder.
 */
public class JarPluginLoader {
	/** The name of the default attribute of jar's manifest used to retrieve the plugins class names. */
	public static final String DEFAULT_PLUGIN_CLASS_MANIFEST_ATTRIBUTE = "Plugin-Class";
	
	private ClassNameBuilder classNameBuilder;
	private InstanceBuilder instanceBuilder;

	/** Constructor.
	 * <br>By default, the class name of the plugins are searched in the attribute {@value #DEFAULT_PLUGIN_CLASS_MANIFEST_ATTRIBUTE} of the jar's manifest.
	 * <br>The plugins are instantiated using their public no argument constructor.
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	public JarPluginLoader() {
		this.classNameBuilder = new ManifestAttributeClassNameBuilder(DEFAULT_PLUGIN_CLASS_MANIFEST_ATTRIBUTE);
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

	/** Gets the plugins contained in jar files in a folder.
	 * @param <T> The interface/class of the plugins (all plugins should implement/extends this interface/class).
	 * @param folder The folder that contains the jar. 
	 * @param depth The maximum number of directory levels to search.
	 * <br>A value of 1 means the search is limited to the jars directly under the searched folder.
	 * <br>To set no limit, you should set the depth to Integer.MAX_VALUE
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A list of #JarPlugInContainer
	 * @throws IOException if a problem occurs while browsing the folder.
	 * @throws IllegalArgumentException if depth is &lt; 1
	 */
	public <T> List<PlugInContainer<T>> getPlugins(File folder, int depth, Class<T> aClass) throws IOException {
		if (depth<1) {
			throw new IllegalArgumentException();
		}
		final List<PlugInContainer<T>> plugins = new ArrayList<>();
	    try (Stream<Path> files = Files.find(folder.toPath(), depth, (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))) {
			files.forEach(p -> plugins.add(getContainer(p.toFile(), aClass)));
	    }
		return plugins;
	}
	
	private <T> JarPlugInContainer<T> getContainer(File jarFile, Class<T> aClass) {
		try {
			final String className = classNameBuilder.get(jarFile, aClass);
			final URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});
			final T plugin = build(loader, className, aClass);
			return new JarPlugInContainer<>(jarFile, loader, plugin);
		} catch (Exception ex) {
			return new JarPlugInContainer<>(jarFile, new PluginInstantiationException(ex));
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T build(URLClassLoader loader, String className, Class<T> aClass) throws Exception {
		try {
			final Class<?> pluginClass = loader.loadClass(className);
			if (aClass.isAssignableFrom(pluginClass)) {
				return instanceBuilder.get((Class<T>) pluginClass);
			} else {
				throw new PluginInstantiationException(className+" is not a "+aClass.getCanonicalName()+" instance");
			}
		} catch (Exception ex) {
			tryToClose(loader);
			throw ex;
		}
	}
	
	private void tryToClose(Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
