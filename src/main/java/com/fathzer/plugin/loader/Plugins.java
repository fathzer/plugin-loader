package com.fathzer.plugin.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The result of the plugins loading process.
 * @param <T> The interface of the plugin.
 */
public class Plugins<T> {
	private List<T> instances;
	private List<PluginInstantiationException> exceptions;
	
	/** Constructor.
	 */
	public Plugins () {
		this.instances = new ArrayList<>();
		this.exceptions = new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	public void add(ClassLoader loader, String className, Class<T> aClass, InstanceBuilder instanceBuilder) {
		try {
			final Class<?> pluginClass = loader.loadClass(className);
			if (aClass.isAssignableFrom(pluginClass)) {
				this.instances.add(instanceBuilder.get((Class<T>) pluginClass));
			} else {
				this.exceptions.add(new PluginInstantiationException(className+" is not a "+aClass.getCanonicalName()+" instance"));
			}
		} catch (Exception e) {
			this.exceptions.add(new PluginInstantiationException(e));
		}
	}
	
	/** Adds a plugin.
	 * @param instance a plugin instance
	 */
	public void add(T instance) {
		instances.add(instance);
	}

	//TODO Remove?
	void add(PluginInstantiationException exception) {
		exceptions.add(exception);
	}

	/** Gets the successfully loaded instances.
	 * @return an unmodifiable list of instances
	 */
	public List<T> getInstances() {
		return Collections.unmodifiableList(instances);
	}

	/** Gets the errors that occured during plugin loading process.
	 * @return an unmodifiable list of exceptions
	 */
	public List<PluginInstantiationException> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}
	
	/** Tests whether this instance is empty.
	 * @return true if this contains no instances and no exceptions.
	 */
	public boolean isEmpty() {
		return instances.isEmpty() && exceptions.isEmpty();
	}
}
