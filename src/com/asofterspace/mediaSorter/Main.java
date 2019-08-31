/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.0.1(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019";

	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		// load config
		ConfigFile config = new ConfigFile("settings", true);

		// create a default config file, if necessary
		if (config.getAllContents().isEmpty()) {
			config.setAllContents(new JSON("{\"filmpath\":\"\"}"));
		}

		// load media files
		String filmpath = config.getValue("filmpath");

		if ((filmpath == null) || (filmpath.equals(""))) {
			System.err.println("Sorry, no filmpath specified in the configuration!");
			System.exit(1);
		}

		SimpleFile vstpu = new SimpleFile(filmpath + "/VSTPU.stpu");
		vstpu.useCharset(StandardCharsets.ISO_8859_1);
		List<String> filmnames = vstpu.getContents();

		// create statistics
		List<String> stats = new ArrayList<>();
		stats.add("Statistics");
		stats.add("");
		int filmcounter = 0;
		Map<String, List<String>> yearlyFilms = new HashMap<>();
		Map<String, List<String>> genreFilms = new HashMap<>();

		for (int i = 2; i < filmnames.size(); i++) {

			String filmname = filmnames.get(i);

			if (filmname.equals("")) {
				break;
			}

			SimpleFile film = new SimpleFile(filmpath + "/" + filmname + ".stpu");
			film.useCharset(StandardCharsets.ISO_8859_1);
			List<String> filmContents = film.getContents();

			filmname = filmContents.get(0);

			if (filmname == null) {
				System.err.println(filmnames.get(i) + " does not have any contents!");
				continue;
			}

			// ignore links (as the main file will also be listed in here)
			if (filmname.startsWith("%[")) {
				continue;
			}

			for (int j = 0; j < filmContents.size(); j++) {
				if (filmContents.get(j).equals("From:")) {
					List<String> thisYearsFilms = yearlyFilms.get(filmContents.get(j+1));
					if (thisYearsFilms == null) {
						thisYearsFilms = new ArrayList<>();
						yearlyFilms.put(filmContents.get(j+1), thisYearsFilms);
					}
					thisYearsFilms.add(filmname);
				}
				if (filmContents.get(j).equals("Genre:")) {
					String[] genres = filmContents.get(j+1).split(" / ");
					for (String curGenre : genres) {
						List<String> thisGenreFilms = genreFilms.get(curGenre);
						if (thisGenreFilms == null) {
							thisGenreFilms = new ArrayList<>();
							genreFilms.put(curGenre, thisGenreFilms);
						}
						thisGenreFilms.add(filmname);
					}
				}
			}

			filmcounter++;
		}
		stats.add("We have " + filmcounter + " films.");
		stats.add("");
		stats.add("");
		stats.add("Yearly distribution:");
		stats.add("");
		Object[] yearKeys = yearlyFilms.keySet().toArray();
		Arrays.sort(yearKeys);
		for (Object key: yearKeys) {
			List<String> thisYearsFilms = yearlyFilms.get(key);
			int amount = thisYearsFilms.size();
			StringBuilder line = new StringBuilder();
			stats.add(key + ": " + Utils.thingOrThings(amount, "film") + ":");
			for (String film : thisYearsFilms) {
				stats.add("  " + film);
			}
			stats.add("");
		}
		stats.add("");
		stats.add("Genre distribution:");
		stats.add("");
		Object[] genreKeys = genreFilms.keySet().toArray();
		Arrays.sort(genreKeys);
		for (Object key: genreKeys) {
			List<String> thisGenreFilms = genreFilms.get(key);
			int amount = thisGenreFilms.size();
			StringBuilder line = new StringBuilder();
			stats.add(key + ": " + Utils.thingOrThings(amount, "film") + ":");
			for (String film : thisGenreFilms) {
				stats.add("  " + film);
			}
			stats.add("");
		}

		// save statistics
		SimpleFile statsFile = new SimpleFile(filmpath + "/Statistics.stpu");
		statsFile.useCharset(StandardCharsets.ISO_8859_1);
		statsFile.saveContents(stats);

		System.out.println("Saved new statistics for " + filmcounter + " films at " + statsFile.getCanonicalFilename() + "!");
	}

}
