package com.fathzer.plugin.loader.jar;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.fathzer.plugin.loader.ClassNameBuilder;
import com.fathzer.plugin.loader.commons.AbstractServiceClassNameBuilder;

/** A {@link ClassNameBuilder} that retrieves the class names from a jar file in the same way as {@link ServiceLoader}.
 */
public class ServiceClassNameBuilder extends AbstractServiceClassNameBuilder<Path> {
	/** An instance of this class.
	 */
	public static final ServiceClassNameBuilder INSTANCE = new ServiceClassNameBuilder();
	
	/** Constructor.
	 */
	protected ServiceClassNameBuilder() {
		super();
	}
	
	@Override
	public Set<String> get(Path file, Class<?> aClass) throws IOException {
		try (JarFile jar = new JarFile(file.toFile())) {
			final ZipEntry zip = jar.getEntry(getServiceFilePath(aClass));
			if (zip==null) {
				return Collections.emptySet();
			}
			try (BufferedReader in = getBufferedReader(jar.getInputStream(zip))) {
				return toClassNames(in.lines());
			}
		}
	}
}