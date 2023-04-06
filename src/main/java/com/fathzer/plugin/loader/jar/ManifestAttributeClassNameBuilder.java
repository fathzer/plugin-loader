package com.fathzer.plugin.loader.jar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import com.fathzer.plugin.loader.ClassNameBuilder;

/** A {@link ClassNameBuilder} that retrieves the class names in an attribute of jar's manifest.
 * <br>It seems that merging manifest attributes in a <a href="https://imagej.net/develop/uber-jars">fat jar</a> is not
 * very easy, so this kind of name builder is discouraged.
 * <br>Use it at your own risks... 
 */
public class ManifestAttributeClassNameBuilder implements ClassNameBuilder<Path> {
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
			final Manifest manifest = jar.getManifest();
			if (manifest==null) {
				return Collections.emptySet();
			}
			final String className = manifest.getMainAttributes().getValue(attrName);
			if (className==null) {
				return Collections.emptySet();
			}
			return Arrays.stream(className.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
		}
	}
}