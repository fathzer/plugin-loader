package com.fathzer.plugin.loader.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

class AbstractPluginsDownloaderTest {
	private static final String PLUGINS_JAR_URI_PATH = "/plugins/test.jar";
	private static final String MISSING_JAR_PLUGIN_KEY = "missing";
	private static final String VALID_PLUGIN_KEY = "test";
	private static final String REGISTRY_OK_CONTENT = "registryOk";
	private static final String REGISTRY_PATH = "/registry";
	private static final String FAKE_JAR_FILE_CONTENT = "A fake jar file";
	private static final String CUSTOM_HEADER = "myHeader";
	private static final String REGISTRY_HEADER_VALUE = "registry";
	private static final String JAR_HEADER_VALUE = "jar";

	private final class TestPluginDownloader<T> extends AbstractPluginsDownloader<T> {
		private final Map<String,URI> map;
		private Collection<Path> lastPathsLoaded;
		
		private TestPluginDownloader(PluginRegistry<T> registry, URI uri, Path localDirectory) {
			super(registry, uri, localDirectory);
			map = new HashMap<>();
			map.put(VALID_PLUGIN_KEY, getUri().resolve(PLUGINS_JAR_URI_PATH));
			map.put(MISSING_JAR_PLUGIN_KEY, getUri().resolve("/plugins/missing.jar"));
		}

		@Override
		protected Map<String, URI> getURIMap(InputStream in) throws IOException {
			final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			if (REGISTRY_OK_CONTENT.equals(content)) {
				return map;
			} else {
				throw new IOException(content);
			}
		}

		@Override
		protected void customizeRegistryRequest(Builder requestBuilder) {
			super.customizeRegistryRequest(requestBuilder);
			requestBuilder.header(CUSTOM_HEADER, REGISTRY_HEADER_VALUE);
		}

		@Override
		protected void customizeJarRequest(Builder requestBuilder) {
			super.customizeJarRequest(requestBuilder);
			requestBuilder.header(CUSTOM_HEADER, JAR_HEADER_VALUE);
		}

		@Override
		protected void load(Collection<Path> paths) {
			this.lastPathsLoaded = paths;
		}
	}

	private static MockWebServer server;
	
