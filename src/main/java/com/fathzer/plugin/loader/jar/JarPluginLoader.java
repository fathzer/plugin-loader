package com.fathzer.plugin.loader.jar;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import com.fathzer.plugin.loader.PluginLoader;
import com.fathzer.plugin.loader.ClassNameBuilder;
import com.fathzer.plugin.loader.InstanceBuilder;

/** A class able to load plugins from jar files contained in a folder.
 */
public class JarPluginLoader extends PluginLoader<Path> {
	/** Constructor.
	 * <br>By default, the class name of the plugins are searched with a {@link ServiceClassNameBuilder}.
	 * <br>The plugins are instantiated using their public no argument constructor.
	 * <br>This makes the default behaviour quite similar to {@link java.util.ServiceLoader}
	 * @see #withClassNameBuilder(ClassNameBuilder)
	 * @see #withInstanceBuilder(InstanceBuilder)
	 */
	public JarPluginLoader() {
		super(ServiceClassNameBuilder.INSTANCE);
	}
	
	/** Builds the classloader that will be used to load the plugin classes.
	 * <br>The default implementation returns a {@link URLClassLoader} on the <i>jarFile</i>'s url.
	 * <br>You may override this method if you want to change this behaviour.
	 * @param context the jar file passed to {@link #getPlugins(Path, Class)}
	 * @return A classloader.  
	 * @throws IOException if something went wrong
	 */
	@Override
	protected ClassLoader buildClassLoader(Path jarFile) throws IOException {
		return new URLClassLoader(new URL[]{jarFile.toUri().toURL()});
	}
}
