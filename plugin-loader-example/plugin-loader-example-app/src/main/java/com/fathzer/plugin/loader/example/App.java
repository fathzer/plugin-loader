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
		List<PlugInContainer<AppPlugin>> greetings = JarPlugInContainer.getPlugins(new File("../plugin-loader-example-plugin/target"),AppPlugin.class,builder);
		greetings.forEach(c -> {
			final AppPlugin p = c.get();
			if (p==null) {
				System.out.println("Unable to load plugin, error is ");
				c.getInstanciationException().printStackTrace(System.out);
			}
			System.out.println(p.getClass()+": "+p.getGreeting());
		});
		System.out.println("done");
		greetings.forEach(PlugInContainer::close);
	}
}