	@BeforeAll
	static void init() throws IOException {
		final Dispatcher dispatcher = new Dispatcher() {
		    @Override
		    public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
		        switch (request.getPath()) {
		            case REGISTRY_PATH:
		                return new MockResponse().setResponseCode(200).setBody(REGISTRY_OK_CONTENT);
		            case "/registryKo":
		                return new MockResponse().setResponseCode(200).setBody("registryKo");
		            case PLUGINS_JAR_URI_PATH:
		                return new MockResponse().setResponseCode(200).setBody(FAKE_JAR_FILE_CONTENT);
		        }
		        return new MockResponse().setResponseCode(404);
		    }
		};
		server = new MockWebServer();
		// Start the server.
		server.setDispatcher(dispatcher);
		server.start();
	}
	
	@AfterAll
	static void cleanUp() throws IOException {
		server.close();
	}
	
	@Test
	void testWrongConstructorArgs(@TempDir Path dir){
		final PluginRegistry<Object> plugins = new PluginRegistry<>(Object::toString);
		final URI uri = server.url(REGISTRY_PATH).uri();
		assertThrows(IllegalArgumentException.class, () -> new TestPluginDownloader<>(null, uri, dir));
		assertThrows(IllegalArgumentException.class, () -> new TestPluginDownloader<>(plugins, null, dir));
		assertThrows(IllegalArgumentException.class, () -> new TestPluginDownloader<>(plugins, uri, null));
	}

	
	@Test
	void testUnknownURI(@TempDir Path dir) throws IOException {
		final PluginRegistry<Object> plugins = new PluginRegistry<>(Object::toString);
		{
			final AbstractPluginsDownloader<Object> downloader = new TestPluginDownloader<>(plugins, server.url("/registryKo").uri(), dir);
			assertThrows (IOException.class, () -> downloader.getURIMap());
		}

		final AbstractPluginsDownloader<Object> downloader = new TestPluginDownloader<>(plugins, server.url("/registryUnknown").uri(), dir);
		assertThrows (IOException.class, () -> downloader.getURIMap());
	}

	@Test
	void test(@TempDir Path dir) throws Exception {
		@SuppressWarnings("unchecked")
		PluginRegistry<Object> registry = mock(PluginRegistry.class);
		final TestPluginDownloader<Object> downloader = new TestPluginDownloader<>(registry, server.url(REGISTRY_PATH).uri(), dir);
		
		assertEquals(registry, downloader.getRegistry());
		assertEquals(dir, downloader.getLocalDirectory());
		downloader.setPluginTypeWording("object");
		assertEquals("object",downloader.getPluginTypeWording());
		
		// Test getting remote plugins map is correct
		clearRequests();
		final Map<String, URI> map = downloader.getURIMap();
		assertEquals(new HashSet<>(Arrays.asList(VALID_PLUGIN_KEY,MISSING_JAR_PLUGIN_KEY)), map.keySet());
		RecordedRequest request = server.takeRequest();
		assertEquals(REGISTRY_HEADER_VALUE,request.getHeader(CUSTOM_HEADER));

		// Test load of a valid key
		when(registry.get(VALID_PLUGIN_KEY)).thenReturn("ok");
		downloader.load(VALID_PLUGIN_KEY);
		// check right paths were passed to registry (a URL classLoader on the right file)
		assertEquals(1, downloader.lastPathsLoaded.size());
		final Path path = downloader.lastPathsLoaded.iterator().next();
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		// Test load of a key missing in registry
		assertThrows(IllegalArgumentException.class, () -> downloader.load("Not in registry"));

		// Test load of a key in registry, but with missing jar
		assertThrows(IOException.class, () -> downloader.load(MISSING_JAR_PLUGIN_KEY));
		
		// Test load nothing doesn't throw exception
		downloader.load();
		
		// Test searched plugin was in repository but not effectively in the repository's jar 
		when(registry.get(VALID_PLUGIN_KEY)).thenReturn(null);
		assertThrows(IllegalArgumentException.class, () -> downloader.load(VALID_PLUGIN_KEY));
	}
	
	@Test
	void testEmptyDir(@TempDir Path dir) throws Exception {
		final AbstractPluginsDownloader<Object> downloader = new TestPluginDownloader<>(new PluginRegistry<>(Object::toString), server.url(REGISTRY_PATH).uri(), dir);

		assertTrue(Files.deleteIfExists(dir), "Problem while deleting temp dir");
		downloader.clean(); // Test no exception is thrown
		Path path = downloader.download(server.url(PLUGINS_JAR_URI_PATH).uri());
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
	}
	
	@Test
	void testDownloadAndClean(@TempDir Path dir) throws IOException, InterruptedException {
		final AbstractPluginsDownloader<Object> downloader = new TestPluginDownloader<>(new PluginRegistry<>(Object::toString), server.url(REGISTRY_PATH).uri(), dir);
		final URI existingURI = server.url(PLUGINS_JAR_URI_PATH).uri();
		clearRequests();
		Path path = downloader.download(existingURI);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		// Test already downloaded jar is not reloaded
		final FileTime lastModifiedTime = Files.getLastModifiedTime(path);
		Path path2 = downloader.download(existingURI);
		assertEquals(path, path2);
		assertEquals(lastModifiedTime, Files.getLastModifiedTime(path));
		
		RecordedRequest request = server.takeRequest();
		assertEquals(JAR_HEADER_VALUE, request.getHeader(CUSTOM_HEADER));
		
		downloader.clean();
		assertFalse(Files.isRegularFile(path));

		final URI missingURI = server.url("/plugins/missing.jar").uri();
		assertThrows(IOException.class, ()->downloader.download(missingURI));
	}

	private void clearRequests() throws InterruptedException {
		do {} while(server.takeRequest(100, TimeUnit.MILLISECONDS)!=null);
	}
}
