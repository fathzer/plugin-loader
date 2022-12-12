package com.fathzer.plugin.loader.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.jar.JarPluginLoader;
import com.fathzer.plugin.loader.jar.ManifestAttributeClassNameBuilder;

class LoaderTest {
	private File pluginsFolder = new File("src/test/resources");

	@Test
	void test() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader();
		List<PlugInContainer<Supplier>> plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertTrue(isValid(plugins.get(0)));
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.get(0).toString());
		
		// Test adding a ClassPath plugin
		plugins.add(new PlugInContainer<Supplier>(()->"Hello"));
		assertEquals(2, plugins.size());
		assertTrue(isValid(plugins.get(1)));
		assertEquals("Hello",plugins.get(1).get().get());
		
		// Test depth + Invalid jar file (empty jar)
		plugins = loader.getPlugins(new File("src/test/resources"), Integer.MAX_VALUE, Supplier.class);
		assertEquals(2, plugins.size());
		final List<String> validPlugins = plugins.stream().filter(this::isValid).map(Object::toString).collect(Collectors.toList());
		assertEquals("[com.fathzer.plugin.loader.test.Plugin]",validPlugins.toString());
		
		// Test other constructor
		final String param = "parameter";
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(param, String.class));
		plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertTrue(isValid(plugins.get(0)));
		assertEquals(param, plugins.get(0).get().get());
	}
	
	@Test
	void testWrongDepth() {
		final JarPluginLoader loader = new JarPluginLoader();
		assertThrows(IllegalArgumentException.class, () -> loader.getPlugins(pluginsFolder, 0, Supplier.class));
		assertThrows(IllegalArgumentException.class, () -> loader.getPlugins(pluginsFolder, -1, Supplier.class));
	}
	
	@Test
	void testUnexistingFolder() {
		final JarPluginLoader loader = new JarPluginLoader();
		// Verify it sends IOException if folder does not exists
		assertThrows (IOException.class, () -> loader.getPlugins(new File("unknown"), 1, Supplier.class));
	}
	
	@Test
	void testInstantiationProblems() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader();
		
		// Test unknown constructor
		final Integer paramInt = Integer.MAX_VALUE;
		loader.withInstanceBuilder(new OtherInstanceBuilder<Integer>(paramInt, Integer.class));
		List<PlugInContainer<Supplier>> plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));

		// Test illegalArgument
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(null, String.class));
		plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));
		
		// Test class does not implement the plugin class
		List<PlugInContainer<Function>> fplugins = loader.getPlugins(pluginsFolder, 1, Function.class);
		assertEquals(1, fplugins.size());
		assertFalse(isValid(fplugins.get(0)));
		
		// Test class name finder & unknown class
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Plugin-Class"));
		plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));

		// Test class name finder & unknown manifest attribute
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Attribute"));
		plugins = loader.getPlugins(pluginsFolder, 1, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));
	}
	
	@Test
	void testWrongPluginContainer() {
		final Supplier<String> s = () -> "hello";
		PlugInContainer<Supplier> x = new PlugInContainer<>(s.getClass(), new OtherInstanceBuilder<String>("a",String.class));
		assertNull(x.get());
		assertNotNull(x.getException());
	}
	
	private boolean isValid(PlugInContainer<?> container) {
		final boolean isValid = container.getException()==null;
		if (isValid) {
			assertNotNull(container.get());
		} else {
			assertNull(container.get());
		}
		return isValid;
	}

	private static class OtherInstanceBuilder<P> implements InstanceBuilder {
		private final P param;
		private final Class<P> aClass;

		public OtherInstanceBuilder(P param, Class<P> aClass) {
			this.param = param;
			this.aClass = aClass;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> pluginClass) throws Exception {
			final Constructor<?> constructor = pluginClass.getConstructor(aClass);
			return (T) constructor.newInstance(param);
		}
	}
}
