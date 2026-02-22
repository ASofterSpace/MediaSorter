/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.mediaSorter.movies.MovieDatabase;
import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.mediaSorter.series.SeriesSorter;
import com.asofterspace.mediaSorter.weblinks.WebLinkDatabase;
import com.asofterspace.mediaSorter.weblinks.WebLinkSorter;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.Utils;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.2.4(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019 - 22. February 2026";


	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		boolean sortMovies = true;
		boolean sortSeries = true;
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
					sortSeries = false;
				}
				if (arg.equals("--series-only")) {
					sortWeblinks = false;
					sortMovies = false;
				}
				if (arg.equals("--weblinks-only")) {
					sortMovies = false;
					sortSeries = false;
				}
			}
		}

		Directory confDir = new Directory("config");
		Directory serverDir = new Directory("server");
		Directory outputDir = new Directory("output");

		if (sortMovies) {
			MovieDatabase database = new MovieDatabase(confDir);
			MovieSorter sorter = new MovieSorter(database, serverDir, outputDir);
			sorter.run();
		}

		if (sortSeries) {
			SeriesSorter sorter = new SeriesSorter(serverDir, outputDir);
			sorter.run();
		}

		if (sortWeblinks) {
			WebLinkDatabase database = new WebLinkDatabase(confDir);
			WebLinkSorter sorter = new WebLinkSorter(database, serverDir, outputDir);
			sorter.run();
		}
	}

}
