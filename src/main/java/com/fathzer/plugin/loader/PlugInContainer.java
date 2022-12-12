package com.fathzer.plugin.loader;

import java.util.function.Supplier;

/** The result of a try to load a plugin.
 * <br>This result could be a concrete instance of the plugin or an exception that occurred during its loading.
 * @param <T> The plugin interface.
 */
public class PlugInContainer<T> implements AutoCloseable, Supplier<T> {
	private T instance;
	private final PluginInstantiationException e;
	
	/** Constructor from a concrete implementation of the plugin. 
	 * @param instance An instance of the plugin.
	 */
	public PlugInContainer(T instance) {
		this.instance = instance;
		this.e = null;
	}

	/** Constructor from an exception.
	 * @param ex The exception that occurred during the plugin creation.
	 */
	public PlugInContainer(PluginInstantiationException ex) {
		this.instance = null;
		this.e = ex;
	}
	
	/** Constructor from a Class and an instance builder.
	 * @param pluginClass The concrete class of the plugin.
	 * @param builder An instance builder that will create the plugin from its class.
	 */
	public PlugInContainer(Class<?extends T> pluginClass, InstanceBuilder builder) {
		PluginInstantiationException exception = null;
		try {
			this.instance = builder.get(pluginClass);
		} catch (Exception ex) {
			exception = new PluginInstantiationException(ex);
		}
		this.e = exception;
	}

	/** Gets the plugin instance.
	 * @return a plugin instance or null if the plugin failed to load. In such a case {@link #getException()} will return the exception that occurred.
	 * @see #getException()
	 */
	@Override
	public T get() {
		return instance;
	}

	/** Gets the exception that occurred during the plugin instanciation.
	 * @return an exception or null if no error occurred.
	 */
	public PluginInstantiationException getException() {
		return e;
	}

	/** Closes this container, relinquishing any underlying resources.
	 * <br>This default implementation does nothing.
	 */
	@Override
	public void close() {
		// Nothing to do
	}
	
	@Override
	public String toString() {
		return this.instance==null ? this.e.toString() : this.instance.getClass().getCanonicalName();
	}
}
