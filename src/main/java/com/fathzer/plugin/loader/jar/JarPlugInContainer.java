package com.fathzer.plugin.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;

import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.PluginInstantiationException;

/** A plugin container dedicated to plugins loaded from a jar.
 * @param <T> The interface of the plugin. 
 */
public class JarPlugInContainer<T> extends PlugInContainer<T> {
	private URLClassLoader loader;
	private final File jarFile;
	
	JarPlugInContainer (File file, URLClassLoader classLoader, T instance) {
		super(instance);
		this.jarFile = file;
		this.loader = classLoader;
	}
	
	JarPlugInContainer (File file, PluginInstantiationException exception) {
		super(exception);
		this.jarFile = file;
		this.loader = null;
	}
	
	/** Gets the jar file from which the plugin was read.
	 * @return a File
	 */
	public File getFile() {
		return jarFile;
	}

	/** Closes this container, relinquishing any underlying resources.
	 * <br>Be aware that this method closes the underlying #{@link java.lang.ClassLoader}. After it is closed, using the plugin may have unpredictable results.
	 */
	@Override
	public void close() {
		if (loader != null) {
			try {
				loader.close();
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			loader = null;
		}
	}
}
