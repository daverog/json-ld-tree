package daverog.jsonld.tree;

import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class TestResourceLoader {

	public static String loadClasspathResourceAsString(String filename) {
		try {
			return IOUtils.toString(ClassLoader.getSystemResourceAsStream(filename), "UTF-8");
		}
		catch (Exception e) {
			fail("Could not load resource from classpath '" + filename + "': " + e.getMessage());
			return "";
		}
	}

	public static InputStream loadClasspathResourceAsStream(String filename) {
		try {
			return ClassLoader.getSystemResourceAsStream(filename);
		}
		catch (Exception e) {
			fail("Could not load resource from classpath '" + filename + "': " + e.getMessage());
			return null;
		}
	}

}
