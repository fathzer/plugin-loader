package com.fathzer.plugin.loader.jar;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;

import com.fathzer.plugin.loader.ClassNameBuilder;
import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.Plugins;

/** A class able to load plugins from jar files contained in a folder.
 */
public class JarPluginLoader {
	private ClassNameBuilder<Path> classNameBuilder;
	private InstanceBuilder instanceBuilder;

	/** Constructor.
	 * <br>By default, the class name of the plugins are searched with a lenient {@link ServiceClassNameBuilder}.
	 * <br>The plugins are instantiated using their public no argument constructor.
	 * <br>This makes the default behaviour quite similar to {@link java.util.ServiceLoader}
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	public JarPluginLoader() {
		this.classNameBuilder = ServiceClassNameBuilder.INSTANCE;
		this.instanceBuilder = InstanceBuilder.DEFAULT;
	}
	
	/** Sets the class name builder.
	 * @param classNameBuilder The new builder
	 * @return this
	 */
	public JarPluginLoader withClassNameBuilder(ClassNameBuilder<Path> classNameBuilder) {
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

	/** Gets the plugins contained in a jar file.
	 * @param <T> The interface/class of the plugins (all plugins should implement/extends this interface/class).
	 * @param jarFile The file to scan.
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A {@link Plugins} instance whose class loader is a URLClassLoader (unless {@link #buildClassLoader(Path)} was override to return something else).
	 * To perform cleanup after plugins are loaded, you can close this loader (@see {@link URLClassLoader#close()}.
	 * @throws IOException if a problem occurs while reading the jar.
	 */
	public <T> Plugins<T> getPlugins(Path jarFile, Class<T> aClass) throws IOException {
		final Set<String> classNames = classNameBuilder.get(jarFile, aClass);
		final ClassLoader loader = classNames.isEmpty() ? null : buildClassLoader(jarFile);
		final Plugins<T> result = new Plugins<>(aClass);
		classNames.forEach(c -> result.add(loader, c, instanceBuilder));
		return result;
	}
	
	/** Builds the classloader that will be used to load the plugin classes.
	 * <br>The default implementation returns a {@link URLClassLoader} on the <i>jarFile</i>'s url.
	 * <br>You may override this method if you want to change this behaviour.
	 * @param jarFile the jar file passed to {@link #getPlugins(Path, Class)}
	 * @return A classloader.  
	 * @throws IOException if something went wrong
	 */
	protected ClassLoader buildClassLoader(Path jarFile) throws IOException {
		return new URLClassLoader(new URL[]{jarFile.toUri().toURL()});
	}
}
