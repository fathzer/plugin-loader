package com.fathzer.plugin.loader.jar;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/** A {@link ClassNameBuilder} that retrieves the class names in an attribute of jar's manifest.
 */
public class ManifestAttributeClassNameBuilder implements ClassNameBuilder {
	private final String attrName;
	
	/** Constructor.
	 * @param attrName The name of the manifest's attribute that contains the plugin's class name.
	 */
	public ManifestAttributeClassNameBuilder(String attrName) {
		this.attrName = attrName;
	}

	@Override
	public String get(File file, Class<?> aClass) throws IOException {
		try (JarFile jar = new JarFile(file)) {
			final String className = jar.getManifest().getMainAttributes().getValue(attrName);
			if (className==null) {
				throw new IOException("Unable to find "+attrName+" entry in jar manifest of "+file);
			}
			return className;
		}
	}
}