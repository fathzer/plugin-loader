package com.fathzer.plugin.loader.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class PluginRegistryTest {
	private static interface Api extends Supplier<String>{};

	private static final class FakePlugin implements Api {
		private String key;

		private FakePlugin(String key) {
			super();
			this.key = key;
		}

		@Override
		public String get() {
			return key;
		}
	}

	@Test
	void test() {
		PluginRegistry<Api> registry = new PluginRegistry<>(Api::get);
		final Map<String, Api> loaded = registry.getLoaded();
		assertTrue(loaded.isEmpty());
		assertNull(registry.register(new FakePlugin("a")));
		assertFalse(loaded.isEmpty());
		assertNotNull(registry.register(new FakePlugin("a")));
		assertNull(registry.get("b"));
		assertNull(registry.register(new FakePlugin("b")));
		assertNotNull(registry.get("b"));
	}
}
