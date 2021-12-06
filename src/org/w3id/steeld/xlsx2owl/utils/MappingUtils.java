package org.w3id.steeld.xlsx2owl.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingUtils {
	
	protected static Pattern iriPrefixPattern = Pattern.compile("^(?>(?<pre>.+?)(?<sep>:|%3A|%3a))?(?<sub>.*+)");
	/**
	 * Returns the given Iri with expanded prefix from prefixMap
	 * @param iri
	 * @param prefixMap
	 * @return
	 */
	public static String expandIriPrefix(String iri, Map<String, String> prefixMap) {
		// check for empty input
		if (iri==null || iri.trim().isEmpty())
			return iri;
		if (prefixMap==null || prefixMap.isEmpty())
			return iri;
		
		// apply regexp
		Matcher matcher = iriPrefixPattern.matcher(iri);
		if (!matcher.matches())
			throw new IllegalStateException("internal regexp problem found for iri '"+iri+"' with our iri pattern '"+iriPrefixPattern.pattern()+"'");
		String prefixShort = matcher.group("pre");
		String subject = matcher.group("sub");
		
		// substitute empty short prefix with 'base' prefix
		if (prefixShort==null || prefixShort.length()==0)
			prefixShort = "base";
		
		// return expanded IRI
		if (prefixMap.containsKey(prefixShort)) {
			return prefixMap.get(prefixShort) + subject; 
		} else return iri;
	}
	
	public static String expandIriPrefix(String iri, String filePath) {
		//CSVFormat.
		return expandIriPrefix(iri, Collections.emptyMap()); //FIXME
	}
	

	/**
     * Returns the array of strings obtained by splitting `s` at wherever `sep` is found in it.
     * `sep` can be either a string or a regular expression.
     * For example, `split("fire, water, earth, air", ",")` returns the array of 4 strings:
     * "fire", " water", " earth" , and " air".
     * The double quotation marks are shown here only to highlight the fact that the spaces are retained.
     *
     * This implementation is a copy of the GREL string function "split" with additional null input handling.
     * @see https://github.com/FnOio/grel-functions-java/blob/master/src/main/java/io/fno/grel/StringFunctions.java
     *
     * @param s   string
     * @param sep separator
     * @return the array of strings obtained by splitting `s` at wherever `sep` is found in it
     */
	public static List<String> split(String s, String sep) {
		if (s==null)
			return Collections.emptyList();
		else if (sep==null)
			return Collections.singletonList(s);
		else if (sep.length()!=1)
			return Collections.singletonList(s);
				//throw new InputMismatchException("expected a single character as split separator, but got '"+sep+"'");
		else return Arrays.asList(s.split(sep));
    }

}
