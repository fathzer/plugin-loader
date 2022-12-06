package com.fathzer.plugin.loader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarPlugInContainer<T> implements PlugInContainer<T> {
	private URLClassLoader loader;
	private final File jarFile;
	private T plugin;
	private Throwable e;
	
	public interface ClassNameBuilder {
		String get(File jarFile, Class<?> aClass) throws IOException;
	}
	
	public static class ManifestAttributeClassNameBuilder implements ClassNameBuilder {
		private final String attrName;
		
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
	
	public JarPlugInContainer (File file, Class<T> aClass, ClassNameBuilder classNameBuilder) {
		this.jarFile = file;
		try {
			final String className = classNameBuilder.get(file, aClass);
			this.loader = new URLClassLoader(new URL[]{file.toURI().toURL()});
			this.plugin = build(className, aClass);
		} catch (IOException ex) {
			this.e = ex;
			close();
		}
	}
	
	public static <T> List<PlugInContainer<T>> getPlugins(File folder, int depth, Class<T> aClass, ClassNameBuilder classNameBuilder) {
		final List<PlugInContainer<T>> plugins = new ArrayList<>();
	    try (Stream<Path> files = Files.find(folder.toPath(), depth, (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))) {
			files.forEach(p -> plugins.add(new JarPlugInContainer<>(p.toFile(), aClass, classNameBuilder)));
	    } catch (IOException e) {
	    	throw new UncheckedIOException(e);
	    }
		return plugins;
	}

	
	@SuppressWarnings("unchecked")
	private T build(String className, Class<T> aClass) throws IOException {
		try {
			final Class<?> pluginClass = loader.loadClass(className);
			if (aClass.isAssignableFrom(pluginClass)) {
				Constructor<?> constructor = pluginClass.getConstructor();
				return (T) constructor.newInstance();
			} else {
				throw new IOException(className+" is not a "+aClass.getCanonicalName()+" instance");
			}
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new IOException(ex);
		}
	}
	
	public File getFile() {
		return jarFile;
	}

	@Override
	public T get() {
		return plugin;
	}
	
	@Override
	public String toString() {
		return this.plugin.getClass().getCanonicalName();
	}

	/** Gets the exception that occurred during the plugin instanciation.
	 * @return a throwable or null if no error occurred.
	 */
	public Throwable getInstanciationException() {
		return e;
	}

	@Override
	public void close() {
		if (loader != null) {
			try {
				loader.close();
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			loader = null;
		}
	}
}
