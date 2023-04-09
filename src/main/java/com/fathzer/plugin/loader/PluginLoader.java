package com.fathzer.plugin.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** A class able to load plugins from an abstract source.
 * @param <T> The source type
 */
public abstract class PluginLoader<T> {
	private ClassNameBuilder<T> classNameBuilder;
	private InstanceBuilder instanceBuilder;
	private Consumer<PluginInstantiationException> exceptionConsumer;

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
		this.exceptionConsumer = e -> {throw e;};
	}
	
	/** Sets the class name builder.
	 * @param classNameBuilder The new builder
	 * @return this
	 */
	public PluginLoader<T> withClassNameBuilder(ClassNameBuilder<T> classNameBuilder) {
		if (classNameBuilder==null) {
			throw new IllegalArgumentException();
		}
		this.classNameBuilder = classNameBuilder;
		return this;
	}

	/** Sets the instance builder.
	 * @param instanceBuilder The new builder
	 * @return this
	 */
	public PluginLoader<T> withInstanceBuilder(InstanceBuilder instanceBuilder) {
		if (instanceBuilder==null) {
			throw new IllegalArgumentException();
		}
		this.instanceBuilder = instanceBuilder;
		return this;
	}
	
	/** Sets the PluginInstantiation exception consumer.
	 * <br>When the {@link InstanceBuilder} throws an exception, the exception consumer is called to process the exception.
	 * <br>The default implementation re-throws the exception. You may provide your own to, for instance, log the error and continue.
	 * @param exceptionConsumer The new consumer
	 * @return this
	 */
	public PluginLoader<T> withExceptionConsumer(Consumer<PluginInstantiationException> exceptionConsumer) {
		if (exceptionConsumer==null) {
			throw new IllegalArgumentException();
		}
		this.exceptionConsumer = exceptionConsumer;
		return this;
	}

	/** Gets the plugins contained in a source.
	 * @param <V> The interface/class of the plugins (all plugins should implement/extends this interface/class).
	 * @param source The source to scan.
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A list of instances whose class loader is the classLoader returned by {@link #buildClassLoader(Object)}.
	 * @throws IOException if a problem occurs while reading the source.
	 * @throws PluginInstantiationException if a problem occurs while creating the plugins.
	 */
	public <V> List<V> getPlugins(T source, Class<V> aClass) throws IOException {
		final Set<String> classNames = classNameBuilder.get(source, aClass);
		final ClassLoader loader = classNames.isEmpty() ? null : buildClassLoader(source);
		final List<V> result = new ArrayList<>();
		classNames.forEach(c -> {
			try {
				result.add(build(loader, c, aClass));
			} catch(PluginInstantiationException e) {
				exceptionConsumer.accept(e);
			}
		});
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private <V> V build(ClassLoader loader, String className, Class<V> aClass) {
		try {
			final Class<?> pluginClass = loader.loadClass(className);
			if (aClass.isAssignableFrom(pluginClass)) {
				return (V)instanceBuilder.get(pluginClass);
			} else {
				throw new PluginInstantiationException(className+" is not a "+aClass.getCanonicalName()+" instance");
			}
		} catch (PluginInstantiationException e) {
			throw e;
		} catch (Exception e) {
			throw new PluginInstantiationException(e);
		}
	}

	
	/** Builds the classloader that will be used to load the plugin classes.
	 * @param context The context, for example, the path of a jar file.
	 * @return A classLoader that can load classes from the context.
	 */
	protected abstract ClassLoader buildClassLoader(T context);
}
