package com.fathzer.plugin.loader.commons;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fathzer.plugin.loader.ClassNameBuilder;

/** An abstract {@link ClassNameBuilder} that retrieves the class names in the same way as {@link ServiceLoader}.
 * @param <T> The context's class
 */
public abstract class AbstractServiceClassNameBuilder<T> implements ClassNameBuilder<T> {
	/** Gets the service definition file path for a service class.
	 * @param serviceClass The service to search
	 * @return a String
	 */
	protected String getServiceFilePath(Class<?> serviceClass) {
		return "META-INF/services/"+serviceClass.getName();
	}
	
	/** Gets a BufferedReader on an inputStream.
	 * @param in The inputStream;
	 * @return a BufferedReader.
	 */
	protected BufferedReader getBufferedReader(InputStream in) {
		return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
	}
	
	/** Converts the lines  of a service definition file to a {@link Set} of implementation class names.
	 * @param lines The lines of a service definition file.
	 * @return a Set.
	 */
	protected Set<String> toClassNames(Stream<String> lines) {
		return lines.map(this::uncommentAndTrim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
	}
	
	private String uncommentAndTrim(String token) {
		final int index = token.indexOf('#');
		token = index>=0 ? token.substring(0, index) : token;
		return token.trim();
	}
}
