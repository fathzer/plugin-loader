package com.fathzer.plugin.loader.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/** A class that loads plugins from an Internet remote repository.
 * <br><b>WARNING</b>: This class requires a Java 11+ JVM and is not available in java 8 distribution!
 */
@IgnoreJRERequirement
public abstract class AbstractPluginsDownloader {
	private final PluginRegistry<?> registry;
	private final URI uri;
	private final Path localDirectory;
	private ProxySettings proxy;
	private String pluginTypeWording = "plugin";
	
	private HttpClient httpClient;
	
	/** Constructor.
	 * @param registry The registry where plugins are loaded.
	 * @param uri The uri where to load the remote plugin registry.
	 * @param localDirectory The folder where plugins jar files will be loaded.
	 */
	protected AbstractPluginsDownloader(PluginRegistry<?> registry, URI uri, Path localDirectory) {
		if (registry==null) {
			throw new IllegalArgumentException("registry can't be null");
		}
		if (uri==null) {
			throw new IllegalArgumentException("uri can't be null");
		}
		if (localDirectory==null) {
			throw new IllegalArgumentException("local directory can't be null");
		}
		this.uri = uri;
		this.localDirectory = localDirectory;
		this.registry = registry;
	}
	
	/** Gets the remote plugin registry URI.
	 * @return The URI passed to the constructor.
	 */
	public URI getUri() {
		return uri;
	}
	
	/** Gets the plugin registry.
	 * @return The PluginRegistry passed to the constructor.
	 */
	public PluginRegistry<?> getRegistry() {
		return registry;
	}
	
	/** Gets the folder where plugins jar files are loaded.
	 * @return The Path passed to the constructor.
	 */
	public Path getLocalDirectory() {
		return localDirectory;
	}

	/** Sets the proxy used to connect with remote registry.
	 * @param proxy The proxy (null, which is the default, to use no proxy)
	 */
	public void setProxy(ProxySettings proxy) {
		this.proxy = proxy;
	}
	
	/** Sets the wording of plugins.
	 * <br>This information is used to generate human friendly error messages.
	 * @param wording The new wording (default is "plugin")
	 */
	public void setPluginTypeWording(String wording) {
		this.pluginTypeWording = wording;
	}

	/** Gets the wording of plugins.
	 * <br>This information is used to generate human friendly error messages.
	 * @return wording of the plugin (default is "plugin")
	 */
	protected String getPluginTypeWording() {
		return this.pluginTypeWording;
	}

	/** Deletes all files in local directory.
	 * @return true if loacalDirectory existed and is deleted.  
	 * @throws IOException If something went wrong
	 */
	public boolean clean() throws IOException {
		if (Files.isDirectory(localDirectory)) {
			try (Stream<Path> files = Files.find(localDirectory, 1, (p, bfa) -> bfa.isRegularFile())) {
				final List<Path> toDelete = files.collect(Collectors.toList());
				for (Path p : toDelete) {
					Files.delete(p);
				}
			}
			return true;
		}
		return false;
	}
	
