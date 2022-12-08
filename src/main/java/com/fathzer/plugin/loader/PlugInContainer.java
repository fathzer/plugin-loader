package com.fathzer.plugin.loader;

import java.util.function.Supplier;

public interface PlugInContainer<T> extends AutoCloseable, Supplier<T> {
	/** Gets the exception that occurred during the plugin instanciation.
	 * @return an exception or null if no error occurred.
	 */
	PluginInstantiationException getException();

	@Override
	void close();
}
