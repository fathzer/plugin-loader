package com.fathzer.plugin.loader;

import java.lang.reflect.Constructor;

/** A class that can create instances of another class.
 */
public interface InstanceBuilder {
	/** A default instance builder that calls the no arg public constructor of classes to build instances.
	 */
	InstanceBuilder DEFAULT = new InstanceBuilder() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> pluginClass) throws ReflectiveOperationException, SecurityException {
			final Constructor<?> constructor = pluginClass.getConstructor();
			return (T) constructor.newInstance();
		}
	};

	/** Instantiates a new instance.
	 * @param <T> The class of the returned instance.
	 * @param aClass The class of the instance to create.
	 * @return A new instance of aClass.
	 * @throws Exception If something went wrong
	 */
	<T> T get(Class<T> aClass) throws Exception;
}