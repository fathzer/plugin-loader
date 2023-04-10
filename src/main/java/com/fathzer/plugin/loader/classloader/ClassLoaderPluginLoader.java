package com.fathzer.plugin.loader.classloader;

import java.io.IOException;
import java.util.List;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.ClassNameBuilder;
import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PluginInstantiationException;

/** A class able to load plugins from jar files contained in a folder.
 */
public class ClassLoaderPluginLoader extends PluginLoader<ClassLoader> {
	/** Constructor.
	 * <br>By default, the class name of the plugins are searched with a {@link ServiceClassNameBuilder}.
	 * <br>The plugins are instantiated using their public no argument constructor.
	 * <br>This makes the default behaviour quite similar to {@link java.util.ServiceLoader}
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	public ClassLoaderPluginLoader() {
		super(new ServiceClassNameBuilder());
	}
	
	/** Loads plugins using the {@link Thread#getContextClassLoader() context ClassLoader} of the calling thread.
	 * @param <V> The plugins type
	 * @param aClass The interface/class implemented/sub-classed by the plugins
	 * @return A List of instances whose class loader is the classLoader returned by {@link #buildClassLoader(Object)}.
	 * @throws IOException if a problem occurs while reading the source.
	 * @throws PluginInstantiationException if a problem occurs while creating the plugins.
	 * @see #getPlugins(ClassLoader, Class)
	 */
	public <V> List<V> getPlugins(Class<V> aClass) throws IOException {
		return getPlugins(null, aClass);
	}
	
	/**
	 * {@inheritDoc}
	 * If that source is null, then the {@link Thread#getContextClassLoader() context ClassLoader} of the calling thread is used.
	 */
	@Override
	public <V> List<V> getPlugins(ClassLoader source, Class<V> aClass) throws IOException {
		return super.getPlugins(source==null ? Thread.currentThread().getContextClassLoader() : source, aClass);
	}

	@Override
	protected ClassLoader buildClassLoader(ClassLoader context) {
		return context;
	}
}
