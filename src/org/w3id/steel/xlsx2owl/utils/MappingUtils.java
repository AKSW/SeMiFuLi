package org.w3id.steel.xlsx2owl.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtils {
	private static final Logger logger = LoggerFactory.getLogger(MappingUtils.class);
	
	/**
	 * RegExp Pattern for IRIs with a prefix like e.g. 'foaf:person' 
	 */
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
	
	/**
	 * reads in a mapping of prefixes to iris from csv input.
	 * CSV input should have a header line like 'prefix,iri' and be separated by comma(',') or semicolon(';') or tab.
	 * No handling of quotes (' or ").
	 * @param in Reader to read the csv input from
	 * @return returns the iri mapping ( prefix -> long iri ) 
	 * @throws IOException
	 */
	public static Map<String, String> readInPrefixCsv(Reader in) throws IOException {
		//we avoid csv libraries for compatibility and implement direct csv parsing here
		
		final String csvSplitRegex = "[,;\t]";
		
		Map<String, String> prefixMap = new LinkedHashMap<>();
		Integer prefixColumn = null;
		Integer iriColumn = null;
		
		BufferedReader bufferedReader = new BufferedReader(in);
		
		//* read in csv header
		String headerLine = bufferedReader.readLine();
		
		String[] headerArray = headerLine.split(csvSplitRegex);
		
		for (int i = headerArray.length -1; i>=0; i--) {
			//System.out.println("'"+headerArray[i]+"'");
			if ("prefix".compareToIgnoreCase(headerArray[i]) == 0) 
				prefixColumn = i;
			if ("iri".compareToIgnoreCase(headerArray[i]) == 0)
				iriColumn = i;
		}
		if (prefixColumn==null || iriColumn==null)
			throw new IOException("unexpected CSV header. Expected something like 'prefix,iri' but got '"+headerLine+"'");
		
		//* read in csv entries
		String line;
		while ( (line = bufferedReader.readLine()) != null) {
			if (line.trim().length()>0) { //only work on non empty lines
				String[] entryArray = line.split(csvSplitRegex);
				//System.out.println(Arrays.asList(entryArray).toString());
				if (entryArray.length != headerArray.length)
					throw new IOException("unexpected CSV line column count. Expected "+headerArray.length+" columns, bot found "+entryArray.length+" columns in line '"+line+"'");
				
				String prefix = entryArray[prefixColumn].trim();
				String longIri = entryArray[iriColumn].trim();
				prefixMap.put(prefix, longIri);
			}
		}
		
		return prefixMap;
	}
	
	/**
	 * Returns the given list of iris, prefixes get expanded if known.
	 * Known prefixes get read in from './resources/prefixes.csv'.
	 * @param iris a list or iris
	 * @return the list of iris with expanded prefixes
	 * @throws IOException
	 */
	public static List<String> expandIriPrefixes(List<String> iris) throws IOException {
		if (iris == null)
			return null;
		if (iris.size()<=0)
			return Collections.emptyList();
		List<String> result = new LinkedList<>();
		Map<String, String> prefixMap = readInPrefixCsv(new FileReader("resources/prefixes.csv"));
		for (String iri:iris) {
			String expandedIri = expandIriPrefix(iri, prefixMap);
			if (expandedIri!=null && expandedIri.trim().length()!=0)
				result.add(expandedIri);
		}
		if (iris.size() > 0)
			logger.debug("expanded Iris '"+iris+"' to '"+result+"'");
		return result;
	}
	
	public static String expandIriPrefix(String iri) throws IOException {
		return expandIriPrefix(iri, "resources/prefixes.csv");
	}

	public static String expandIriPrefix(String iri, String prefixFilePath) throws IOException {
		Map<String, String> prefixMap = readInPrefixCsv(new FileReader(prefixFilePath));	
		String expandedIri = expandIriPrefix(iri, prefixMap);
		if (iri!=null && iri.trim().length()!=0)
			logger.debug("expanded Iri '"+iri+"' to '"+expandedIri+"'");
		return expandedIri;
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
		else {
			List<String> result = Arrays.asList(s.split(sep));
			if (result.size()>1)
				logger.debug("split '"+s+"' to "+result);
			return result;
		}
    }
	
	public static List<String> splitAndExpandIris(String s, String sep) throws IOException {
		return expandIriPrefixes( split(s, sep) );
	}

}
