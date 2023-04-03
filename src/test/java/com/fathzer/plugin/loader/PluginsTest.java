package com.fathzer.plugin.loader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.jar.JarPluginLoader;

class PluginsTest {
	
	private static class NotASupplier {}

	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final Plugins<Supplier> plugins = new Plugins<>();
		assertTrue(plugins.isEmpty());
		assertEquals(0, plugins.getExceptions().size());
		assertEquals(0, plugins.getInstances().size());
		// Adds an exception
		final String className = NotASupplier.class.getCanonicalName();
		plugins.add(ClassLoader.getSystemClassLoader(), className, Supplier.class, InstanceBuilder.DEFAULT);
		assertFalse(plugins.isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		assertEquals(0, plugins.getInstances().size());
		final Supplier instance = () -> "a supplier of mine";
		plugins.add(instance);
		assertFalse(plugins.isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		assertEquals(1, plugins.getInstances().size());
		
		
		Plugins<Supplier> plugins2 = new JarPluginLoader().getPlugins(Constants.OK_FILE, Supplier.class);
		assertFalse(plugins2.isEmpty());
	}

}
