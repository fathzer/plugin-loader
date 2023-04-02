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

import com.fathzer.plugin.loader.ClassNameBuilder;

/** A {@link ClassNameBuilder} that retrieves the class names in the same way as {@link ServiceLoader}.
 */
public class ServiceClassNameBuilder implements ClassNameBuilder<Path> {
	public static final ServiceClassNameBuilder INSTANCE = new ServiceClassNameBuilder();
	
	protected ServiceClassNameBuilder() {
		super();
	}
	
	@Override
	public Set<String> get(Path file, Class<?> aClass) throws IOException {
		try (JarFile jar = new JarFile(file.toFile())) {
			final String serviceFilePath = "META-INF/services/"+aClass.getName();
			final ZipEntry zip = jar.getEntry(serviceFilePath);
			if (zip==null) {
				return Collections.emptySet();
			}
			try (BufferedReader in = new BufferedReader(new InputStreamReader(jar.getInputStream(zip), StandardCharsets.UTF_8))) {
				return in.lines().map(this::uncommentAndTrim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
			}
		}
	}
	
	String uncommentAndTrim(String token) {
		final int index = token.indexOf('#');
		token = index>=0 ? token.substring(0, index) : token;
		return token.trim();
	}
}