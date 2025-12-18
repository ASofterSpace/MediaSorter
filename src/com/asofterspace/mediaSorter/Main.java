/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.mediaSorter.weblinks.WebLinkSorter;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.Utils;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.2.2(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019 - 18. December 2025";


	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		boolean sortMovies = true;
		boolean sortWeblinks = true;

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}

			for (String arg : args) {
				if (arg.equals("--movies-only")) {
					sortWeblinks = false;
				}
				if (arg.equals("--weblinks-only")) {
					sortMovies = false;
				}
			}
		}

		Directory confDir = new Directory("config");
		Database database = new Database(confDir);

		ConfigFile config = null;

		try {
			// load config
			config = new ConfigFile("settings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"filmpath\":\"\", \"filmfileorigin\":\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the settings failed:");
			System.err.println(e);
			System.exit(1);
		}

		if (sortMovies) {
			MovieSorter movieSorter = new MovieSorter(database, config);
			movieSorter.run();
		}

		if (sortWeblinks) {
			WebLinkSorter webLinkSorter = new WebLinkSorter(database, config);
			webLinkSorter.run();
		}
	}

}
