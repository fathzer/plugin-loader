package com.fathzer.plugin.loader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** The result of the plugins loading process.
 * @param <T> The interface of the plugin.
 */
public class Plugins<T> implements AutoCloseable {
	private URLClassLoader loader;
	private List<PlugInContainer<T>> containers;
	
	public Plugins (URLClassLoader classLoader) {
		this.loader = classLoader;
		this.containers = new ArrayList<>();
	}
	
	public void add(T instance) {
		containers.add(new PlugInContainer<>(instance));
	}

	public void add(PluginInstantiationException exception) {
		containers.add(new PlugInContainer<>(exception));
	}

	public List<PlugInContainer<T>> getPluginContainers() {
		return containers;
	}

	/** Gets the successfully loaded instances.
	 * @return a list of instances
	 */
	public List<T> getInstances() {
		return containers.stream().map(PlugInContainer::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	/** Gets the errors that occured during plugin loading process.
	 * @return a list of exceptions
	 */
	public List<PluginInstantiationException> getExceptions() {
		return containers.stream().map(PlugInContainer::getException).filter(Objects::nonNull).collect(Collectors.toList());
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
