package com.fathzer.plugin.loader.test;

import java.util.function.Supplier;

public class Plugin implements Supplier<String> {
	private String content = "Hello";
	
	public Plugin() {
		this("Hello");
	}
	
	public Plugin(String content) {
		if (content==null) {
			throw new IllegalArgumentException();
		}
		this.content = content;
	}
	
	@Override
	public String get() {
		return content;
	}
}
