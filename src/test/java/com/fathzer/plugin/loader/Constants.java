package com.fathzer.plugin.loader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
	public static final Path PLUGINS_FOLDER = Paths.get("src/test/resources");
	public static final Path OK_FILE = PLUGINS_FOLDER.resolve("plugin-loader-test-plugin-0.0.1.jar");
	public static final Path KO_FILE = PLUGINS_FOLDER.resolve("other/wrongFormat.jar");

}
