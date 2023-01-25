package com.fathzer.plugin.loader.example;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.example.api.AppPlugin;
import com.fathzer.plugin.loader.jar.JarPlugInContainer;
import com.fathzer.plugin.loader.jar.JarPluginLoader;

public class App {
	public static void main(String[] args) throws IOException {
		// First argument (if it exists), contains the folder that contains the plugins
		final File pluginRepository = args.length!=0 ? new File(args[0]) : new File("");
		// Second argument (if it exists), contains the search depth into the plugins folder
		final int depth = args.length>1 ? Integer.parseInt(args[1]) : 1;
		final JarPluginLoader loader = new JarPluginLoader();
		List<PlugInContainer<AppPlugin>> greetings = loader.getPlugins(pluginRepository, depth, AppPlugin.class);
		greetings.forEach(c -> {
			final File file = ((JarPlugInContainer<AppPlugin>)c).getFile();
			final AppPlugin p = c.get();
			if (p==null) {
				System.err.println("Unable to load plugin in file "+file+", error is "+c.getException());
			} else {
				System.out.println("Found plugin "+p.getClass()+" in file "+file+". It returns "+p.getGreeting());
			}
		});
		System.out.println("done");
		greetings.forEach(PlugInContainer::close);
	}
}
