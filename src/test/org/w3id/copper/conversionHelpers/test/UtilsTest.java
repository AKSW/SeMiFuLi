package org.w3id.copper.conversionHelpers.test;

//import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;
import org.w3id.copper.conversionHelpers.Utils;

class UtilsTest {

	@Test
	void testEpochToIso8601() throws IOException {

		long epoch = 1673359547L;
		assertEquals("2023-01-10T15:05:47Z", Utils.epochToIso8601(epoch));
		assertEquals("2023-01-10T15:05:47Z", Utils.epochToIso8601(epoch * 1000));
	}

}
