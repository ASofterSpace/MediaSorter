/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import java.util.Collections;
import java.util.Comparator;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.utils.TextEncoding;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.0.3(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019 - 12. February 2020";

	private final static String[] TRY_PIC_ENDINGS = {"jpg", "jpeg", "gif", "png", "bmp"};


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

		ConfigFile config = null;

		try {
			// load config
			config = new ConfigFile("settings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"filmpath\":\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the settings failed:");
			System.err.println(e);
			System.exit(1);
		}

		// load media files
		String filmpath = config.getValue("filmpath");

		if ((filmpath == null) || (filmpath.equals(""))) {
			System.err.println("Sorry, no filmpath specified in the configuration!");
			System.exit(1);
		}

		SimpleFile vstpu = new SimpleFile(filmpath + "/VSTPU.stpu");
		vstpu.setEncoding(TextEncoding.ISO_LATIN_1);
		List<String> filmnames = vstpu.getContents();

		// create statistics
		List<String> stats = new ArrayList<>();
		stats.add("Statistics");
		stats.add("");
		int filmcounter = 0;
		Map<Integer, List<String>> amazingnessFilms = new HashMap<>();
		Map<String, List<String>> yearlyFilms = new HashMap<>();
		Map<String, List<String>> genreFilms = new HashMap<>();

		List<Film> films = new ArrayList<>();

		for (int i = 2; i < filmnames.size(); i++) {

			String filmfilename = filmnames.get(i);

			if (filmfilename.equals("")) {
				break;
			}

			SimpleFile film = new SimpleFile(filmpath + "/" + filmfilename + ".stpu");
			film.setEncoding(TextEncoding.ISO_LATIN_1);
			List<String> filmContents = film.getContents();

			String filmname = filmContents.get(0);

			if (filmname == null) {
				System.err.println(filmnames.get(i) + " does not have any contents!");
				continue;
			}

			// ignore links (as the main file will also be listed in here)
			if (filmname.startsWith("%[")) {
				continue;
			}

			Film curFilm = new Film(filmname);
			films.add(curFilm);

			for (int j = 0; j < filmContents.size(); j++) {
				if (filmContents.get(j).equals("Amazingness:")) {
					Integer amazingness = getAmazingness(filmContents.get(j+1));
					if (amazingness != null) {
						List<String> thisAmazingnessFilms = amazingnessFilms.get(amazingness);
						if (thisAmazingnessFilms == null) {
							thisAmazingnessFilms = new ArrayList<>();
							amazingnessFilms.put(amazingness, thisAmazingnessFilms);
						}
						thisAmazingnessFilms.add(filmname);
					}
					curFilm.setAmazingness(amazingness);
				}
				if (filmContents.get(j).equals("From:")) {
					List<String> thisYearsFilms = yearlyFilms.get(filmContents.get(j+1));
					if (thisYearsFilms == null) {
						thisYearsFilms = new ArrayList<>();
						yearlyFilms.put(filmContents.get(j+1), thisYearsFilms);
					}
					thisYearsFilms.add(filmname);
					curFilm.setYear(filmContents.get(j+1));
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
						curFilm.addGenre(curGenre);
					}
				}
			}

			for (String ending : TRY_PIC_ENDINGS) {
				File prevFile = new File(filmpath + "/" + filmfilename + "_1." + ending);
				if (prevFile.exists()) {
					curFilm.setPreviewPic(filmfilename + "_1." + ending);
					break;
				}
			}

			filmcounter++;
		}
		stats.add("We have " + filmcounter + " films.");
		stats.add("");
		stats.add("");
		stats.add("Amazingness distribution:");
		stats.add("");
		Object[] amazingnessKeys = amazingnessFilms.keySet().toArray();
		Arrays.sort(amazingnessKeys, Collections.reverseOrder());
		for (Object key: amazingnessKeys) {
			List<String> thisAmazingnessFilms = amazingnessFilms.get(key);
			int amount = thisAmazingnessFilms.size();
			StringBuilder line = new StringBuilder();
			stats.add("Amazingness " + key + ": " + StrUtils.thingOrThings(amount, "film") + ":");
			for (String film : thisAmazingnessFilms) {
				stats.add("  " + film);
			}
			stats.add("");
		}
		stats.add("");
		stats.add("Yearly distribution:");
		stats.add("");
		Object[] yearKeys = yearlyFilms.keySet().toArray();
		Arrays.sort(yearKeys);
		for (Object key: yearKeys) {
			List<String> thisYearsFilms = yearlyFilms.get(key);
			int amount = thisYearsFilms.size();
			StringBuilder line = new StringBuilder();
			stats.add(key + ": " + StrUtils.thingOrThings(amount, "film") + ":");
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
			stats.add(key + ": " + StrUtils.thingOrThings(amount, "film") + ":");
			for (String film : thisGenreFilms) {
				stats.add("  " + film);
			}
			stats.add("");
		}

		// save statistics
		SimpleFile statsFile = new SimpleFile(filmpath + "/Statistics.stpu");
		statsFile.setEncoding(TextEncoding.ISO_LATIN_1);
		statsFile.saveContents(stats);

		System.out.println("Saved new statistics for " + filmcounter + " films at " + statsFile.getCanonicalFilename() + "!");

		// create overviews
		saveFilmsAsOverview(films, filmpath + "/overview.htm");

		Collections.sort(films, new Comparator<Film>() {
			public int compare(Film a, Film b) {
				Integer aComp = a.getAmazingness();
				if (aComp == null) {
					aComp = 0;
				}
				Integer bComp = b.getAmazingness();
				if (bComp == null) {
					bComp = 0;
				}
				return bComp - aComp;
			}
		});

		saveFilmsAsOverview(films, filmpath + "/overviewByAmazingness.htm");

		Collections.sort(films, new Comparator<Film>() {
			public int compare(Film a, Film b) {
				String aComp = a.getYear();
				if (aComp == null) {
					aComp = "0000";
				}
				String bComp = b.getYear();
				if (bComp == null) {
					bComp = "0000";
				}
				return bComp.compareTo(aComp);
			}
		});

		saveFilmsAsOverview(films, filmpath + "/overviewByYear.htm");

		System.out.println("Saved overview HTML files!");
	}

	private static Integer getAmazingness(String str) {
		Integer result = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == 'X') {
				result++;
			}
		}
		if ((result == 0) && (str.contains("?"))) {
			result = null;
		}
		return result;
	}

	private static void saveFilmsAsOverview(List<Film> films, String filename) {

		StringBuilder overview = new StringBuilder();
		overview.append("<html>");
		overview.append("<head>");
		overview.append("<style>");
		overview.append("div.filmcontainer {");
		overview.append("	display: flex;");
		overview.append("	flex-wrap: wrap;");
		overview.append("}");
		overview.append("div.film {");
		overview.append("	padding: 4pt;");
		overview.append("}");
		overview.append("img {");
		overview.append("	width: 200pt;");
		overview.append("}");
		overview.append("div.filmtitle {");
		overview.append("	text-align: center;");
		overview.append("	width: 200pt;");
		overview.append("}");
		overview.append("div.extrainfo {");
		overview.append("	text-align: center;");
		overview.append("	width: 200pt;");
		overview.append("}");
		overview.append("</style>");
		overview.append("</head>");
		overview.append("<body>");
		overview.append("<div class='filmcontainer'>");
		for (Film film : films) {
			overview.append("<div class='film'>");
			overview.append("<div class='filmtitle'>" + film.getTitle() + "</div>");
			if (film.getAmazingness() == null) {
				overview.append("<div class='extrainfo'>" + film.getYear() + " &loz; ?</div>");
			} else {
				overview.append("<div class='extrainfo'>" + film.getYear() + " &loz; " + film.getAmazingness() + "/10</div>");
			}
			if (film.getPreviewPic().contains("'")) {
				overview.append("<img src=\"" + film.getPreviewPic() + "\" />");
			} else {
				overview.append("<img src='" + film.getPreviewPic() + "' />");
			}
			overview.append("</div>");
		}
		overview.append("</div>");
		overview.append("</body>");
		overview.append("</html>");

		// save overview
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.setEncoding(TextEncoding.ISO_LATIN_1);
		overviewFile.saveContent(overview);
	}

}
