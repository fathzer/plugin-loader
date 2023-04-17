package com.fathzer.plugin.loader.classloader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.Constants;

class ServiceClassNameBuilderTest {

	@Test
	void test() throws IOException {
		final ServiceClassNameBuilder nameBuilder = new ServiceClassNameBuilder();
		assertEquals(Collections.singleton("com.fathzer.plugin.loader.classloader.ASupplier"),nameBuilder.get(ClassLoader.getSystemClassLoader(), Supplier.class));
		
		try (URLClassLoader loader = new URLClassLoader(new URL[]{Constants.OK_FILE.toUri().toURL()})) {
			assertEquals(new HashSet<>(Arrays.asList("com.fathzer.plugin.loader.classloader.ASupplier","com.fathzer.plugin.loader.test.Plugin")),nameBuilder.get(loader, Supplier.class));
			
			// Try excluding file: resources
			nameBuilder.setUrlFilter(u->!"file".equals(u.getProtocol()));
			assertEquals(Collections.singleton("com.fathzer.plugin.loader.test.Plugin"),nameBuilder.get(loader, Supplier.class));
		}
	}

}
