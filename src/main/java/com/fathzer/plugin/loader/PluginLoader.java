package com.fathzer.plugin.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/** A class able to load plugins from an abstract source.
 * @param T The source type
 */
public abstract class PluginLoader<T> {
	private ClassNameBuilder<T> classNameBuilder;
	private InstanceBuilder instanceBuilder;

	/** Constructor.
	 * <br>By default, the plugins are instantiated using their public no argument constructor.
	 * <br>This makes the default behaviour quite similar to {@link java.util.ServiceLoader}
	 * @param defaultClassNameBuilder The {@link ClassNameBuilder} used by this class to find the class name of the plugins to be loaded.
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	protected PluginLoader(ClassNameBuilder<T> defaultClassNameBuilder) {
		this.classNameBuilder = defaultClassNameBuilder;
		this.instanceBuilder = InstanceBuilder.DEFAULT;
	}
	
	/** Sets the class name builder.
	 * @param classNameBuilder The new builder
	 * @return this
	 */
	public PluginLoader<T> withClassNameBuilder(ClassNameBuilder<T> classNameBuilder) {
		this.classNameBuilder = classNameBuilder;
		return this;
	}

	/** Sets the instance builder.
	 * @param instanceBuilder The new builder
	 * @return this
	 */
	public PluginLoader<T> withInstanceBuilder(InstanceBuilder instanceBuilder) {
		this.instanceBuilder = instanceBuilder;
		return this;
	}

	/** Gets the plugins contained in a source.
	 * @param <V> The interface/class of the plugins (all plugins should implement/extends this interface/class).
	 * @param source The source to scan.
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A {@link Plugins} instance whose class loader is the classLoader returned by {@link #buildClassLoader(Path)}.
	 * @throws IOException if a problem occurs while reading the source.
	 */
	public <V> Plugins<V> getPlugins(T source, Class<V> aClass) throws IOException {
		final Set<String> classNames = classNameBuilder.get(source, aClass);
		final ClassLoader loader = classNames.isEmpty() ? null : buildClassLoader(source);
		final Plugins<V> result = new Plugins<>(aClass);
		classNames.forEach(c -> result.add(loader, c, instanceBuilder));
		return result;
	}
	
	/** Builds the classloader that will be used to load the plugin classes.*/
	protected abstract ClassLoader buildClassLoader(T context) throws IOException;
}
