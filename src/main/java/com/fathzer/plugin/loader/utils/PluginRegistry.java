package com.fathzer.plugin.loader.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
