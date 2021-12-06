package org.w3id.steeld.xlsx2owl.utils.test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3id.steeld.xlsx2owl.utils.MappingUtils;

class MappingUtilsTest {
	
	@Test
	void testExpandIriPrefix() {
		@SuppressWarnings("serial")
		Map<String, String> prefixMapWithoutBase = new HashMap<String, String>() {{
				put("dc", "http://purl.org/dc/elements/1.1/");
				put("owl", "http://www.w3.org/2002/07/owl#");
		}};
		@SuppressWarnings("serial")
		Map<String, String> prefixMapWithBase = new HashMap<String, String>(prefixMapWithoutBase) {{
			put("base", "http://w3id.org/test/");
		}};
		
		testExpandIriPrefixHelper("dc:subject", "http://purl.org/dc/elements/1.1/subject", prefixMapWithBase);
		testExpandIriPrefixHelper("dc:", "http://purl.org/dc/elements/1.1/", prefixMapWithBase);
		
		testExpandIriPrefixHelper("subject", "http://w3id.org/test/subject", prefixMapWithBase, "with base");
		testExpandIriPrefixHelper("subject", "subject", prefixMapWithoutBase, "without base");
		
		testExpandIriPrefixHelper("", "", prefixMapWithoutBase, "without base");
		testExpandIriPrefixHelper("", "", prefixMapWithBase, "with base");
		
		testExpandIriPrefixHelper("subject", "subject", Collections.emptyMap(), "with empty prefix map");
		testExpandIriPrefixHelper("subject", "subject", null, "with missing (null) prefix map");
	}

	void testExpandIriPrefixHelper(String input, String expected, Map<String, String> prefixMap) {
		testExpandIriPrefixHelper(input, expected, prefixMap, "");
	}

	void testExpandIriPrefixHelper(String input, String expected, Map<String, String> prefixMap, String testSpec) {
		assertEquals(expected,
				MappingUtils.expandIriPrefix(input, prefixMap), "testing iri '" + input + "' " + testSpec);
	}

	@Test
	void testSplit() {
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.split("abc", ";").toArray(),
				"split with single entry 'abc'");
		assertArrayEquals(new String[]{"a", "b"},
				MappingUtils.split("a;b", ";").toArray(),
				"split with 2 entries 'a;b'");
		assertArrayEquals(new String[]{"a", "", "b"},
				MappingUtils.split("a;;b", ";").toArray(),
				"split with empty parts 'a;;b'");

		//testing null values
		assertArrayEquals(new String[]{},
				MappingUtils.split(null, ";").toArray(),
				"splitting null should return null");
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.split("abc", null).toArray(),
				"split 'abc' on null should return input");
//		assertThrows(Exception.class,
//				() -> MappingUtils.split("abc", ""),
//				"split 'abc' with empty separator '' should throw exception");
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.split("abc", "").toArray(),
				"split 'abc' on empty string '' should return input");
	}
	
	@Test
	void testReadInPrefixCsv() throws IOException {
		assertEquals("{owl=http://www.w3.org/2002/07/owl#, dc=http://purl.org/dc/elements/1.1/}",
				MappingUtils.readInPrefixCsv(new StringReader(
						"prefix,iri\r\n"
								+ "dc,http://purl.org/dc/elements/1.1/\r\n"
								+ "owl,http://www.w3.org/2002/07/owl#\r\n"
						)).toString(),
				"testing csv read in");
		assertEquals("{owl=http://www.w3.org/2002/07/owl#, dc=http://purl.org/dc/elements/1.1/}",
				MappingUtils.readInPrefixCsv(new FileReader("resources/prefixes.csv")).toString(),
				"testing csv read in from File");
	}

}
