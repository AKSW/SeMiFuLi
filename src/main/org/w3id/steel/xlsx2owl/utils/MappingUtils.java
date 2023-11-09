package org.w3id.steel.xlsx2owl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtils {
	public static final String IRI_PREFIX_MAP_FILE_PROPERTY = "IRI_PREFIX_MAP_FILE";
	public static final String IRI_PREFIX_MAP_FILE_ENV_VARIABLE = "IRI_PREFIX_MAP_FILE";

	private static final Logger logger = LoggerFactory.getLogger(MappingUtils.class);
	
	/**
	 * RegExp Pattern for IRIs with a prefix like e.g. 'foaf:person' 
	 */
	protected static Pattern iriPrefixPattern = Pattern.compile("^(?>(?<pre>.+?)(?<sep>:|%3A|%3a))?(?<sub>.*+)");
	
	/**
	 * Tries to return a path to an existing prefix csv file. 
	 * It checks for file existance in the following order:
	 * * java property `IRI_PREFIX_MAP_FILE_PROPERTY`
	 * * environment variable `IRI_PREFIX_MAP_FILE_ENV_VARIABLE`
	 * * "prefixes.csv"
	 * * "resources/prefixes.csv"
	 * * binPath+"/prefixes.csv"
	 * * binPath+"/resources/prefixes.csv"
	 * @return
	 */
	public static String getPrefixMapFilePath() {
		
		//logger.debug("searching for PrefixMapFile");
		// get current bin's / jar's path
		// see https://stackoverflow.com/questions/2837263/how-do-i-get-the-directory-that-the-currently-executing-jar-file-is-in
		String binPathStr = null;
		try {
			binPathStr = MappingUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			File binPath = new File(binPathStr);
			if (binPath.isFile()) {
				binPath = binPath.getParentFile();
				binPathStr = binPath.getPath();
			}
			
		//} catch (URISyntaxException | FileNotFoundException e) {
		} catch (Exception e) {
			logger.debug("unable to get bin path, skipping", e);
			binPathStr = null;
		}
		
		String homePathStr = System.getProperty("user.home");
 
		// fill path list for testing
		LinkedList<String> testPaths = new LinkedList<String>(); //(Arrays.asList("prefixes.csv", "resources/prefixes.csv", binPathStr+"/prefixes.csv", binPathStr+"/resources/prefixes.csv" ));
		for (String basePath: Arrays.asList(".", binPathStr, homePathStr)) {
			if (basePath != null) {
				testPaths.addLast(basePath + File.separator + "prefixes.csv");
				testPaths.addLast(basePath + File.separator + "resources" + File.separator + "prefixes.csv");
			}
		}
		//System.out.println("test paths: " + testPaths);
		
		
		// add value from System property as second entry
		try {
			String value = System.getProperty(IRI_PREFIX_MAP_FILE_PROPERTY);
			if (null != value)
				testPaths.addFirst( value );
		} catch (SecurityException e) {
			logger.info("got securityException while querying environment", e);
		}
		
		// add value from System environment variable as first entry
		try {
			String value = System.getenv(IRI_PREFIX_MAP_FILE_ENV_VARIABLE);
			if (null != value)
				testPaths.addFirst( value );
		} catch (SecurityException e) {
			logger.info("got securityException while querying environment", e);
		}
		
		// return first entry from test list where the file exists
		for (String testPath: testPaths ) {
			try {
				if ( new File(testPath).isFile() ) {
					logger.debug("found iri prefix mapping file at '{}'", testPath);
					return testPath;
				}
			} catch (Exception e) {
				logger.debug("exception while looking for prefixes.csv", e);
			}
		}
		// return last entry if no file exist
		logger.error("found no iri prefix mapping file. Looked at '{}'", testPaths);
		return testPaths.getLast();
	}
	
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
	 * @throws IOException An IOException is thrown if problems arise reading the prefixes.csv file
	 */
	public static List<String> expandIriPrefixes(List<String> iris) throws IOException {
		if (iris == null)
			return null;
		if (iris.size()<=0)
			return Collections.emptyList();
		List<String> result = new LinkedList<>();
		Map<String, String> prefixMap;
		try {
			prefixMap = readInPrefixCsv( new FileReader(getPrefixMapFilePath() ) );
		} catch (FileNotFoundException e) {
			logger.debug("Problem encountered while reading in prefix map file, skipping.", e);
			prefixMap = new HashMap<String, String>();
		}
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
		return expandIriPrefix(iri, getPrefixMapFilePath());
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
	
	/**
     * Returns the array of strings obtained by splitting `s` at wherever `sep` is found in it and removing heading and trailing spaces.
     * `sep` can be either a string or a regular expression.
     * For example, `split("fire, water, earth, air", ",")` returns the array of 4 strings:
     * "fire", "water", "earth" , and "air".
     * The double quotation marks are shown here only to highlight the fact that the spaces are removed.
     *
     * This implementation is a copy of the GREL string function "split" with additional null input handling and trim.
     * @see https://github.com/FnOio/grel-functions-java/blob/master/src/main/java/io/fno/grel/StringFunctions.java
     *
     * @param s   string
     * @param sep separator
     * @return the array of strings obtained by splitting `s` at wherever `sep` is found in it
     */
	public static List<String> splitAndTrim(String s, String sep) {
		List<String> result = new ArrayList<String>( split(s, sep) );
		for (int z=0; z<result.size(); z++) {
			if (result.get(z)!=null) {
				result.set(z, result.get(z).trim() );
			}
		}
		return result;
	}

	/**
     * Returns a boolean indicating whether s contains sub.
	 * For example, "food".contains("oo") returns true whereas "food".contains("ee") returns false.
     * This implementation is a copy of the GREL string function "contains" with additional null input handling.
	 * 
	 * @see https://github.com/FnOio/grel-functions-java/blob/master/src/main/java/io/fno/grel/StringFunctions.java
     *
     * @param s     string
     * @param sub   substring
     * @return      {@code true} if {@code s} contains {@code sub}.
     */
    public static Boolean contains(String s, String sub) {
		if (s==null) return false;
		else if (sub==null) return true;
        else return s.contains(sub);
    }

	/**
     * Returns a boolean indicating whether s not contains sub.
     * This implementation is a shortcut for not+contains.
     *
     * @param s     string
     * @param sub   substring
     * @return      {@code true} if {@code s} not contains {@code sub}.
     */
    public static Boolean notContains(String s, String sub) {
		//System.out.println("checking '"+s+"' not contains '"+sub+"'");
		return !contains(s, sub);
    }

	/**
	 * Splits the given iris by separator 'sep' and expands prefixes (if known) in the resulting list of iris.
	 * A CSV file 'prefixes.csv' with known prefixes is expected.
	 * @param iris string containing possibly multiple iris, separated by 'sep'
	 * @param sep
	 * @return returns a list of iris with known prefixes expanded
	 * @throws IOException An IOException is thrown if problems arise reading the prefixes.csv file
	 */
	public static List<String> splitAndExpandIriPrefixes(String iris, String sep) throws IOException {
		return expandIriPrefixes( splitAndTrim(iris, sep));
	}
	
	/**
	 * Convertes a unix epoch to an ISOString, usually seen in xsd:DateTime
	 * @param epoch
	 * @return
	 */
	public static String epochToIso8601(long epoch) {
		logger.debug("epoch:", epoch);
		if(epoch <= 167335954700L) // < 1975
			epoch *= 1000;
		String format = "yyyy-MM-dd'T'HH:mm:ss'Z'";
		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
		sdf.setTimeZone(TimeZone.getDefault());
		String toReturn = sdf.format(new Date(epoch));
		logger.debug("converted to:", toReturn);
		return toReturn;
	}

	/**
     * Logically NOT a boolean to yield another boolean.
     * This implementation is a copy of the GREL function "not" with additional null input handling.
	 * null returns false
	 * 
	 * @see https://github.com/FnOio/grel-functions-java/blob/master/src/main/java/io/fno/grel/BooleanFunctions.java
     * @param b a boolean
     * @return the reverted boolean
     */
	//TODO there is some problem with this function, the FNO invocation test fails as the b is always null.
    // public static Boolean not(Boolean b) {
	// 	System.out.println("negating '"+b+"'");
	// 	if (b==null) return false;
    //     return !b;
    // }
	
}
