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
It provides an abstraction of the process of loading plugins and concrete implementations to load plugins stored in jar files.  
Unlike [java.util.ServiceLoader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html), it allows to customize error management and how plugin classes are discovered and instantiated.

The [plugin-loader-example](https://github.com/fathzer/plugin-loader/tree/main/plugin-loader-example) folder contains an example of jar plugin implementation and loading.

It requires java 11+.  
Nevertheless, a variant of this library is available for Java 8 users. They have to use the 'jdk8' [maven classifier](https://www.baeldung.com/maven-artifact-classifiers#bd-3-consuming-jar-artifact-of-a-specific-java-version) in their dependency. Only the com.fathzer.plugin.loader.utils.AbstractPluginDownloader class is not available in this variant.

## How to load plugins from jar files

### First define an interface for your plugin.

In the example, it is a very basic interface, but it can be as complex as you need. It also can be an abstract class.  
It's a good practice to define this interface in a library different from the application it self, in order to clearly define the dependencies between application and its plugins.  
Here is the example:

```java
package com.myapp;
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
As the plugin can be complex, the jar file can contains many classes. So, you have to define which class implements the plugin interface. The standard way is to add a resource file in META-INF/services. In this example, its path should be META-INF/services/com.myapp.AppPlugin and it should contain the canonical name of every plugin implementation classes (see [ServiceLoader documentation](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) to have the exact format of the file).  

### Finally load the plugin in your application

com.fathzer.plugin.loader.jar.JarPluginLoader will allow you to get the plugin in your application.

Here is an example that loads the plugins contained in the *pluginFile* local jar file:

```java
final PluginLoader<Path> loader = new JarPluginLoader();
final List<AppPlugin> plugins = loader.getPlugins(pluginFile, AppPlugin.class);
```




## How to load plugins from ClassLoader
JarPluginLoader is not the only way to load plugins. *com.fathzer.plugin.loader.PluginLoader* is an abstract class that can have multiple implementations.  
Another classical implementation provided by this library is *ClassLoaderPluginLoader*.  
It allows you to search and load plugins through a class loader.

A typical use is to load the plugins available on the classpath. Here is the code to do that:
```java
final ClassLoaderPluginLoader loader = new ClassLoaderPluginLoader();
final List<AppPlugin> plugins = loader.getPlugins(AppPlugin.class);
```

## Working with custom plugins
The default implementation works with plugin defined as services useable with [ServiceLoader documentation](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html), but you can change how the plugins are discovered or how they are instantiated.

The plugin class names are discovered by a ClassNameBuilder. You can change the default one using the PluginLoader.withClassNameBuilder.  
Here is an example that use a predefined class name:
```java
final PluginLoader<ClassLoader> loader = new ClassLoaderPluginLoader();
loader.withClassNameBuilder((c,v) -> Collections.singleton("com.fathzer.MyPlugin"));
```

You can also change the way plugins are instantiated using the PluginLoader.withInstanceBuilder method. For instance to use a constructor with argument.  
Here is an example that uses a constructor with a fixed string argument:
```java
final PluginLoader<ClassLoader> loader = new ClassLoaderPluginLoader();
final String context = ...
InstanceBuilder ib = new InstanceBuilder() {
  @Override
  public <T> T get(Class<T> pluginClass) throws Exception {
    final Constructor<T> constructor = pluginClass.getConstructor(String.class);
    return constructor.newInstance(context);
  }
};
loader.withInstanceBuilder(ib);
```

## A word about error management
If a problem occurs during plugin instantiation, a *PluginInstantiationException* is throw. This is the default behaviour, but you prefer to log the error and continue to instantiate other plugins contained in a jar.  
You can simply customize the exception management using the *PluginLoader.withExceptionConsumer* method as in the following example:
```java
new ClassLoaderPluginLoader().withExceptionConsumer(e -> log.warn("An error occured while loading plugins", e));
```

#TODO
- Get jar files in a directory
- Plugin registry
