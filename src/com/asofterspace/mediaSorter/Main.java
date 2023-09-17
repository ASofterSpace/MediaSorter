/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.1.9(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019 - 18. September 2023";

	private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

	private final static String OVERVIEW = "overview";
	private final static String OVERVIEW_BY_AMAZINGNESS = "overviewByAmazingness";
	private final static String OVERVIEW_BY_YEAR = "overviewByYear";
	public final static String OVERVIEW_BY_GENRES = "overviewByGenres";
	public final static String OVERVIEW_BY_LANGUAGES = "overviewByLanguages";
	public final static String OVERVIEW_BY_ADDITION = "overviewByAddition";
	public final static String OVERVIEW_FILM = "overviewForFilm";

	private final static String NO_GENRE_SELECTED = "No Genre Assigned Yet";
	private final static String NO_LANGUAGE_SELECTED = "No Language Assigned Yet";
	private final static String PASSES_BECHDEL_STR = "Passes the Bechdel Test:";

	private static List<String> filesFound = new ArrayList<>();


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

		// load media files
		String filmpath = config.getValue("filmpath");
		String filmfileorigin = config.getValue("filmfileorigin");
		String filmfileoriginalt = config.getValue("filmfileoriginalt");

		if ((filmpath == null) || (filmpath.equals(""))) {
			System.err.println("Sorry, no filmpath specified in the configuration.\n" +
				"From this path, the input data is loaded.");
			System.exit(1);
		}

		if ((filmfileorigin == null) || (filmfileorigin.equals(""))) {
			System.err.println("Sorry, no filmfileorigin specified in the configuration.\n" +
				"At this path, the actual files to be opened in an external player are located.");
			System.exit(1);
		}

		SimpleFile vstpu = new SimpleFile(filmpath + "/VSTPU.stpu");
		vstpu.setISOorUTFreadAndUTFwriteEncoding(true);
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

		Record filmMappings = config.getAllContents().get("filmNameMappings");

		for (int i = 2; i < filmnames.size(); i++) {

			String filmfilename = filmnames.get(i);

			if ("".equals(filmfilename)) {
				break;
			}

			SimpleFile film = new SimpleFile(filmpath + "/" + filmfilename + ".stpu");
			film.setISOorUTFreadAndUTFwriteEncoding(true);

			// make some adjustments automatically
			boolean madeChanges = false;
			String filmContent = film.getContent();
			if (filmContent.toLowerCase().contains(PASSES_BECHDEL_STR.toLowerCase())) {
				if (!filmContent.contains(PASSES_BECHDEL_STR)) {
					filmContent = StrUtils.replaceAllIgnoreCase(filmContent,
						PASSES_BECHDEL_STR, PASSES_BECHDEL_STR);
					madeChanges = true;
				}
			}
			if (!filmContent.contains(PASSES_BECHDEL_STR)) {
				filmContent = StrUtils.replaceFirst(filmContent, "\nArchive:\n",
					"\n" + PASSES_BECHDEL_STR + "\nno\n\n\nArchive:\n");
				madeChanges = true;
			}
			if (madeChanges) {
				film.saveContent(filmContent);
			}

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

			filmname = mapFilmnameToSpecialFilmname(filmname, filmMappings);

			Film curFilm = new Film(film, filmname, filmfilename, filmcounter);
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
					String triggerText = null;
					StringBuilder reviewText = new StringBuilder();
					int curLine = j+2;
					while (!"".equals(filmContents.get(curLine).trim())) {
						String line = filmContents.get(curLine).trim();
						String lineLow = line.toLowerCase();
						if (lineLow.startsWith("trigger warning: ") || lineLow.startsWith("trigger warnings: ") ||
							lineLow.startsWith("content note: ") || lineLow.startsWith("content notes: ")) {
							triggerText = line.substring(line.indexOf(": ") + 2).trim();
						} else {
							reviewText.append(line + " ");
						}
						curLine++;
					}
					curFilm.setReview(reviewText.toString().trim());
					curFilm.setTriggerWarning(triggerText);
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
				if (filmContents.get(j).equals("Passes the Bechdel Test:")) {
					curFilm.setBechdel(filmContents.get(j+1));
				}
				if (filmContents.get(j).equals("Genre:")) {
					String[] genres = filmContents.get(j+1).split(" / ");
					for (String curGenre : genres) {
						if ("???".equals(curGenre)) {
							curGenre = null;
						}
						List<String> thisGenreFilms = genreFilms.get(curGenre);
						if (thisGenreFilms == null) {
							thisGenreFilms = new ArrayList<>();
							genreFilms.put(curGenre, thisGenreFilms);
						}
						thisGenreFilms.add(filmname);
						curFilm.addGenre(curGenre);
					}
				}
				if (filmContents.get(j).equals("Related movies:")) {
					for (int movieNum = j + 1; movieNum < filmContents.size(); movieNum++) {
						String relatedMovie = filmContents.get(movieNum);
						if (!relatedMovie.startsWith("%[\\Desktop\\Filme\\")) {
							break;
						}
						relatedMovie = relatedMovie.substring("%[\\Desktop\\Filme\\".length());
						relatedMovie = relatedMovie.substring(0, relatedMovie.length() - 1);
						curFilm.addRelatedMovieName(relatedMovie);
					}
				}
				if (filmContents.get(j).equals("Archive:")) {
					FilmLocation curFilmLocation = null;
					for (int arcLineNum = j + 1; arcLineNum < filmContents.size(); arcLineNum++) {
						String arcLine = filmContents.get(arcLineNum);
						if ("Location: movies main".equals(arcLine)) {
							continue;
						}
						if (arcLine.startsWith("Location:")) {
							System.err.println(filmfilename + " is not located in movies main!");
							continue;
						}
						if ("Review:".equals(arcLine)) {
							break;
						}
						if (arcLine.startsWith("Language:")) {
							curFilmLocation = new FilmLocation(curFilm, filmfileorigin, filmfileoriginalt);
							curFilm.addFilmLocation(curFilmLocation);
							curFilmLocation.parseLanguages(arcLine.substring("Language:".length() + 1));
						}
						if (arcLine.startsWith("Subtitles:")) {
							curFilmLocation.parseSubtitleLanguages(arcLine.substring("Subtitles:".length() + 1));
						}
						if (arcLine.startsWith("Quality:")) {
							curFilmLocation.parseQuality(arcLine.substring("Quality:".length() + 1));
							System.out.println("There is a quality notice for " + curFilm.getTitle() + "!");
						}
						if (arcLine.startsWith("Edition:")) {
							curFilmLocation.parseEdition(arcLine.substring("Edition:".length() + 1));
						}
						if (arcLine.startsWith("Note:")) {
							curFilmLocation.parseNote(arcLine.substring("Note:".length() + 1));
						}
						if (arcLine.startsWith("%[")) {
							curFilmLocation.parseLocation(arcLine);
						}
						if ("".equals(arcLine)) {
							curFilmLocation = null;
						}
					}
				}
			}

			for (String ending : IMAGE_EXTENSIONS) {
				File prevFile = new File(filmpath + "/" + filmfilename + "_1." + ending);
				if (prevFile.exists()) {
					curFilm.setPreviewPic(filmfilename + "_1." + ending);
					break;
				}
			}

			filmcounter++;
		}
		for (Film film : films) {
			film.consolidate();
			film.resolveRelatedMovies(films);
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
		Set<String> genreKeySet = genreFilms.keySet();
		genreKeySet.remove(null);
		List<String> genreKeys = new ArrayList<String>(genreKeySet);
		Collections.sort(genreKeys);
		for (Object key : genreKeys) {
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
		statsFile.setISOorUTFreadAndUTFwriteEncoding(true);
		statsFile.saveContents(stats);

		System.out.println("Saved new statistics for " + filmcounter + " films at " + statsFile.getCanonicalFilename() + "!");


		// create all films overview
		Map<String, List<Film>> filmBrackets = new HashMap<>();
		filmBrackets.put("All Films Alphabetically", films);
		saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW + ".htm");


		// create overview sorted by amazingness
		filmBrackets = new TreeMap<>(new Comparator<String>() {
			public int compare(String a, String b) {
				// order "not yet graded" towards the end
				if ((a.charAt(0) == 'N') && (b.charAt(0) == 'N')) {
					return 0;
				}
				if (a.charAt(0) == 'N') {
					return 1;
				}
				if (b.charAt(0) == 'N') {
					return -1;
				}
				// append leading 0 if we have e.g. "1 out of 10", but not if we have "10 out of 10"
				if (a.charAt(1) == ' ') {
					a = "0" + a;
				}
				if (b.charAt(1) == ' ') {
					b = "0" + b;
				}
				return b.compareTo(a);
			}
		});

		for (Film film : films) {
			List<Film> curList = filmBrackets.get(film.getAmazingnessBracket());
			if (curList == null) {
				curList = new ArrayList<>();
				filmBrackets.put(film.getAmazingnessBracket(), curList);
			}
			curList.add(film);
		}

		saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_AMAZINGNESS + ".htm");



		// create overview sorted by year
		filmBrackets = new TreeMap<>(new Comparator<String>() {
			public int compare(String a, String b) {
				return b.compareTo(a);
			}
		});

		for (Film film : films) {
			if (film.getYear() == null) {
				System.err.println(film.getTitle() + " does not have a year assigned!");
				continue;
			}
			List<Film> curList = filmBrackets.get(film.getYear());
			if (curList == null) {
				curList = new ArrayList<>();
				filmBrackets.put(film.getYear(), curList);
			}
			curList.add(film);
		}

		saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_YEAR + ".htm");



		// create overviews for each genre
		final Map<String, List<Film>> genreMap = new HashMap<>();
		List<String> genres = new ArrayList<>();

		for (Film film : films) {
			for (String genre : film.getGenres()) {
				if (!genres.contains(genre)) {
					genres.add(genre);
					genreMap.put(genre, new ArrayList<Film>());
				}
				genreMap.get(genre).add(film);
			}
		}

		boolean genresContainedNull = genres.contains(null);
		genres.remove(null);
		Collections.sort(genres, new Comparator<String>() {
			public int compare(String a, String b) {
				return genreMap.get(b).size() - genreMap.get(a).size();
			}
		});
		// order "no genre selected yet" towards the end
		if (genresContainedNull) {
			genres.add(null);
		}

		int i = 0;

		Map<String, String> genreToKeyMap = new HashMap<>();

		for (String genre : genres) {
			String genreSanitized = genre.trim().toLowerCase();
			genreSanitized = StrUtils.replaceAll(genreSanitized, " ", "");
			genreSanitized = StrUtils.replaceAll(genreSanitized, "*", "");
			genreSanitized = StrUtils.replaceAll(genreSanitized, "&", "");
			genreToKeyMap.put(genre, genreSanitized);
			List<Film> filmsOfThisGenre = genreMap.get(genre);
			filmBrackets = new HashMap<>();
			if (genre == null) {
				genre = NO_GENRE_SELECTED;
			}
			filmBrackets.put(genre, filmsOfThisGenre);
			saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_GENRES + "_" + genreSanitized + ".htm");
			i++;
		}

		saveOverview(films.size(), genres, filmpath + "/" + OVERVIEW_BY_GENRES + ".htm", "Genre", NO_GENRE_SELECTED, OVERVIEW_BY_GENRES, genreToKeyMap);



		// create overviews for each language
		final Map<String, List<Film>> langMap = new HashMap<>();
		List<String> languages = new ArrayList<>();

		for (Film film : films) {
			for (String lang : film.getLanguages()) {
				if (!languages.contains(lang)) {
					languages.add(lang);
					langMap.put(lang, new ArrayList<Film>());
				}
				langMap.get(lang).add(film);
			}
		}

		boolean langsContainedNull = languages.contains(null);
		languages.remove(null);
		Collections.sort(languages, new Comparator<String>() {
			public int compare(String a, String b) {
				return langMap.get(b).size() - langMap.get(a).size();
			}
		});
		// order "no languages selected yet" towards the end
		if (langsContainedNull) {
			languages.add(null);
		}

		i = 0;

		Map<String, Integer> langToNumberMap = new HashMap<>();

		for (String lang : languages) {
			langToNumberMap.put(lang, i);
			List<Film> filmsOfThisLang = langMap.get(lang);
			filmBrackets = new HashMap<>();
			if (lang == null) {
				lang = NO_LANGUAGE_SELECTED;
			}
			filmBrackets.put(lang, filmsOfThisLang);
			saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_LANGUAGES + i + ".htm");
			i++;
		}

		saveOverview(films.size(), languages, filmpath + "/" + OVERVIEW_BY_LANGUAGES + ".htm", "Language", NO_LANGUAGE_SELECTED, OVERVIEW_BY_LANGUAGES, null);


		// create overview sorted by date of addition to the database
		filmBrackets = new TreeMap<>(new Comparator<String>() {
			public int compare(String a, String b) {
				return b.compareTo(a);
			}
		});

		for (Film film : films) {
			film.consolidateAdditionDateWithDatabase(database);
			if (film.getAdditionYearAndMonth() == null) {
				System.err.println(film.getTitle() + " does not have an addition year and month assigned!");
				continue;
			}
			List<Film> curList = filmBrackets.get(film.getAdditionYearAndMonth());
			if (curList == null) {
				curList = new ArrayList<>();
				filmBrackets.put(film.getAdditionYearAndMonth(), curList);
			}
			curList.add(film);
		}

		database.save();

		saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_ADDITION + ".htm");



		System.out.println("Saved overview HTML files!");

		for (Film film : films) {
			saveFilmFile(film, genreToKeyMap, langToNumberMap, filmpath, filmpath + "/" + OVERVIEW_FILM + "_" + film.getNumber() + ".htm");
		}

		System.out.println("Saved individual film files!");


		// check that all films are really encountered and none are missing from the list that do exist on the disk
		List<String> filesThatShouldBeFound = new ArrayList<>();
		addFilmsThatShouldBeFound(filesThatShouldBeFound, filmfileorigin);
		addFilmsThatShouldBeFound(filesThatShouldBeFound, filmfileoriginalt);

		for (String shouldBeFound : filesThatShouldBeFound) {

			// ignore subtitle files, those are not directly encountered and explicitly tracked anyway
			if (shouldBeFound.endsWith(".srt") ||
				shouldBeFound.endsWith(".idx") ||
				shouldBeFound.endsWith(".sub") ||
				shouldBeFound.endsWith(".dfxp") ||
				shouldBeFound.endsWith(".ass") ||
				shouldBeFound.endsWith(".sup") ||
				shouldBeFound.endsWith("Thumbs.db")) {
				continue;
			}

			if (!filesFound.contains(shouldBeFound)) {
				System.err.println("File '" + shouldBeFound + "' exists on the disk - but was not encountered!");
			}
		}

		System.out.println("Performed file encounter checks!");
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

	private static StringBuilder getHtmlTop(int filmAmount, boolean useFlatBackground) {

		StringBuilder overview = new StringBuilder();

		// use MotW so that IE allows embedded javascript to run without asking every time
		overview.append("<!DOCTYPE html>\r\n");
		overview.append("<!-- saved from url=(0016)https://localhost -->\r\n");
		overview.append("<html>\r\n");

		// add header
		overview.append("<head>");
		overview.append("<meta charset=\"utf-8\">");
		overview.append("<style>");
		overview.append("body {");
		overview.append("	background: linear-gradient(-29deg, #160022, #202, #11001A, #202, #201, #202, #202, #202, #11001A, #201, #202, #102, #101, #11001A, #202, #160022, #202, #11001A, #101, #11001A, #202);");
		overview.append("	color: rgb(136, 170, 255);");
		overview.append("}");
		overview.append("div.filmcontainer {");
		overview.append("	display: flex;");
		overview.append("	flex-wrap: wrap;");
		overview.append("	justify-content: center;");
		overview.append("	padding-top: 25pt;");
		overview.append("	padding-bottom: 75pt;");
		overview.append("}");
		overview.append("div.film {");
		overview.append("	padding: 4pt;");
		overview.append("}");
		overview.append("div.filmamount {");
		overview.append("	position: absolute;");
		overview.append("	left: 0;");
		overview.append("	top: 18pt;");
		overview.append("	font-size: 14pt;");
		overview.append("	font-style: italic;");
		overview.append("	color: #00EF50;");
		overview.append("	font-family: \"Consolas\";");
		overview.append("}");
		overview.append("div.filminfo, div.linkcontainer, div.bracketTitle {");
		overview.append("	position: relative;");
		overview.append("}");
		overview.append("img {");
		overview.append("	width: 200pt;");
		overview.append("	height: 300pt;");
		overview.append("	object-fit: cover;");
		overview.append("}");
		overview.append("img.bigpic {");
		overview.append("	width: 300pt;");
		overview.append("	height: unset;");
		overview.append("	padding-right: 10pt;");
		overview.append("}");
		overview.append("div.bracketTitle {");
		overview.append("	text-align: center;");
		overview.append("	font-size: 400%;");
		overview.append("}");
		overview.append("div.filmtitle, div.extrainfo {");
		overview.append("	text-align: center;");
		overview.append("	width: 200pt;");
		overview.append("	font-weight: bold;");
		overview.append("}");
		overview.append("div.filmtitle {");
		overview.append("	position: relative;");
		overview.append("	height: 30pt;");
		overview.append("}");
		overview.append("div.filmtitleheightadjust {");
		overview.append("	position: absolute; left: 0; right: 0; bottom: 0;");
		overview.append("}");
		String imgFrameCol = "#202";
		overview.append("div.imgsurrounder {");
		overview.append("	box-shadow: inset 0px 0px 5px 5px " + imgFrameCol + ";");
		overview.append("	height: 300pt;position: absolute;left: 0;top: 0;width: 200pt;");
		overview.append("}");
		overview.append("div.filminfo {");
		overview.append("	font-size: 200%;");
		overview.append("	padding-bottom: 8pt;");
		overview.append("}");
		overview.append("div.linkcontainer {");
		overview.append("	text-align: center;");
		overview.append("   padding: 10pt 0pt 25pt 0pt;");
		overview.append("}");
		overview.append("div.sidebysidecontainer {");
		overview.append("   padding-top: 20pt;");
		overview.append("}");
		overview.append("div.sidebyside {");
		overview.append("   vertical-align: top;");
		overview.append("}");
		overview.append("div.left {");
		overview.append("   position: absolute;");
		overview.append("}");
		overview.append("div.right {");
		overview.append("   padding-left: 310pt;");
		overview.append("   min-height: 420pt;");
		overview.append("}");
		overview.append("div.center {");
		overview.append("   text-align: center;");
		overview.append("}");
		overview.append("a.toplink {");
		overview.append("	font-size: 200%;");
		overview.append("	padding: 2pt 5pt 5pt 5pt;");
		overview.append("	margin: 10pt;");
		overview.append("	box-shadow: 5px 5px 5px #308, -5px 5px 5px #F3F, -5px -5px 5px #A0A, 5px -5px 5px #80F;");
		overview.append("	background-color: #70B;");
		overview.append("	border-radius: 10pt;");
		overview.append("	color: #FBF;");
		overview.append("}");
		overview.append("div.film > a {");
		overview.append("	position: relative;display: inline-block;");
		overview.append("}");
		overview.append("a {");
		overview.append("	text-decoration: none;");
		overview.append("	color: rgb(220, 120, 255);;");
		overview.append("}");
		overview.append("div.filmcontainer a {");
		overview.append("	color: rgb(136, 170, 255);");
		overview.append("}");
		overview.append("</style>");
		overview.append("</head>");

		// add links to other pages
		overview.append("<body");
		if (useFlatBackground) {
			overview.append(" style='background: " + imgFrameCol + ";'");
		}
		overview.append(">");
		overview.append("<div class='linkcontainer'>");
		overview.append("<div class='filmamount'>");
		overview.append("// " + filmAmount + " //");
		overview.append("</div>");
		overview.append("<a class='toplink' href='" + OVERVIEW + ".htm'>ABC</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_ADDITION + ".htm'>Newest</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_AMAZINGNESS + ".htm'>Amazingness</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_YEAR + ".htm'>Year</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_GENRES + ".htm'>Genre</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_LANGUAGES + ".htm'>Language</a>");
		overview.append("</div>");

		return overview;
	}

	private static void saveOverview(int filmAmount, List<String> genres, String filename, String overviewKind, String nullStr, String ovrStr, Map<String, String> genreToKeyMap) {

		StringBuilder overview = getHtmlTop(filmAmount, false);

		overview.append("<div class='bracketTitle'>");
		overview.append("Select a " + overviewKind + ":");
		overview.append("</div>");

		// add genres
		int i = 0;
		for (String genre : genres) {
			if (genre == null) {
				genre = nullStr;
			}
			overview.append("<div class='linkcontainer'>");
			overview.append("<a class='toplink' href='" + ovrStr);
			if (genreToKeyMap == null) {
				overview.append("" + i);
			} else {
				overview.append("_" + genreToKeyMap.get(genre));
			}
			overview.append(".htm'>" + HTML.escapeHTMLstr(genre) + "</a>");
			overview.append("</div>");

			i++;
		}
		overview.append("</body>");
		overview.append("</html>");

		// save overview
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.saveContent(overview);
	}

	private static void saveFilmsAsOverview(Map<String, List<Film>> films, String filename) {

		int filmAmount = 0;
		for (List<Film> filmList : films.values()) {
			filmAmount += filmList.size();
		}
		StringBuilder overview = getHtmlTop(filmAmount, true);

		String BECHDEL_BUTTON_DEFAULT = "Remove Films Not Passing Bechdel Test";
		overview.append("<script>\n");
		overview.append("window.toggleBechdel = function() {\n");
		overview.append("\tvar bechdelButton = document.getElementById('bechdelButton');\n");
		overview.append("\tvar bechdelFalseFilms = document.getElementsByClassName('film bechdelfalsefilm');\n");
		overview.append("\tif (bechdelButton.innerHTML == '" + BECHDEL_BUTTON_DEFAULT + "') {\n");
		overview.append("\t\tbechdelButton.innerHTML = 'Show Films Not Passing Bechdel Test';\n");
		overview.append("\t\tfor (var i = 0; i < bechdelFalseFilms.length; i++) {\n");
		overview.append("\t\t\tbechdelFalseFilms[i].style.display = 'none';\n");
		overview.append("\t\t}\n");
		overview.append("\t} else {\n");
		overview.append("\t\tbechdelButton.innerHTML = '" + BECHDEL_BUTTON_DEFAULT + "';\n");
		overview.append("\t\tfor (var i = 0; i < bechdelFalseFilms.length; i++) {\n");
		overview.append("\t\t\tbechdelFalseFilms[i].style.display = 'inline';\n");
		overview.append("\t\t}\n");
		overview.append("\t}\n");
		overview.append("}\n");
		overview.append("</script>\n");
		overview.append("<span id='bechdelButton' " +
			"style='position:fixed; bottom:0px; right:-30px; cursor: pointer; font-weight: bold; " +
			"background: radial-gradient(#202, #304, #202, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0)); " +
			"padding: 5px 40px; z-index: 10;' " +
			"onclick='toggleBechdel();'>");
		overview.append(BECHDEL_BUTTON_DEFAULT);
		overview.append("</span>");

		// add films
		for (Map.Entry<String, List<Film>> filmBracket : films.entrySet()) {
			String filmBracketLabel = filmBracket.getKey();
			List<Film> filmsInBracket = filmBracket.getValue();
			overview.append("<a name='" + filmBracketLabel + "'></a>");
			overview.append("<div class='bracketTitle'>");
			overview.append("<div class='filmamount'>");
			overview.append("// " + filmsInBracket.size() + " //");
			overview.append("</div>");
			overview.append(HTML.escapeHTMLstr(filmBracketLabel));
			overview.append("</div>");

			appendFilmsToOverview(overview, filmsInBracket);
		}
		overview.append("</body>");
		overview.append("</html>");

		// save overview
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.saveContent(overview);
	}

	private static void appendFilmsToOverview(StringBuilder overview, List<Film> filmsInBracket) {

		overview.append("<div class='filmcontainer'>");
		for (Film film : filmsInBracket) {
			film.appendAsHtmlToOverview(overview);
		}
		overview.append("</div>");
	}

	private static void saveFilmFile(Film film, Map<String, String> genreToKeyMap, Map<String, Integer> langToNumberMap, String filmpath, String filename) {

		StringBuilder overview = getHtmlTop(1, false);

		overview.append("<div class='bracketTitle'>");
		overview.append(HTML.escapeHTMLstr(film.getTitle()));
		overview.append("</div>");

		overview.append("<div class='sidebysidecontainer'>");

		overview.append("<div class='sidebyside left'>");
		if (film.getPreviewPic().contains("'")) {
			overview.append("<img class='bigpic' src=\"" + film.getPreviewPic() + "\"/>");
		} else {
			overview.append("<img class='bigpic' src='" + film.getPreviewPic() + "'/>");
		}
		overview.append("</div>");

		overview.append("<div class='sidebyside right'>");

		overview.append("<div class='filminfo'>");
		overview.append("Amazingness: <a href='" + OVERVIEW_BY_AMAZINGNESS + ".htm#" + HTML.escapeHTMLstr(film.getAmazingnessBracket()) + "'>" + HTML.escapeHTMLstr(film.getAmazingnessLongText()) + "</a>");
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("Review: " + HTML.escapeHTMLstr(film.getReview()));
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("Content Note: ");
		overview.append(HTML.escapeHTMLstr(film.getTriggerWarning()));
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("Bechdel Test: ");
		overview.append(film.getBechdelText());
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("From: <a href='" + OVERVIEW_BY_YEAR + ".htm#" + HTML.escapeHTMLstr(film.getYear()) + "'>" + HTML.escapeHTMLstr(film.getYear()) + "</a>");
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("Genres: " + film.getGenreHTML(genreToKeyMap));
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append(film.getLocationHTML(langToNumberMap));
		overview.append("</div>");

		overview.append("</div>");

		overview.append("</div>");

		if (film.getRelatedMovies().size() > 0) {
			overview.append("<div class='filminfo center'>");
			overview.append("<div class='filmamount'>");
			overview.append("// " + film.getRelatedMovies().size() + " //");
			overview.append("</div>");
			overview.append("Related Movies:");
			overview.append("</div>");

			appendFilmsToOverview(overview, film.getRelatedMovies());
		}

		// only show this if there are other movies, besides this one, with the same name
		if (film.getSimilarlyNamedMovies().size() > 1) {
			overview.append("<div class='filminfo center'>");
			overview.append("<div class='filmamount'>");
			overview.append("// " + film.getSimilarlyNamedMovies().size() + " //");
			overview.append("</div>");
			overview.append("Similarly Named Movies:");
			overview.append("</div>");

			appendFilmsToOverview(overview, film.getSimilarlyNamedMovies());
		}

		overview.append("</body>");
		overview.append("</html>");

		// save the file
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.saveContent(overview);
	}

	private static String mapFilmnameToSpecialFilmname(String filmname, Record filmMappings) {

		if (filmMappings.get(filmname) != null) {
			return filmMappings.getString(filmname);
		}

		return filmname;
	}

	private static void addFilmsThatShouldBeFound(List<String> filesThatShouldBeFound, String origin) {
		Directory parentDir = new Directory(origin);
		boolean recursively = true;
		for (File file : parentDir.getAllFiles(recursively)) {
			filesThatShouldBeFound.add(file.getCanonicalFilename());
		}
	}

	public static void foundMediaFileIn(String location) {
		filesFound.add(location);
	}

}
