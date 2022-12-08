package com.fathzer.plugin.loader.example;

import java.io.File;
import java.util.List;

import com.fathzer.plugin.loader.JarPlugInContainer;
import com.fathzer.plugin.loader.JarPluginLoader;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.example.api.AppPlugin;

public class App {
	public static void main(String[] args) {
		final File pluginRepository = args.length!=0 ? new File(args[0]) : new File("");
		final JarPluginLoader loader = new JarPluginLoader();
		List<PlugInContainer<AppPlugin>> greetings = loader.getPlugins(pluginRepository,AppPlugin.class);
		greetings.forEach(c -> {
			final File file = ((JarPlugInContainer<AppPlugin>)c).getFile();
			final AppPlugin p = c.get();
			if (p==null) {
				System.err.println("Unable to load plugin in file "+file+", error is "+c.getInstanciationException());
//				c.getInstanciationException().printStackTrace();
			} else {
				System.out.println("Found plugin "+p.getClass()+" in file "+file+". It returns "+p.getGreeting());
			}
		});
		System.out.println("done");
		greetings.forEach(PlugInContainer::close);
	}
}
