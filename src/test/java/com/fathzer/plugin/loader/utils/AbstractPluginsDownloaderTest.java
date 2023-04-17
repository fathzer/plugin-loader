package com.fathzer.plugin.loader.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
	private static final String REPOSITORY_OK_CONTENT = "repositoryOk";
	private static final String REPOSITORY_PATH = "/repository";
	private static final String FAKE_JAR_FILE_CONTENT = "A fake jar file";
	private static final String CUSTOM_HEADER = "myHeader";
	private static final String REPOSITORY_HEADER_VALUE = "repository";
	private static final String JAR_HEADER_VALUE = "jar";

	private static class TestPluginDownloader extends AbstractPluginsDownloader {
		private final Map<String,URI> map;
		
		private TestPluginDownloader(URI uri, Path localDirectory) {
			super(uri, localDirectory);
			map = new HashMap<>();
			map.put(VALID_PLUGIN_KEY, getUri().resolve(PLUGINS_JAR_URI_PATH));
			map.put(MISSING_JAR_PLUGIN_KEY, getUri().resolve("/plugins/missing.jar"));
		}

		@Override
		protected Map<String, URI> getURIMap(InputStream in) throws IOException {
			final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			if (REPOSITORY_OK_CONTENT.equals(content)) {
				return map;
			} else {
				throw new IOException(content);
			}
		}

		@Override
		protected HttpRequest.Builder getRepositoryRequestBuilder() {
			final HttpRequest.Builder requestBuilder = super.getRepositoryRequestBuilder();
			requestBuilder.header(CUSTOM_HEADER, REPOSITORY_HEADER_VALUE);
			return requestBuilder;
		}

		@Override
		protected HttpRequest.Builder getJarRequestBuilder(URI uri) {
			final HttpRequest.Builder requestBuilder = super.getJarRequestBuilder(uri);
			requestBuilder.header(CUSTOM_HEADER, JAR_HEADER_VALUE);
			return requestBuilder;
		}
	}

	private static MockWebServer server;
	
	@BeforeAll
	static void init() throws IOException {
		final Dispatcher dispatcher = new Dispatcher() {
		    @Override
		    public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
		        switch (request.getPath()) {
		            case REPOSITORY_PATH:
		                return new MockResponse().setResponseCode(200).setBody(REPOSITORY_OK_CONTENT);
		            case "/repositoryKo":
		                return new MockResponse().setResponseCode(200).setBody("repositoryKo");
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
		final URI uri = server.url(REPOSITORY_PATH).uri();
		assertThrows(IllegalArgumentException.class, () -> new TestPluginDownloader(null, dir));
		assertThrows(IllegalArgumentException.class, () -> new TestPluginDownloader(uri, null));
	}

	
	@Test
	void testUnknownURI(@TempDir Path dir) throws IOException {
		{
			final AbstractPluginsDownloader downloader = new TestPluginDownloader(server.url("/repositoryKo").uri(), dir);
			assertThrows (IOException.class, () -> downloader.getURIMap());
		}

		final AbstractPluginsDownloader downloader = new TestPluginDownloader(server.url("/repositoryUnknown").uri(), dir);
		assertThrows (IOException.class, () -> downloader.getURIMap());
	}
	
	@Test
	void testProxy(@TempDir Path dir) throws Exception {
		final String proxyAuthHeader = "Proxy-Authorization";
		final URI uri = server.url(REPOSITORY_PATH).uri();
		final TestPluginDownloader downloader = new TestPluginDownloader(uri, dir);
		clearRequests();
		downloader.setProxy(ProxySettings.fromString("a:b@host:3456"));
		ProxySelector proxy = downloader.getHttpClient().proxy().orElse(null);
		assertNotNull(proxy);
		Proxy p = proxy.select(uri).get(0);
		assertEquals(new InetSocketAddress("host",3456), p.address());
		// Verif proxy authorization is there.
		HttpRequest request = downloader.getRepositoryRequestBuilder().build();
		final Optional<String> header = request.headers().firstValue(proxyAuthHeader);
		assertTrue(header.isPresent());
		
		// Test we can revert proxy to null
		downloader.setProxy(null);
		assertFalse(downloader.getHttpClient().proxy().isPresent());
		request = downloader.getRepositoryRequestBuilder().build();
		assertFalse(request.headers().firstValue(proxyAuthHeader).isPresent());

		// Test with unauthenticated proxy
		downloader.setProxy(ProxySettings.fromString("proxy:4567"));
		proxy = downloader.getHttpClient().proxy().orElse(null);
		assertNotNull(proxy);
		p = proxy.select(uri).get(0);
		assertEquals(new InetSocketAddress("proxy",4567), p.address());
		request = downloader.getRepositoryRequestBuilder().build();
		assertFalse(request.headers().firstValue(proxyAuthHeader).isPresent());
	}

	@Test
	void test(@TempDir Path dir) throws Exception {
		final URI uri = server.url(REPOSITORY_PATH).uri();
		final TestPluginDownloader downloader = new TestPluginDownloader(uri, dir);
		
		assertEquals(dir, downloader.getLocalDirectory());
		downloader.setPluginTypeWording("object");
		assertEquals("object",downloader.getPluginTypeWording());
		
		// Test getting remote plugins map is correct
		clearRequests();
		final Map<String, URI> map = downloader.getURIMap();
		assertEquals(new HashSet<>(Arrays.asList(VALID_PLUGIN_KEY,MISSING_JAR_PLUGIN_KEY)), map.keySet());
		RecordedRequest request = server.takeRequest();
		assertEquals(REPOSITORY_HEADER_VALUE,request.getHeader(CUSTOM_HEADER));
		assertNull(request.getHeader("Proxy-Authorization"));

		// Test load of a valid key
		Collection<Path> paths = downloader.download(VALID_PLUGIN_KEY);
		// check right paths are returned
		assertEquals(1, paths.size());
		final Path path = paths.iterator().next();
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		// Test downloaded list does not change if file is already loaded
		assertEquals(paths, downloader.download(VALID_PLUGIN_KEY));
		// Test downloadFile is not called if file exists
		TestPluginDownloader failDownloader = new TestPluginDownloader(uri, dir) {
			@Override
			protected void downloadFile(URI uri, Path path) throws IOException {
				throw new IllegalStateException("Should not be called");
			}
		};
		assertEquals(paths, failDownloader.download(VALID_PLUGIN_KEY));
		
		// Test load of a key missing in repository
		assertThrows(IllegalArgumentException.class, () -> downloader.download("Not in repository"));

		// Test load of a key in repository, but with missing jar
		assertThrows(IOException.class, () -> downloader.download(MISSING_JAR_PLUGIN_KEY));
		
		// Test load nothing doesn't throw exception
		assertTrue(downloader.download().isEmpty());
	}
	
	@Test
	void testEmptyDir(@TempDir Path dir) throws Exception {
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(server.url(REPOSITORY_PATH).uri(), dir);

		assertTrue(Files.deleteIfExists(dir), "Problem while deleting temp dir");
		downloader.clean(); // Test no exception is thrown
		final URI uri = server.url(PLUGINS_JAR_URI_PATH).uri();
		final Path path = downloader.getDownloadTarget(uri);
		downloader.downloadFile(uri, path);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
	}
	
	@Test
	void testDownloadAndClean(@TempDir Path dir) throws IOException, InterruptedException {
		final AbstractPluginsDownloader downloader = new TestPluginDownloader(server.url(REPOSITORY_PATH).uri(), dir);
		final URI existingURI = server.url(PLUGINS_JAR_URI_PATH).uri();
		clearRequests();
		Path path = downloader.getDownloadTarget(existingURI);
		downloader.downloadFile(existingURI, path);
		assertTrue(Files.isRegularFile(path));
		assertEquals(FAKE_JAR_FILE_CONTENT, Files.readAllLines(path).get(0));
		
		// Test already downloaded jar is not reloaded
		Path path2 = downloader.getDownloadTarget(existingURI);
		assertEquals(path, path2);
		assertFalse(downloader.shouldLoad(existingURI, path2));
		
		RecordedRequest request = server.takeRequest();
		assertEquals(JAR_HEADER_VALUE, request.getHeader(CUSTOM_HEADER));
		
		downloader.clean();
		assertFalse(Files.isRegularFile(path));

		final URI missingURI = server.url("/plugins/missing.jar").uri();
		final Path path3 = downloader.getDownloadTarget(missingURI);
		assertThrows(IOException.class, ()->downloader.downloadFile(missingURI, path3));
		assertFalse(Files.isRegularFile(path3));
	}

	private void clearRequests() throws InterruptedException {
		do {} while(server.takeRequest(100, TimeUnit.MILLISECONDS)!=null);
	}
}
