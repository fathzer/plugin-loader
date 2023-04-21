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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/** A class that downloads plugins from an Internet remote repository to a local folder.
 * <br><b>WARNING</b>: This class requires a Java 11+ JVM and is not available in java 8 distribution!
 */
@IgnoreJRERequirement
public abstract class AbstractPluginsDownloader {
	private final URI uri;
	private final Path localDirectory;
	private ProxySettings proxy;
	private String pluginTypeWording = "plugin";
	
	private HttpClient httpClient;
	
	/** Constructor.
	 * @param uri The uri where to load the remote plugin .
	 * @param localDirectory The folder where plugins jar files will be loaded.
	 */
	protected AbstractPluginsDownloader(URI uri, Path localDirectory) {
		if (uri==null) {
			throw new IllegalArgumentException("uri can't be null");
		}
		if (localDirectory==null) {
			throw new IllegalArgumentException("local directory can't be null");
		}
		this.uri = uri;
		this.localDirectory = localDirectory;
	}
	
	/** Gets the remote plugin repository URI.
	 * @return The URI passed to the constructor.
	 */
	protected URI getUri() {
		return uri;
	}
	
	/** Gets the folder where plugins jar files are loaded.
	 * @return The Path passed to the constructor.
	 */
	public Path getLocalDirectory() {
		return localDirectory;
	}

	/** Sets the proxy used to connect with remote repository.
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
	
	/** Searches for plugin keys in remote repository, then downloads the corresponding jars.
	 * @param keys The plugin's keys to search
	 * @throws IOException If something went wrong
	 * @return The paths of files that contains the jars (including the ones for which {@link #shouldLoad(URI, Path)} returned false
	 */
	public Collection<Path> download(String... keys) throws IOException {
		if (keys.length==0) {
			return Collections.emptyList();
		}
		final Map<String, URI> remoteRepository = getURIMap();
		checkMissingKeys(Arrays.stream(keys), k -> !remoteRepository.containsKey(k));
		final Set<URI> toDownload = Arrays.stream(keys).map(remoteRepository::get).collect(Collectors.toSet());
		final List<Path> paths = new ArrayList<>(toDownload.size());
		for (URI current : toDownload) {
			final Path file = getDownloadTarget(current);
			paths.add(file);
			if (shouldLoad(current, file)) {
				downloadFile(current, file);
			}
		}
		return paths;
	}
	
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
	 * @throws IOException if something went wrong
	 */
	protected void downloadFile(URI uri, Path path) throws IOException {
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
	private void checkMissingKeys(Stream<String> keys, Predicate<String> missingDetector) {
		final Set<String> missing = keys.filter(missingDetector).collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(String.format("Unable to find the following %s in remote repository: %s", pluginTypeWording, missing));
		}
	}
	
	/** Gets the content of the remote repository.
	 * <br>This method gets an input stream from the uri passed to this class constructor, then pass this input stream to {@link #getURIMap(InputStream)} and return its result.
	 * @return A key to uri map.
	 * @throws IOException If something went wrong
	 */
	public Map<String, URI> getURIMap() throws IOException {
		final HttpRequest request = getRepositoryRequestBuilder().build();
		final HttpResponse<InputStream> response = call(request, BodyHandlers.ofInputStream());
		if (response.statusCode()!=200) {
			throw new IOException(String.format("Unexpected status code %d received while downloading %s repository", response.statusCode(), pluginTypeWording));
		}
		try (InputStream in = response.body()) {
			return getURIMap(in);
		}
	}
	
	/** Gets the builder of the request used to query the repository.
	 * <br>A sub-class can override this method to, for example, add headers to the request.
	 * @return a request builder that build the request.
	 */
	protected HttpRequest.Builder getRepositoryRequestBuilder() {
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
            builder.setHeader("Proxy-Authorization", "Basic " + proxy.getBase64Login());
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
