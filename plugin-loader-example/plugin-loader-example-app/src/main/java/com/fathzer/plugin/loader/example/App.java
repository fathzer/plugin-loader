package com.fathzer.plugin.loader.example;

import java.io.File;
import java.util.List;

import com.fathzer.plugin.loader.JarPlugInContainer;
import com.fathzer.plugin.loader.JarPlugInContainer.ClassNameBuilder;
import com.fathzer.plugin.loader.JarPlugInContainer.ManifestAttributeClassNameBuilder;
import com.fathzer.plugin.loader.PlugInContainer;
import com.fathzer.plugin.loader.example.api.AppPlugin;

public class App {
	private static final String PLUGIN_CLASS_ATTRIBUTE = "Plugin-Class";

	public static void main(String[] args) {
		final ClassNameBuilder builder = new ManifestAttributeClassNameBuilder(PLUGIN_CLASS_ATTRIBUTE);
		List<PlugInContainer<AppPlugin>> greetings = JarPlugInContainer.getPlugins(getPluginRepository(args),Integer.MAX_VALUE,AppPlugin.class,builder);
		greetings.forEach(c -> {
			final File file = ((JarPlugInContainer<AppPlugin>)c).getFile();
			final AppPlugin p = c.get();
			if (p==null) {
				System.err.println("Unable to load plugin in file "+file+", error is "+c.getInstanciationException());
			} else {
				System.out.println("Found plugin "+p.getClass()+" in file "+file+". It returns "+p.getGreeting());
			}
		});
		System.out.println("done");
		greetings.forEach(PlugInContainer::close);
	}
	
	private static final File getPluginRepository(String[] args) {
		return args.length!=0 ? new File(args[0]) : new File("");
	}
}
