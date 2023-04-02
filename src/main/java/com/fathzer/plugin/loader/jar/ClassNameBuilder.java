package com.fathzer.plugin.loader.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/** A class that finds the plugin's concrete class name from a jar, typically from a manifest attribute. 
 */
public interface ClassNameBuilder {
	/** Gets the class names of the plugin implementations.
	 * @param jarFile The jar that contains the plugin
	 * @param aClass The interface or abstract class implemented by the plugin.
	 * @return The class names of the plugin implementation (typically a list of argument of the <i>loadClass</i> method of a ClassLoader).
	 * @throws IOException if something went wrong
	 */
	Set<String> get(Path jarFile, Class<?> aClass) throws IOException;
}