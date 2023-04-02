package com.fathzer.plugin.loader.jar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/** A {@link ClassNameBuilder} that retrieves the class names in the same way as {@link ServiceLoader}.
 */
public class ServiceClassNameBuilder implements ClassNameBuilder {
	@Override
	public Set<String> get(Path file, Class<?> aClass) throws IOException {
		try (JarFile jar = new JarFile(file.toFile())) {
			final String serviceFilePath = "META-INF/services/"+aClass.getName();
			final ZipEntry zip = jar.getEntry(serviceFilePath);
			if (zip==null) {
				return Collections.emptySet();
			}
			try (BufferedReader in = new BufferedReader(new InputStreamReader(jar.getInputStream(zip), StandardCharsets.UTF_8))) {
				return in.lines().map(this::uncommentAndTrim).collect(Collectors.toSet());
			}
		}
	}
	
	String uncommentAndTrim(String line) {
		final int index = line.indexOf('#');
		line = index>=0 ? line.substring(0, index) : line;
		return line.trim();
	}
}