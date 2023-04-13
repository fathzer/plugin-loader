package com.fathzer.plugin.loader.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
	
	private static final class OtherFakePlugin implements Api {
		@Override
		public String get() {
			return "a";
		}
	}

	@Test
	void test() {
		PluginRegistry<Api> registry = new PluginRegistry<>(Api::get);
		final Map<String, Api> loaded = registry.getRegistered();
		assertTrue(loaded.isEmpty());
		assertNull(registry.register(new FakePlugin("a")));
		assertFalse(loaded.isEmpty());
		assertNotNull(registry.register(new FakePlugin("a")));
		assertNull(registry.get("b"));
		assertNull(registry.register(new FakePlugin("b")));
		assertNotNull(registry.get("b"));
		
		assertEquals(new HashSet<>(Arrays.asList("a","b")), registry.getRegistered().keySet());
		final OtherFakePlugin otherFakePlugin = new OtherFakePlugin();
		final FakePlugin bFakePlugin = new FakePlugin("b");
		final FakePlugin cFakePlugin = new FakePlugin("c");
		final List<Api> newOnes = registry.registerAll(Arrays.asList(otherFakePlugin, bFakePlugin, cFakePlugin));
		assertEquals(2, newOnes.size());
		assertTrue(newOnes.contains(otherFakePlugin));
		assertTrue(newOnes.contains(cFakePlugin));
		assertFalse(newOnes.contains(bFakePlugin));
		
		assertEquals(cFakePlugin, registry.unregister("c"));
		assertNull(registry.get("c"));
	}
}
