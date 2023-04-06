package com.fathzer.plugin.loader.classloader;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.plugin.loader.Constants.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.Plugins;
import com.fathzer.plugin.loader.utils.FileUtils;

class ClassLoaderPluginLoaderTest {
	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final ClassLoaderPluginLoader loader = new ClassLoaderPluginLoader();
		Plugins<Supplier> plugins = loader.getPlugins(Supplier.class);
		assertEquals(1, plugins.getExceptions().size());
		assertTrue(plugins.getInstances().isEmpty());
		
		loader.withClassNameBuilder((context,cls) -> Collections.singleton(MySupplier.class.getCanonicalName()));
		plugins = loader.getPlugins(null, Supplier.class);
		assertEquals(1, plugins.getInstances().size());
		assertTrue(plugins.getExceptions().isEmpty());
		
		plugins = loader.getPlugins(Supplier.class);
		assertEquals(1, plugins.getInstances().size());
		assertTrue(plugins.getExceptions().isEmpty());
		final Class<? extends Supplier> class1 = plugins.getInstances().get(0).getClass();
		assertEquals(MySupplier.class, class1);
		
		final ServiceClassNameBuilder nameBuilder = new ServiceClassNameBuilder();
		nameBuilder.setUrlFilter(url -> "jar".equals(url.getProtocol()));
		loader.withClassNameBuilder(nameBuilder);
		final ClassLoader cl = new URLClassLoader(new URL[] {FileUtils.getURL(OK_FILE)});
		plugins = loader.getPlugins(cl, Supplier.class);
		assertTrue(plugins.getExceptions().isEmpty());
		assertEquals(1, plugins.getInstances().size());
		assertEquals("com.fathzer.plugin.loader.test.Plugin", plugins.getInstances().get(0).getClass().getCanonicalName());
	}

}
