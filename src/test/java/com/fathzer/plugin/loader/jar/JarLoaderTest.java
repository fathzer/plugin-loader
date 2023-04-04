package com.fathzer.plugin.loader.jar;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.plugin.loader.Constants.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.Plugins;

class JarLoaderTest {

	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader().withClassNameBuilder(new ManifestAttributeClassNameBuilder("Plugin-Class"));
		final Plugins<Supplier> plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.getInstances().get(0).getClass().getCanonicalName());
		assertTrue(plugins.getExceptions().isEmpty());
		
		// Test other constructor
		final String param = "parameter";
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(param, String.class));
		final Plugins<Supplier> otherPlugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertEquals(1, otherPlugins.getInstances().size());
		assertEquals(param, otherPlugins.getInstances().get(0).get());

		// Test invalid jar file (empty jar)
		assertThrows (IOException.class, () -> loader.getPlugins(KO_FILE, Supplier.class));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	void testInstantiationProblems() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader();

		// Test unknown service file
		Plugins<Function> fplugins = loader.getPlugins(OK_FILE, Function.class);
		assertTrue(fplugins.isEmpty());

		// Test unknown constructor
		final Integer paramInt = Integer.MAX_VALUE;
		loader.withInstanceBuilder(new OtherInstanceBuilder<Integer>(paramInt, Integer.class));
		Plugins<Supplier> plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());

		// Test illegalArgument
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(null, String.class));
		plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		
		// Test class name builder & unknown class
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Plugin-Class"));
		plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		
		// Test class is not assignable
		loader.withClassNameBuilder((p,c) -> Collections.singleton("com.fathzer.plugin.loader.test.Plugin"));
		fplugins = loader.getPlugins(OK_FILE, Function.class);
		assertTrue(fplugins.getInstances().isEmpty());
		assertEquals(1, fplugins.getExceptions().size());

		// Test class name finder & unknown manifest attribute
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Attribute"));
		plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertTrue(plugins.isEmpty());
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
