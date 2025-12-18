/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.mediaSorter.Database;
import com.asofterspace.toolbox.configuration.ConfigFile;


public class WebLinkSorter {

	private Database database;

	private ConfigFile config;


	public WebLinkSorter(Database database, ConfigFile config) {
		this.database = database;
		this.config = config;
	}

	public void run() {
		System.out.println("Starting weblink sorting...");

		// TODO

		// run through:
		// /Desktop/ and open all .stpu files
		// workbench: load JSON of entries directly
		// workbench: load snailed JSON
		// /data/ for video sources, podcasts etc. (search .txt files in there)

		// for each:
		// check if there are links in there (starting with http:// or https://)
		// strip links of tracking info, then store
		// if link is preceded by source: above (so going lines up until an empty line is encountered),
		// then store as main source, otherwise as mentioned
		// write all findings into a JavaScript file that is then loaded by a static viewer HTML file that
		// allows searching for URLs and returns the best matches

		System.out.println("Weblink sorting done!");
	}

}
