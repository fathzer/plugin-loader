package com.fathzer.plugin.loader;

public class PluginInstantiationException extends Exception {
	private static final long serialVersionUID = 1L;

	public PluginInstantiationException(String message) {
		super(message);
	}

	public PluginInstantiationException(Throwable cause) {
		super(cause);
	}
}
