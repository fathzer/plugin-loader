package com.fathzer.plugin.loader.utils;

import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Proxy.Type;

/** A class that represents a proxy setting.
 */
public class ProxySettings {
	private String host;
	private int port;
	private PasswordAuthentication login;
	
	private ProxySettings() {
		// To make it compatible with jackson databind deserialization
	}
	
	/** Constructor.
	 * @param host The proxy's host name
	 * @param port The proxy's port
	 * @param login The login used for authentication (null if no authentication)
	 */
	public ProxySettings(String host, int port, PasswordAuthentication login) {
		this.host = host;
		this.port = port;
		this.login = login;
	}

	/** Creates a proxy setting from a string.
	 * @param proxy The address in the format [user:[password]@]host:port 
	 * @return A proxy setting or null if argument is null or empty
	 * @throws IllegalArgumentException if argument is incorrect
	 */
	public static ProxySettings fromString(String proxy) {
		if (proxy==null || proxy.trim().isEmpty()) {
			return null;
		}
		final ProxySettings result = new ProxySettings();
		try {
			final URI uri = new URI("http://"+proxy);
			result.port = uri.getPort();
			if (result.port<=0) {
				throw new IllegalArgumentException("missing port");
			}
			result.host = uri.getHost();
			if (result.host==null) {
				throw new IllegalArgumentException("missing host");
			}
			result.login = LoginParser.fromString(uri.getUserInfo());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("argument should be of the form [user:pwd@]host:port",e);
		}
		return result;
	}

	/** Converts this to a java.net.Proxy instance.
	 * @return a Proxy instance
	 */
	public Proxy toProxy() {
		return new Proxy(Type.HTTP, new InetSocketAddress(getHost(), getPort()));
	}

	/** Gets the proxy's host name.
	 * @return a string
	 */
	public String getHost() {
		return host;
	}

	/** Gets the proxy's port.
	 * @return a integer
	 */
	public int getPort() {
		return port;
	}

	/** Gets the proxy's user login.
	 * @return a string or null if no authentication is set
	 */
	public PasswordAuthentication getLogin() {
		return login;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (this.login!=null) {
			builder.append(LoginParser.toString(this.login));
			builder.append('@');
		}
		builder.append(this.host).append(':').append(this.port);
		return builder.toString();
	}
}
