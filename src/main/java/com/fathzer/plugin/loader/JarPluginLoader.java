package com.fathzer.plugin.loader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarPluginLoader {
	public static final String DEFAULT_PLUGIN_CLASS_MANIFEST_ATTRIBUTE = "Plugin-Class";
	
	private int depth;
	private ClassNameBuilder classNameBuilder;
	private InstanceBuilder instanceBuilder;

	public interface ClassNameBuilder {
		String get(File jarFile, Class<?> aClass) throws IOException;
	}

	public interface InstanceBuilder {
		<T> T get(Class<T> aClass) throws IOException;
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
	
	public static class DefaultInstanceBuilder implements InstanceBuilder {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> pluginClass) throws IOException {
			try {
				final Constructor<?> constructor = pluginClass.getConstructor();
				return (T) constructor.newInstance();
			} catch (ReflectiveOperationException | SecurityException  e) {
				throw new IOException(e);
			}
		}
	}
	
	public JarPluginLoader() {
		this.classNameBuilder = new ManifestAttributeClassNameBuilder(DEFAULT_PLUGIN_CLASS_MANIFEST_ATTRIBUTE);
		this.instanceBuilder = new DefaultInstanceBuilder();
		this.depth = Integer.MAX_VALUE;
	}
	
	public JarPluginLoader withClassNameBuilder(ClassNameBuilder classNameBuilder) {
		this.classNameBuilder = classNameBuilder;
		return this;
	}

	public JarPluginLoader withDepth(int depth) {
		this.depth = depth;
		return this;
	}
	
	public JarPluginLoader withInstanceBuilder(InstanceBuilder instanceBuilder) {
		this.instanceBuilder = instanceBuilder;
		return this;
	}

	public <T> List<PlugInContainer<T>> getPlugins(File folder, Class<T> aClass) {
		final List<PlugInContainer<T>> plugins = new ArrayList<>();
	    try (Stream<Path> files = Files.find(folder.toPath(), depth, (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))) {
			files.forEach(p -> plugins.add(getContainer(p.toFile(), aClass)));
	    } catch (IOException e) {
	    	throw new UncheckedIOException(e);
	    }
		return plugins;
	}
	
	private <T> JarPlugInContainer<T> getContainer(File jarFile, Class<T> aClass) {
		try {
			final String className = classNameBuilder.get(jarFile, aClass);
			final URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});
			final T plugin = build(loader, className, aClass);
			return new JarPlugInContainer<>(jarFile, loader, plugin);
		} catch (IOException ex) {
			return new JarPlugInContainer<>(jarFile, ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T build(URLClassLoader loader, String className, Class<T> aClass) throws IOException {
		try {
			final Class<?> pluginClass = loader.loadClass(className);
			if (aClass.isAssignableFrom(pluginClass)) {
				return instanceBuilder.get((Class<T>) pluginClass);
			} else {
				loader.close();
				throw new IOException(className+" is not a "+aClass.getCanonicalName()+" instance");
			}
		} catch (ClassNotFoundException ex) {
			loader.close();
			throw new IOException(ex);
		}
	}
}
