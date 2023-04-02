package com.fathzer.plugin.loader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.jar.JarPluginLoader;

class PluginsTest {

	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final Plugins<Supplier> plugins = new Plugins<>(ClassLoader.getSystemClassLoader());
		assertEquals(ClassLoader.getSystemClassLoader(), plugins.getClassLoader());
		assertTrue(plugins.isEmpty());
		assertEquals(0, plugins.getExceptions().size());
		assertEquals(0, plugins.getInstances().size());
		plugins.add(new PluginInstantiationException("Just a test"));
		assertFalse(plugins.isEmpty());
		final Supplier instance = () -> "a thing of mine";
		plugins.add(instance);
		assertFalse(plugins.isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		assertEquals(1, plugins.getInstances().size());
		
		
		Plugins<Supplier> plugins2 = new JarPluginLoader().getPlugins(Constants.OK_FILE, Supplier.class);
		assertFalse(plugins2.isEmpty());
		assertThrows(IllegalArgumentException.class, () -> plugins2.add(instance));
	}

}
