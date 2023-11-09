package org.w3id.steel.xlsx2owl.utils.test;

//import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.LogManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;
import org.w3id.steel.xlsx2owl.utils.MappingUtils;

import be.ugent.idlab.knows.functions.agent.Agent;
import be.ugent.idlab.knows.functions.agent.AgentFactory;
import be.ugent.idlab.knows.functions.agent.Arguments;
import be.ugent.idlab.knows.functions.agent.functionModelProvider.fno.exception.FnOException;

class MappingUtilsTest {
	
	@Test
	void testExpandIriPrefix() throws IOException {
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
		
		//** test execution with mapping from file
		assertEquals("http://purl.org/dc/elements/1.1/subject", MappingUtils.expandIriPrefix("dc:subject"));
	}
	
	@Test
	void testSplitAndExpandIriPrefixes() throws IOException {		
		assertArrayEquals(
				new String[] {"http://purl.org/dc/elements/1.1/subject"},
				MappingUtils.splitAndExpandIriPrefixes("dc:subject", ";").toArray()
				);
		
		assertArrayEquals(
				new String[] {"http://purl.org/dc/elements/1.1/subject", "http://purl.org/dc/elements/1.1/title"},
				MappingUtils.splitAndExpandIriPrefixes("dc:subject;dc:title", ";").toArray()
				);
		
		assertArrayEquals(
				new String[] {},
				MappingUtils.splitAndExpandIriPrefixes(null, ";").toArray()
				);
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
	void testSplitAndTrim() {
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.splitAndTrim("abc", ";").toArray(),
				"split with single entry 'abc'");
		assertArrayEquals(new String[]{"a", "b"},
				MappingUtils.splitAndTrim("a;b", ";").toArray(),
				"split with 2 entries 'a;b'");
		assertArrayEquals(new String[]{"a", "", "b"},
				MappingUtils.splitAndTrim("a;;b", ";").toArray(),
				"split with empty parts 'a;;b'");
		assertArrayEquals(new String[]{"a", "", "b"},
				MappingUtils.splitAndTrim("a ;; b", ";").toArray(),
				"split with empty parts and spaces 'a ;; b'");

		//testing null values
		assertArrayEquals(new String[]{},
				MappingUtils.splitAndTrim(null, ";").toArray(),
				"splitting null should return null");
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.splitAndTrim("abc", null).toArray(),
				"split 'abc' on null should return input");
//		assertThrows(Exception.class,
//				() -> MappingUtils.split("abc", ""),
//				"split 'abc' with empty separator '' should throw exception");
		assertArrayEquals(new String[]{"abc"},
				MappingUtils.splitAndTrim("abc", "").toArray(),
				"split 'abc' on empty string '' should return input");
	}

	@Test
	void testContains() {
		//testing normal cases
		assertTrue(MappingUtils.contains("abc", "b"), "'abc' should contain 'b'");	
		assertTrue(MappingUtils.contains("abc", "a"), "'abc' should contain 'a'");
		assertTrue(MappingUtils.contains("abc", "c"), "'abc' should contain 'c'");
		assertTrue(MappingUtils.contains("abc", ""), "'abc' should contain empty string ''");
		assertFalse(MappingUtils.contains("abc", "d"), "'abc' should not contain 'd'");

		//testing null values
		assertFalse(MappingUtils.contains(null, "a"), "null string should contain nothing");
		assertTrue(MappingUtils.contains("abc", null), "every string should contain null");	
	}

	@Test
	void testNotContains() {
		//testing normal cases
		assertFalse(MappingUtils.notContains("abc", "b"), "'abc' should not notContain 'b'");	
		assertFalse(MappingUtils.notContains("abc", "a"), "'abc' should not notContain 'a'");
		assertFalse(MappingUtils.notContains("abc", "c"), "'abc' should not notContain 'c'");
		assertFalse(MappingUtils.notContains("abc", ""), "'abc' should not notContain empty string ''");
		assertTrue(MappingUtils.notContains("abc", "d"), "'abc' should notContain 'd'");

		//testing null values
		assertTrue(MappingUtils.notContains(null, "a"), "null string should not notContain nothing");
		assertFalse(MappingUtils.notContains("abc", null), "every string should not notContain null");	
	}

	// @Test
	// void testNot() {
	// 	assertFalse(MappingUtils.not(true), "not true should be false");
	// 	assertTrue(MappingUtils.not(false), "not false should be true");
	// 	assertFalse(MappingUtils.not(null), "not 'null' should be false");
	// }
	
	@Test
	void testReadInPrefixCsv() throws IOException {
		assertEquals("{dc=http://purl.org/dc/elements/1.1/, owl=http://www.w3.org/2002/07/owl#}",
				MappingUtils.readInPrefixCsv(new StringReader(
						"prefix,iri\r\n"
								+ "dc,http://purl.org/dc/elements/1.1/\r\n"
								+ "owl,http://www.w3.org/2002/07/owl#\r\n"
						)).toString(),
				"testing csv read in");
		assertEquals("{dc=http://purl.org/dc/elements/1.1/, owl=http://www.w3.org/2002/07/owl#}",
				MappingUtils.readInPrefixCsv(new FileReader("resources/prefixes.csv")).toString(),
				"testing csv read in from File");
		assertEquals("{dc=http://purl.org/dc/elements/1.1/"
				+ ", owl=http://www.w3.org/2002/07/owl#"
				+ ", rdfs=http://www.w3.org/2000/01/rdf-schema#"
				+ ", rdf=http://www.w3.org/1999/02/22-rdf-syntax-ns#"
				+ "}",
				MappingUtils.readInPrefixCsv(new FileReader("src/test/resources/prefixes.csv")).toString(),
				"testing csv read in from File with multiple separators");
	}
	
	@Test
	void testEpochToIso8601() throws IOException {
		long epoch = 1673359547L;
		
		//set default time zone used in test to get independent of runtime
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
		assertEquals("Europe/Berlin",TimeZone.getDefault().getID());
		
		assertEquals("2023-01-10T15:05:47Z", MappingUtils.epochToIso8601(epoch));
		assertEquals("2023-01-10T15:05:47Z", MappingUtils.epochToIso8601(epoch * 1000));
	}
	
	@Test
	void testFnoInvocation() throws Exception {
		// see https://github.com/FnOio/function-agent-java#example

		//// enable debug log for better errors
		//org.apache.logging.log4j.LogManager.getRootLogger();
		//Configurator.setAllLevels(org.apache.logging.log4j.LogManager.getRootLogger().getName(), Level.getLevel("Debug"));
		
		Agent agent = AgentFactory.createFromFnO("resources/functions_xlsx2owl.ttl", "resources/grelTypes.ttl");
		// prepare the parameters for the function
	    Arguments arguments = new Arguments()
	        .add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", "dc:subject");
	    // execute the function
	    String result = (String)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#expandPrefix", arguments);
	    assertEquals("http://purl.org/dc/elements/1.1/subject", result);
	    
	    // check function expandPrefixes
	    arguments = new Arguments()
	    		.add("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#p_array", "dc:subject");
	    List<String>resultList = (List<String>)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#expandPrefixes", arguments);
	    assertEquals(Arrays.asList("http://purl.org/dc/elements/1.1/subject"), resultList);
	    
	    // check function split
	    arguments = new Arguments()
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", "a;b;c")
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#p_string_sep", ";");
	    //assertEquals("('http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter' -> 'a;b,c')('http://users.ugent.be/~bjdmeest/function/grel.ttl#param_string_sep' -> ';,')",arguments.toString());
	    resultList = (List<String>)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#split", arguments);
	    assertEquals(Arrays.asList("a", "b", "c"), resultList);

	    // check function splitAndTrim
	    arguments = new Arguments()
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", " a; b ;c")
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#p_string_sep", ";");
	    resultList = (List<String>)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#splitAndTrim", arguments);
	    assertEquals(Arrays.asList("a", "b", "c"), resultList);
	    
	    // check function splitAndExpandPrefixes
	    arguments = new Arguments()
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", "dc:subject; dc:title")
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#p_string_sep", ";");
	    resultList = (List<String>)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#splitAndExpandPrefixes", arguments);
	    assertEquals(Arrays.asList("http://purl.org/dc/elements/1.1/subject", "http://purl.org/dc/elements/1.1/title"), resultList);

		//check function contains
		arguments = new Arguments()
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", "abc")
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#p_string_sep", "b");
		Boolean resultBool = (Boolean)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#contains", arguments);
		assertTrue(resultBool);

		//check function notContains
		arguments = new Arguments()
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParameter", "abc")
	    		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#p_string_sep", "b");
		resultBool = (Boolean)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#notContains", arguments);
		assertFalse(resultBool);

		//check function not
		// arguments = new Arguments()
	    // 		.add("http://users.ugent.be/~bjdmeest/function/grel.ttl#param_b", Boolean.FALSE);
		// resultBool = (Boolean)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#boolean_not", arguments);
		// //resultBool = (Boolean)agent.execute("http://users.ugent.be/~bjdmeest/function/grel.ttl#boolean_not", arguments);
		// assertTrue(resultBool, "not(false) should be true");
	    
	    // check function epochToIso8601
        // set default time zone used in test to get independent of runtime
	  	TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
	  	assertEquals("Europe/Berlin",TimeZone.getDefault().getID());
	    
	    arguments = new Arguments()
	    		.add("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#p_long_i", (Long)1673359547L);
	    result = (String)agent.execute("http://w3id.org/steel/xlsx2owl-utils/functions.ttl#epochToIso8601", arguments);
		assertEquals("2023-01-10T15:05:47Z", result);
	}
	
	@Test
	void testGetPrefixMapFilePath() {
		System.clearProperty(MappingUtils.IRI_PREFIX_MAP_FILE_PROPERTY);
		assertEquals("." + File.separator + "resources" + File.separator + "prefixes.csv", MappingUtils.getPrefixMapFilePath() );
		
		System.setProperty(MappingUtils.IRI_PREFIX_MAP_FILE_PROPERTY, "resources" + File.separator + "functions_xlsx2owl.ttl");
		assertEquals("resources" + File.separator + "functions_xlsx2owl.ttl", MappingUtils.getPrefixMapFilePath() );
		System.clearProperty(MappingUtils.IRI_PREFIX_MAP_FILE_PROPERTY);
		
		//difficult to test with changed working directory
	}

}
