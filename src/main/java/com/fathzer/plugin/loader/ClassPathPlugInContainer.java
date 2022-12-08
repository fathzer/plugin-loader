package com.fathzer.plugin.loader;

public class ClassPathPlugInContainer<T> implements PlugInContainer<T> {
	private T plugin;
	
	public ClassPathPlugInContainer(T plugin) {
		this.plugin = plugin;
	}

	@Override
	public T get() {
		return plugin;
	}
	
	@Override
	public String toString() {
		return this.plugin.getClass().getCanonicalName();
	}

	public PluginInstantiationException getException() {
		return null;
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
