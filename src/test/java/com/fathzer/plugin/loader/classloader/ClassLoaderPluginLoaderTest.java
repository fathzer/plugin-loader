package com.fathzer.plugin.loader.classloader;

import static org.junit.jupiter.api.Assertions.*;
import static com.fathzer.plugin.loader.Constants.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.PluginInstantiationException;
import com.fathzer.plugin.loader.utils.FileUtils;

class ClassLoaderPluginLoaderTest {
	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final ClassLoaderPluginLoader loader = new ClassLoaderPluginLoader();
		assertThrows(IllegalArgumentException.class, () -> loader.withClassNameBuilder(null));
		assertThrows(IllegalArgumentException.class, () -> loader.withExceptionConsumer(null));
		assertThrows(IllegalArgumentException.class, () -> loader.withInstanceBuilder(null));
		
		assertThrows (PluginInstantiationException.class, () -> loader.getPlugins(Supplier.class));
		
		loader.withClassNameBuilder((context,cls) -> Collections.singleton(MySupplier.class.getCanonicalName()));
		assertEquals(1, loader.getPlugins(null, Supplier.class).size());
		
		List<Supplier> plugins = loader.getPlugins(Supplier.class);
		assertEquals(1, plugins.size());
		final Class<? extends Supplier> class1 = plugins.get(0).getClass();
		assertEquals(MySupplier.class, class1);
		
		final ServiceClassNameBuilder nameBuilder = new ServiceClassNameBuilder();
		nameBuilder.setUrlFilter(url -> "jar".equals(url.getProtocol()));
		loader.withClassNameBuilder(nameBuilder);
		final ClassLoader cl = new URLClassLoader(new URL[] {FileUtils.getURL(OK_FILE)});
		plugins = loader.getPlugins(cl, Supplier.class);
		assertEquals(1, plugins.size());
		assertEquals("com.fathzer.plugin.loader.test.Plugin", plugins.get(0).getClass().getCanonicalName());
	}

}
