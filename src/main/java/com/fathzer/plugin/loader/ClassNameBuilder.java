package com.fathzer.plugin.loader;

import java.io.IOException;
import java.util.Set;

/** A class that finds the plugin's concrete class name from a context (for instance from a manifest attribute of a jar).
 * @param <T> The context's class
 */
public interface ClassNameBuilder<T> {
	/** Gets the class names of the plugin implementations.
	 * @param context The jar that contains the plugin
	 * @param aClass The interface or abstract class implemented by the plugin.
	 * @return The class names of the plugin implementation (typically a list of argument of the <i>loadClass</i> method of a ClassLoader).
	 * @throws IOException if something went wrong
	 */
	Set<String> get(T context, Class<?> aClass) throws IOException;
}