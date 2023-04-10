package com.fathzer.plugin.loader.utils;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/** A class that loads plugins from an Internet remote repository.
 * <br><b>WARNING</b>: This class requires a Java 11+ JVM and is not available in java 8 distribution!
 * @param <T> The plugins type
 */
@IgnoreJRERequirement
public abstract class AbstractPluginsDownloader<T> {
	private final PluginRegistry<T> registry;
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
	protected AbstractPluginsDownloader(PluginRegistry<T> registry, URI uri, Path localDirectory) {
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
	protected URI getUri() {
		return uri;
	}
	
	/** Gets the plugin registry.
	 * @return The PluginRegistry passed to the constructor.
	 */
	protected PluginRegistry<T> getRegistry() {
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
		this.httpClient = null;
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
	
	/** Search for plugins in remote repository, then loads them and verify they are not missing anymore.
	 * @param keys The plugin's keys to search
	 * @throws IOException If something went wrong
	 */
	public void load(String... keys) throws IOException {
		if (keys.length==0) {
			return;
		}
		final Map<String, URI> remoteRegistry = getURIMap();
		checkMissingKeys(Arrays.stream(keys), k -> !remoteRegistry.containsKey(k)," remote repository");
		final Set<URI> toDownload = Arrays.stream(keys).map(remoteRegistry::get).collect(Collectors.toSet());
		final List<Path> paths = new ArrayList<>(toDownload.size());
		for (URI current : toDownload) {
			final Path file = getDownloadTarget(current);
			paths.add(file);
			if (shouldLoad(uri, file)) {
				download(current, file);
			}
		}
		load(paths);
		checkMissingKeys(Arrays.stream(keys), s -> this.registry.get(s)==null," loaded plugins");
	}
	
	/** Loads local plugins files downloaded from remote repository in the registry passed to the constructor. 
	 * @param paths The file to load in the registry
	 * @throws IOException If something went wrong
	 */
	protected abstract void load(Collection<Path> paths) throws IOException;
	
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
	 * @param path The local path where the file should be downloaded (the one returned by {@link #getDownloadTarget(URI)}.
	 * There's no guarantee that the directory that contains path is created. If not, this method should create it.
	 * @return The path where the URI body was downloaded.
	 * @throws IOException if something went wrong
	 */
	protected void download(URI uri, Path path) throws IOException {
		final Path parent = path.getParent();
		if (!Files.exists(parent)) {
			Files.createDirectories(parent);
		}
		final HttpRequest request = getJarRequestBuilder(uri).build();
		final BodyHandler<Path> bodyHandler = info -> info.statusCode() == 200 ? BodySubscribers.ofFile(path) : BodySubscribers.replacing(Paths.get("/NULL"));
		final HttpResponse<Path> response = call(request, bodyHandler);
		if (response.statusCode()!=200) {
			throw new IOException(String.format("Unexpected status code %d received while downloading %s", response.statusCode(), uri));
		}
	}
	
	private <V> HttpResponse<V> call(HttpRequest request, BodyHandler<V> handler) throws IOException {
		try {
			return getHttpClient().send(request, handler);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}
	
	/** Checks that plugins are registered in a map.
	 * @param keys The keys to check.
	 * @throws IllegalArgumentException if some keys are missing
	 */
	private void checkMissingKeys(Stream<String> keys, Predicate<String> missingDetector, String container) {
		final Set<String> missing = keys.filter(missingDetector).collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(String.format("Unable to find the following %s in %s: %s", pluginTypeWording, container, missing));
		}
	}
	
	/** Gets the content of the remote registry.
	 * <br>This method gets an input stream from the uri passed to this class constructor, then pass this input stream to {@link #getURIMap(InputStream)} and return its result.
	 * @return A key to uri map.
	 * @throws IOException If something went wrong
	 */
	protected Map<String, URI> getURIMap() throws IOException {
		final HttpRequest request = getRegistryRequestBuilder().build();
		final HttpResponse<InputStream> response = call(request, BodyHandlers.ofInputStream());
		if (response.statusCode()!=200) {
			throw new IOException(String.format("Unexpected status code %d received while downloading %s registry", response.statusCode(), pluginTypeWording));
		}
		try (InputStream in = response.body()) {
			return getURIMap(in);
		}
	}
	
	/** Gets the builder of the request used to query the registry.
	 * <br>A sub-class can override this method to, for example, add headers to the request.
	 * @return a request builder that build the request.
	 */
	protected HttpRequest.Builder getRegistryRequestBuilder() {
		return getRequestBuilder().uri(uri);
	}

	/** Gets the builder of the request used to download a jar.
	 * <br>A sub-class can override this method to, for example, add headers to the request.
	 * @param uri The jar's uri.
	 * @return a request builder that build the request.
	 */
	protected HttpRequest.Builder getJarRequestBuilder(URI uri) {
		return getRequestBuilder().uri(uri);
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
