package com.fathzer.plugin.loader.classloader;

import java.util.function.Supplier;

public class MySupplier implements Supplier<String> {
	@Override
	public String get() {
		return "hi";
	}
}