package com.fathzer.plugin.loader.jar;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fathzer.plugin.loader.InstanceBuilder;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.Plugins;

class JarLoaderTest {
	private Path pluginsFolder = Paths.get("src/test/resources");
	private Path okFile = pluginsFolder.resolve("plugin-loader-test-plugin-0.0.1.jar");
	private Path wrongFile = pluginsFolder.resolve("other/wrongFormat.jar");

	@Test
	void testGetFiles() throws IOException {
		List<Path> files = JarPluginLoader.getJarFiles(pluginsFolder, 1);
		assertEquals(1, files.size());
		assertEquals(okFile, files.get(0));

		files = JarPluginLoader.getJarFiles(pluginsFolder, 2);
		assertEquals(2, files.size());
		assertEquals(new HashSet<>(Arrays.asList(okFile,wrongFile)), files.stream().collect(Collectors.toSet()));

		assertThrows(IllegalArgumentException.class, () -> JarPluginLoader.getFiles(pluginsFolder, 0, JarPluginLoader.JAR_FILE_PREDICATE));
		assertThrows(IllegalArgumentException.class, () -> JarPluginLoader.getFiles(pluginsFolder, -1, JarPluginLoader.JAR_FILE_PREDICATE));
		// Verify it sends IOException if folder does not exists
		assertThrows (IOException.class, () -> JarPluginLoader.getFiles(Paths.get("unknown"), 1, JarPluginLoader.JAR_FILE_PREDICATE));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	void test() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader().withClassNameBuilder(new ManifestAttributeClassNameBuilder("Plugin-Class"));
		Plugins<Supplier> plugins = loader.getPlugins(okFile, Supplier.class);
		assertEquals(1, plugins.getPluginContainers().size());
		assertTrue(isValid(plugins.getPluginContainers().get(0)));
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.getPluginContainers().get(0).toString());
		assertEquals("com.fathzer.plugin.loader.test.Plugin",plugins.getInstances().get(0).getClass().getCanonicalName());
		assertTrue(plugins.getExceptions().isEmpty());
		
		// Test adding a ClassPath plugin
		plugins.add(()->"Hello");
		assertEquals(2, plugins.getInstances().size());
		assertTrue(isValid(plugins.getPluginContainers().get(1)));
		assertEquals("Hello",plugins.getInstances().get(1).get());
		
		// Test other constructor
		final String param = "parameter";
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(param, String.class));
		plugins = loader.getPlugins(okFile, Supplier.class);
		assertEquals(1, plugins.getInstances().size());
		assertTrue(isValid(plugins.getPluginContainers().get(0)));
		assertEquals(param, plugins.getInstances().get(0).get());

		// Test invalid jar file (empty jar)
		assertThrows (IOException.class, () -> loader.getPlugins(wrongFile, Supplier.class));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	void testInstantiationProblems() throws IOException {
		final JarPluginLoader loader = new JarPluginLoader();
		
		// Test unknown constructor
		final Integer paramInt = Integer.MAX_VALUE;
		loader.withInstanceBuilder(new OtherInstanceBuilder<Integer>(paramInt, Integer.class));
		Plugins<Supplier> plugins = loader.getPlugins(okFile, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());

		// Test illegalArgument
		loader.withInstanceBuilder(new OtherInstanceBuilder<String>(null, String.class));
		plugins = loader.getPlugins(okFile, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		
		// Test class name builder & unknown class
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Plugin-Class"));
		plugins = loader.getPlugins(okFile, Supplier.class);
		assertTrue(plugins.getInstances().isEmpty());
		assertEquals(1, plugins.getExceptions().size());
		
		// Test class is not assignable
		loader.withClassNameBuilder((p,c) -> Collections.singleton("com.fathzer.plugin.loader.test.Plugin"));
		Plugins<Function> fplugins = loader.getPlugins(okFile, Function.class);
		assertTrue(fplugins.getInstances().isEmpty());
		assertEquals(1, fplugins.getExceptions().size());

		// Test class name finder & unknown manifest attribute
		loader.withClassNameBuilder(new ManifestAttributeClassNameBuilder("Unknown-Attribute"));
		plugins = loader.getPlugins(okFile, Supplier.class);
		assertTrue(plugins.getPluginContainers().isEmpty());
	}
	
	@SuppressWarnings("rawtypes")
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
	
	@Test
	void testJarPredicate(@TempDir Path dir) throws IOException {
		final Path dirWithJarName = dir.resolve("folder.jar");
		Files.createDirectories(dirWithJarName);
		final BasicFileAttributes bfa = Files.readAttributes(dirWithJarName, BasicFileAttributes.class);
		JarPluginLoader.JAR_FILE_PREDICATE.test(dirWithJarName, bfa);
	}
}
