package com.fathzer.plugin.loader.classloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fathzer.plugin.loader.ClassNameBuilder;
import com.fathzer.plugin.loader.commons.AbstractServiceClassNameBuilder;

/** A {@link ClassNameBuilder} that retrieves the class names from a {@link ClassLoader} in the same way as {@link ServiceLoader}.
 */
public class ServiceClassNameBuilder extends AbstractServiceClassNameBuilder<ClassLoader> {
	private Predicate<URL> urlFilter; 
	
	/** Constructor.
	 * <br>By default, this instance scans all sources in the class loader. 
	 * @see #setUrlFilter(Predicate)
	 */
	public ServiceClassNameBuilder() {
		super();
		this.urlFilter = x->true;
	}
	
	/** Sets a filter to narrow the search to some sources.
	 * @param urlFilter A predicate. All URL sources that does not match this predicate will be exclude from the search. 
	 */
	public void setUrlFilter(Predicate<URL> urlFilter) {
		this.urlFilter = urlFilter;
	}

	@Override
	public Set<String> get(ClassLoader loader, Class<?> aClass) throws IOException {
		final Enumeration<URL> resources = loader.getResources(getServiceFilePath(aClass));
		Stream<String> allLines = Stream.empty();
		while (resources.hasMoreElements()) {
			final URL url = resources.nextElement();
			if (urlFilter.test(url)) {
				try (BufferedReader in = getBufferedReader(url.openStream())) {
					// Be cautious, the stream returned by in.lines() is closed at the end of the try
					// So we have to copy the lines into memory
					allLines = Stream.concat(allLines, Arrays.stream(in.lines().toArray(String[]::new)));
				}
			}
		}
		return toClassNames(allLines);
	}
}