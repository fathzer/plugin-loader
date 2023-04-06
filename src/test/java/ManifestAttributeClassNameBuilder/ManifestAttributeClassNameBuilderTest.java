package ManifestAttributeClassNameBuilder;

import static org.junit.jupiter.api.Assertions.*;

import static com.fathzer.plugin.loader.Constants.*;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.fathzer.plugin.loader.jar.ManifestAttributeClassNameBuilder;

class ManifestAttributeClassNameBuilderTest {

	@Test
	void test() throws IOException {
		final ManifestAttributeClassNameBuilder builder = new ManifestAttributeClassNameBuilder("attr");
		assertTrue(builder.get(EMPTY_FILE, getClass()).isEmpty());
		assertTrue(builder.get(EMPTY_MANIFEST, getClass()).isEmpty());
		assertThrows(IOException.class, () -> builder.get(KO_FILE, getClass()).isEmpty());
		assertThrows(IOException.class, () -> builder.get(Paths.get("src/test/resources/missing.jar"), getClass()));
	}

}
