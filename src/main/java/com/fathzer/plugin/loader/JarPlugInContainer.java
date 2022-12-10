package com.fathzer.plugin.loader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;

public class JarPlugInContainer<T> implements PlugInContainer<T> {
	private URLClassLoader loader;
	private final File jarFile;
	private final T plugin;
	private final PluginInstantiationException e;
	
	JarPlugInContainer (File file, URLClassLoader classLoader, T instance) {
		this.jarFile = file;
		this.loader = classLoader;
		this.plugin = instance;
		this.e = null;
	}
	
	JarPlugInContainer (File file, PluginInstantiationException exception) {
		this.jarFile = file;
		this.loader = null;
		this.plugin = null;
		this.e = exception;
	}
	
	public File getFile() {
		return jarFile;
	}

	@Override
	public T get() {
		return plugin;
	}
	
	@Override
	public String toString() {
		return this.plugin==null ? this.e.toString() : this.plugin.getClass().getCanonicalName();
	}

	/** Gets the exception that occurred during the plugin instanciation.
	 * @return a throwable or null if no error occurred.
	 */
	public PluginInstantiationException getException() {
		return e;
	}

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
