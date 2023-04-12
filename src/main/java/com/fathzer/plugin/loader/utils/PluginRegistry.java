package com.fathzer.plugin.loader.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A class to manage plugins identified by a key String.
 * <br>This class is not thread safe
 * @param <T> The class of the plugins. 
 */
public class PluginRegistry<T> {
	private final Map<String, T> pluginsMap;
	private final Function<T, String> keyFunction;
	
	/** Constructor.
	 * <br>The built registry is empty.
	 * @param keyFunction A function that get the plugin's key.
	 */
	public PluginRegistry(Function<T, String> keyFunction) {
		this.pluginsMap = new HashMap<>();
		this.keyFunction = keyFunction;
	}
	
	/** Register a plugin.
	 * @param plugin The plugin to register.
	 * @return The plugin previously registered for the same key. Null if no plugin was registered for that key.
	 */
	public T register(T plugin) {
		return pluginsMap.put(keyFunction.apply(plugin), plugin);
	}
	
	/** Register a plugins collection.
	 * @param plugins The plugins to register.
	 * @return The plugins that were not previously registered for the same key.<br>
	 * Please note that if <i>plugins</i> contains a new instance of a class already registered with the same key,
	 * the instance will replace the previous one in the registry, but will not be returned.  
	 */
	public List<T> registerAll(Collection<T> plugins) {
		return plugins.stream().filter(p -> {
			final T old = register(p);
			return old==null || !p.getClass().equals(old.getClass());
		}).collect(Collectors.toList());
	}
	
	/** Unregister the plugin registered with a key.
	 * @param key The key that was registered.
	 * @return The plugin that was registered with that key. Null if the key is unknown.
	 */
	public T unregister(String key) {
		return pluginsMap.remove(key);
	}
	
	/** Gets a plugin by its key.
	 * @param key The plugin's key
	 * @return The plugin or null if the plugin does not exists.
	 */
	public T get(String key) {
		return pluginsMap.get(key);
	}
	
	/** Gets all available plugins.
	 * @return An unmodifiable map. Please note the returned map will reflect changes in this registry. If a plugin is added, it will appear in the returned map.
	 */
	public Map<String, T> getLoaded() {
		return Collections.unmodifiableMap(pluginsMap);
	}
}
