![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/plugin-loader)
![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fathzer_plugin-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer_plugin-loader)
[![javadoc](https://javadoc.io/badge2/com.fathzer/plugin-loader/javadoc.svg)](https://javadoc.io/doc/com.fathzer/plugin-loader)

# plugin-loader
A java plugin loader.

A plugin is a class that is loaded dynamically by an application to give extra functionalities or customization.

From the technical point of view, the plugin should implement an interface (or extends a class) defined by the application.  
Usually, the plugin can be stored in a jar file, but you can imagine loading it from the network.

This library helps application developper's to manage plugins in their application.  
It provides an abstraction of the process of loading plugins and a concrete implementation to load plugins stored in jar files.

The [plugin-loader-example](https://github.com/fathzer/plugin-loader/tree/main/plugin-loader-example) folder contains an example of jar plugin implementation and loading.

It requires java 8+

## How to use the plugin loader with jar files

### First define an interface for your plugin.

In the example, it is a very basic interface, but it can be as complex as you need. It also can be an abstract class.  
It's a good practice to define this interface in a library different from the application it self, in order to clearly define the dependencies between application and its plugins.  
Here is the example:

```java
public interface AppPlugin {
    String getGreeting();
}
```

Usually there's also an implicit contract about how to instantiate the plugin. The default is to use the no arguments constructor.

### Then, implement the plugin.
Here is the code of the example plugin:

```java
public class MyPlugin implements AppPlugin {
    @Override
    public String getGreeting() {
        return "Hello, I'm a plugin";
    }
}
```

You should package the class in a jar file.  
As the plugin can be complex, the jar file can contains many classes. So, you have to define which class implements the plugin interface in a manifest attribute of the jar.  
By default *Plugin-Class* attribute is used. See [pom.xml of plugin example](https://github.com/fathzer/plugin-loader/blob/main/plugin-loader-example/plugin-loader-example-plugin/pom.xml) to view how to do it with Maven.

### Finally load the plugin in your application

The **JarPluginLoader** ([Javadoc is here](https://javadoc.io/doc/com.fathzer/plugin-loader)) class allow you to load the plugins contained in a local folder.

Here is an example:

```java
final JarPluginLoader loader = new JarPluginLoader();
// Loads all the plugins at first level inside the pluginRepository folder.
List<PlugInContainer<AppPlugin>> greetings = loader.getPlugins(pluginRepository, 1, AppPlugin.class);
greetings.forEach(c -> {
	// For each plugin
	final AppPlugin p = c.get();
	if (p==null) {
		// An error occurred while loading the plugin.
		final File file = ((JarPlugInContainer<AppPlugin>)c).getFile();
		System.err.println("Unable to load plugin in file "+file+", error is "+c.getException());
	} else {
		// The plugin was successfully loaded, use it.
		System.out.println("Found plugin "+p.getClass()+". It returns "+p.getGreeting());
	}
});
```

