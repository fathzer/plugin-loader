package com.fathzer.plugin.loader.jar;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.plugin.loader.Constants.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PluginInstantiationException;

class JarLoaderTest {

	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final PluginLoader<Path> loader = new JarPluginLoader().withClassNameBuilder(new ManifestAttributeClassNameBuilder("Plugin-Class"));
		final List<Supplier> plugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertEquals(1, plugins.size());
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.get(0).getClass().getCanonicalName());
		
		// Test other constructor
		final String param = "parameter";
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(param, String.class));
		final List<Supplier> otherPlugins = loader.getPlugins(OK_FILE, Supplier.class);
		assertEquals(1, otherPlugins.size());
		assertEquals(param, otherPlugins.get(0).get());

		// Test invalid jar file (empty jar)
		assertThrows (IOException.class, () -> loader.getPlugins(KO_FILE, Supplier.class));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	void testInstantiationProblems() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader();

		// Test unknown service file
		List<Function> fplugins = loader.getPlugins(OK_FILE, Function.class);
		assertTrue(fplugins.isEmpty());

		// Test unknown constructor
		final Integer paramInt = Integer.MAX_VALUE;
		loader.withInstanceBuilder(new OtherInstanceBuilder<Integer>(paramInt, Integer.class));
		assertThrows(PluginInstantiationException.class, () -> loader.getPlugins(OK_FILE, Supplier.class));

		// Test illegalArgument
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(null, String.class));
		assertThrows(PluginInstantiationException.class, () -> loader.getPlugins(OK_FILE, Supplier.class));
		
		// Test class name builder & unknown class
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Plugin-Class"));
		assertThrows(PluginInstantiationException.class, () -> loader.getPlugins(OK_FILE, Supplier.class));
		
		// Test class is not assignable
		loader.withClassNameBuilder((p,c) -> Collections.singleton("com.fathzer.plugin.loader.test.Plugin"));
		assertThrows(PluginInstantiationException.class, () -> loader.getPlugins(OK_FILE, Function.class));
		
		// Test exception consumer
		final List<PluginInstantiationException> ex = new ArrayList<>();
		loader.withExceptionConsumer(ex::add);
		fplugins = loader.getPlugins(OK_FILE, Function.class);
		assertTrue(fplugins.isEmpty());
		assertEquals(1, ex.size());
		

		// Test class name finder & unknown manifest attribute
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Attribute"));
		assertTrue(loader.getPlugins(OK_FILE, Supplier.class).isEmpty());
	}
	
	private static class OtherInstanceBuilder<P> implements InstanceBuilder {
		private final P param;
		private final Class<P> aClass;

		public OtherInstanceBuilder(P param, Class<P> aClass) {
			this.param = param;
			this.aClass = aClass;
		}
		
		@Override
		public <T> T get(Class<T> pluginClass) throws Exception {
			final Constructor<T> constructor = pluginClass.getConstructor(aClass);
			return constructor.newInstance(param);
		}
	}
}
