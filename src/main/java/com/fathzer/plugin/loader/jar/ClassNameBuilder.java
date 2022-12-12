package com.fathzer.plugin.loader.jar;

import java.io.File;

/** A class that finds the plugin's concrete class name from a jar, typically from a manifest attribute. 
 */
public interface ClassNameBuilder {
	/** Gets the class name of the plugin implementation.
	 * @param jarFile The jar that contains the plugin
	 * @param aClass The interface or abstract class implemented by the plugin.
	 * @return The class name of the plugin implementation (typically the argument of the <i>loadClass</i> method of a ClassLoader).
	 * @throws Exception if something went wrong
	 */
	String get(File jarFile, Class<?> aClass) throws Exception;
}