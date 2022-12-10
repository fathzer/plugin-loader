package com.fathzer.plugin.loader.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.JarPluginLoader;
import com.fathzer.plugin.loader.JarPluginLoader.InstanceBuilder;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.PluginInstantiationException;

class LoaderTest {

	@Test
	void test() {
		// Verify it works in the standard way 
		final File pluginsFolder = new File("src/test/resources");
		JarPluginLoader loader = new JarPluginLoader();
		List<PlugInContainer<Supplier>> plugins = loader.getPlugins(pluginsFolder, Supplier.class);
		assertEquals(1, plugins.size());
		assertTrue(isValid(plugins.get(0)));
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.get(0).toString());
		
		// Test depth + Invalid jar file (empty jar)
		assertThrows(IllegalArgumentException.class, () -> loader.withDepth(0));
		assertThrows(IllegalArgumentException.class, () -> loader.withDepth(-1));
		loader.withDepth(Integer.MAX_VALUE);
		plugins = loader.getPlugins(new File("src/test/resources"), Supplier.class);
		assertEquals(2, plugins.size());
		final List<String> validPlugins = plugins.stream().filter(this::isValid).map(Object::toString).collect(Collectors.toList());
		assertEquals("[com.fathzer.plugin.loader.test.Plugin]",validPlugins.toString());
		
		loader.withDepth(1);
		
		// Test unknown constructor
		final Integer paramInt = Integer.MAX_VALUE;
		loader.withInstanceBuilder(new OtherInstanceBuilder<Integer>(paramInt, Integer.class));
		plugins = loader.getPlugins(pluginsFolder, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));

		// Test other constructor
		final String param = "parameter";
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(param, String.class));
		plugins = loader.getPlugins(pluginsFolder, Supplier.class);
		assertEquals(1, plugins.size());
		assertTrue(isValid(plugins.get(0)));
		assertEquals(param, plugins.get(0).get().get());
		
		// Test class does not implement the plugin class
		List<PlugInContainer<Function>> fplugins = loader.getPlugins(pluginsFolder, Function.class);
		assertEquals(1, fplugins.size());
		assertFalse(isValid(fplugins.get(0)));
		
		// Test class name finder & unknown class
		loader.withClassNameBuilder(new JarPluginLoader.ManifestAttributeClassNameBuilder("Unknown-Plugin-Class"));
		plugins = loader.getPlugins(pluginsFolder, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));

		// Test class name finder & unknown manifest attribute
		loader.withClassNameBuilder(new JarPluginLoader.ManifestAttributeClassNameBuilder("Unknown-Attribute"));
		plugins = loader.getPlugins(pluginsFolder, Supplier.class);
		assertEquals(1, plugins.size());
		assertFalse(isValid(plugins.get(0)));
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

	private static class OtherInstanceBuilder<T> implements InstanceBuilder {
		private final T param;
		private final Class<T> aClass;

		public OtherInstanceBuilder(T param, Class<T> aClass) {
			this.param = param;
			this.aClass = aClass;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> pluginClass) throws PluginInstantiationException {
			try {
				final Constructor<?> constructor = pluginClass.getConstructor(aClass);
				return (T) constructor.newInstance(param);
			} catch (ReflectiveOperationException | SecurityException  e) {
				throw new PluginInstantiationException(e);
			}
		}
	}
}
