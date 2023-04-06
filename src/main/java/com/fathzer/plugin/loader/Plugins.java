package com.fathzer.plugin.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The result of the plugins loading process.
 * @param <T> The interface of the plugin.
 */
public class Plugins<T> {
	private Class<T> aClass;
	private List<T> instances;
	private List<PluginInstantiationException> exceptions;
	
	/** Constructor.
	 * @param aClass The class of T
	 */
	public Plugins (Class<T> aClass) {
		this.aClass = aClass;
		this.instances = new ArrayList<>();
		this.exceptions = new ArrayList<>();
	}
	
	/** Tries to add a new instance from its class name.
	 * <br>Its something goes wrong during instantiation, an exception is added to the exceptions list,
	 * else, a new instance is added to the instances list.
	 * @param loader The class loader to use to retrieve the class.
	 * @param className The name of the class
	 * @param instanceBuilder An {@link InstanceBuilder} used to instantiate the class from its {@link Class}
	 * @see #getExceptions()
	 * @see #getInstances()
	 */
	@SuppressWarnings("unchecked")
	public void add(ClassLoader loader, String className, InstanceBuilder instanceBuilder) {
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

	/** Adds the content of another plugins instance to this.
	 * @param plugins another instance
	 */
	public void add(Plugins<T> plugins) {
		this.exceptions.addAll(plugins.exceptions);
		this.instances.addAll(plugins.instances);
	}
}