	/** Search for plugins in remote registry, then loads them and verify they are not missing anymore.
	 * @param keys The plugin's keys to search
	 * @throws IOException If something went wrong
	 */
	public void load(String... keys) throws IOException {
		if (keys.length==0) {
			return;
		}
		final Map<String, URI> remoteRegistry = getURIMap();
		checkMissingKeys(Arrays.stream(keys), k -> !remoteRegistry.containsKey(k));
		final Set<URI> toDownload = Arrays.stream(keys).map(remoteRegistry::get).collect(Collectors.toSet());
		try {
			load(toDownload.stream().map(this::download).toArray(Path[]::new));
			checkMissingKeys(Arrays.stream(keys), s -> this.registry.get(s)==null);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
	
	/** Loads local plugins files downloaded from remote repository in the registry passed to the constructor. 
	 * @param paths The file to load in the registry
	 */
	protected abstract void load(Path[] paths);
	
	/** Gets the local path where a remote jar should be downloaded. 
	 * @param uri The uri of a remote jar
	 * @return a Path. Default value if a file in the local directory passed to the constructor, this the same filename as the uri.
	 */
	protected Path getDownloadTarget(URI uri) {
		return localDirectory.resolve(Paths.get(uri.getPath()).getFileName());
	}
	
	/** Tests whether a remote jar should be downloaded. 
	 * @param uri The uri of a remote jar
	 * @param path The local path where the file should be downloaded (the one returned by {@link #getDownloadTarget(URI)}
	 * @return true if the file should be loaded (default is the file is loaded if it does not exists in local directory.
	 */
	protected boolean shouldLoad(URI uri, Path path) {
		return !Files.exists(path);
	}

	/** Downloads an URI to a file.
	 * @param uri The uri to download
	 * @return The path where the URI body was downloaded.
	 * @throws UncheckedIOException if something went wrong
	 */
	protected Path download(URI uri) {
		final Path file = getDownloadTarget(uri);
		if (shouldLoad(uri, file)) {
			final HttpRequest.Builder requestBuilder = getRequestBuilder().uri(uri);
			customizeJarRequest(requestBuilder);
			final HttpRequest request = requestBuilder.build();
			try {
				if (!Files.exists(localDirectory)) {
					Files.createDirectories(localDirectory);
				}
				final BodyHandler<Path> bodyHandler = info -> info.statusCode() == 200 ? BodySubscribers.ofFile(file) : BodySubscribers.replacing(Paths.get("/NULL"));
				final HttpResponse<Path> response = getHttpClient().send(request, bodyHandler);
				if (response.statusCode()!=200) {
					throw new IOException(String.format("Unexpected status code %d received while downloading %s", response.statusCode(), uri));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new UncheckedIOException(new IOException(e));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return file;
	}
	
	/** Checks that plugins are registered in a map.
	 * @param keys The keys to check.
	 * @throws IllegalArgumentException if some keys are missing
	 */
	private void checkMissingKeys(Stream<String> keys, Predicate<String> missingDetector) {
		final Set<String> missing = keys.filter(missingDetector).collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(String.format("Unable to find the following %s: %s", pluginTypeWording, missing));
		}
	}
	
	/** Gets the content of the remote registry.
	 * <br>This method gets an input stream from the uri passed to this class constructor, then pass this input stream to {@link #getURIMap(InputStream)} and return its result.
	 * @return A key to uri map.
	 * @throws IOException If something went wrong
	 */
	protected Map<String, URI> getURIMap() throws IOException {
		final HttpRequest.Builder requestBuilder = getRequestBuilder();
		customizeRegistryRequest(requestBuilder);
		final HttpRequest request = requestBuilder.uri(uri).build();
		try {
			final HttpResponse<InputStream> response = getHttpClient().send(request, BodyHandlers.ofInputStream());
			if (response.statusCode()!=200) {
				throw new IOException(String.format("Unexpected status code %d received while downloading %s registry", response.statusCode(), pluginTypeWording));
			}
			try (InputStream in = response.body()) {
				return getURIMap(in);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}
	
	/** Allows sub-classes to customize the request used to query the registry.
	 * <br>For example, a sub-class can add headers to the request with this method.
	 * @param requestBuilder The request under construction.
	 */
	protected void customizeRegistryRequest(HttpRequest.Builder requestBuilder) {
		// Allows customization of request in sub-classes
	}

	/** Allows sub-classes to customize the request used to download a jar.
	 * <br>For example, a sub-class can add headers to the request with this method.
	 * @param requestBuilder The request under construction.
	 */
	protected void customizeJarRequest(HttpRequest.Builder requestBuilder) {
		// Allows customization of request in sub-classes
	}

	/** Gets the map that links a plugin key to the URI of a remote jar file from an InputStream 
	 * @param in An input stream on the remote repository URI 
	 * @return A map.
	 * @throws IOException If something went wrong while reading the input stream
	 */
	protected abstract Map<String, URI> getURIMap(InputStream in) throws IOException;

	private HttpRequest.Builder getRequestBuilder() {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				  .version(HttpClient.Version.HTTP_2)
				  .GET();
		if (proxy!=null && proxy.getLogin()!=null) {
			final String login = proxy.getLogin().getUserName()+":"+String.valueOf(proxy.getLogin().getPassword());
			final String encoded = new String(Base64.getEncoder().encode(login.getBytes()));
            builder.setHeader("Proxy-Authorization", "Basic " + encoded);
		}
		return builder;
	}

	/** Build the http client used to connect with the remote repository.
	 * @return An HTTPClient
	 */
	protected HttpClient getHttpClient() {
		if (httpClient==null) {
			final Builder clientBuilder = HttpClient.newBuilder();
			if (proxy!=null) {
				clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
			}
			clientBuilder.connectTimeout(Duration.ofSeconds(30));
			clientBuilder.followRedirects(Redirect.ALWAYS);
			this.httpClient = clientBuilder.build();
		}
		return httpClient;
	}
}
