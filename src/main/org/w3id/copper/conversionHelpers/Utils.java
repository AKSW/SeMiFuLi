package org.w3id.copper.conversionHelpers;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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

}
