package com.fathzer.plugin.loader.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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
	public Set<String> get(Path file, Class<?> aClass) throws IOException {
		try (JarFile jar = new JarFile(file.toFile())) {
			final String className = jar.getManifest().getMainAttributes().getValue(attrName);
			if (className==null) {
				return Collections.emptySet();
			}
			return Arrays.stream(className.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
		}
	}
}