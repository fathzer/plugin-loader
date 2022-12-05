package com.fathzer.plugin.loader;

import java.util.function.Supplier;

public interface PlugInContainer<T> extends AutoCloseable, Supplier<T> {
	/** Gets the exception that occurred during the plugin instanciation.
	 * @return a throwable or null if no error occurred.
	 */
	Throwable getInstanciationException();

	@Override
	void close();
}
