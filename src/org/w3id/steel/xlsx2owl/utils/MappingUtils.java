package org.w3id.steel.xlsx2owl.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.InputMismatchException;
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
	 * CSV input should be comma(',') separated and have the header 'prefix,iri'.
	 * @param in Reader to read the csv input from
	 * @return returns the iri mapping
	 * @throws IOException
	 */
	public static Map<String, String> readInPrefixCsv(Reader in) throws IOException {
		Map<String, String> prefixMap = new HashMap<>();
		
		CSVFormat csvReader = CSVFormat.Builder.create(CSVFormat.EXCEL).setHeader().build();
		Iterable<CSVRecord> records = csvReader.parse(in);
		for (CSVRecord record : records) {
			if (!record.isMapped("prefix") || !record.isMapped("iri")) {
				String headerList = String.join(",", new LinkedList<String>(record.toMap().keySet()));
				throw new IOException("unexpected CSV header. Expected 'prefix,iri' but got '"+headerList+"'");
			}
		    String prefix = record.get("prefix");
		    String longIri = record.get("iri");
		    prefixMap.put(prefix, longIri);
		}
		
		return prefixMap;
	}

//	public static String[] readCsvLineTuple(Reader in) {
//		while( (strLine = br.readLine()) != null)
//	    {
//	        lineNumber++;
//
//	        //break comma separated line using ","
//	        st = new StringTokenizer(strLine, ",");
//
//	        while(st.hasMoreTokens())
//	        {
//	        //display csv values
//	        tokenNumber++;
//	        System.out.println("Line # " + lineNumber +
//	                        ", Token # " + tokenNumber
//	                        + ", Token : "+ st.nextToken());
//
//
//	                    System.out.println(cols[4]);
//	}
	
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
