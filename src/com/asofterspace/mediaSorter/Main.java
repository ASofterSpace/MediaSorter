/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import java.util.Collections;
import java.util.Comparator;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.HTML;
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
import java.util.TreeMap;
import java.util.Set;
import java.util.Map;


public class Main {

	public final static String PROGRAM_TITLE = "Media Sorter";
	public final static String VERSION_NUMBER = "0.0.0.3(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "31. August 2019 - 12. February 2020";

	private final static String[] TRY_PIC_ENDINGS = {"jpg", "jpeg", "gif", "png", "bmp"};

	private final static String OVERVIEW = "overview";
	private final static String OVERVIEW_BY_AMAZINGNESS = "overviewByAmazingness";
	private final static String OVERVIEW_BY_YEAR = "overviewByYear";
	private final static String OVERVIEW_BY_GENRES = "overviewByGenres";
	private final static String OVERVIEW_FILM = "overviewForFilm";

	private final static String NO_GENRE_SELECTED = "No Genre Assigned Yet";


	// TODO :: add links for amazingness and for genres directly from the media page
	// TODO :: make links purple
	// TODO :: add actual file locations
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

			if ("".equals(filmfilename)) {
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

			Film curFilm = new Film(filmname, filmfilename, filmcounter);
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
					curFilm.setReview(filmContents.get(j+2));
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
					for (int movieNum = j +1; movieNum < filmContents.size(); movieNum++) {
						String relatedMovie = filmContents.get(movieNum);
						if (!relatedMovie.startsWith("%[\\Desktop\\Filme\\")) {
							break;
						}
						relatedMovie = relatedMovie.substring("%[\\Desktop\\Filme\\".length());
						relatedMovie = relatedMovie.substring(0, relatedMovie.length() - 1);
						curFilm.addRelatedMovieName(relatedMovie);
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
		for (Film film : films) {
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
		statsFile.setEncoding(TextEncoding.ISO_LATIN_1);
		statsFile.saveContents(stats);

		System.out.println("Saved new statistics for " + filmcounter + " films at " + statsFile.getCanonicalFilename() + "!");

		// create all films overview
		Map<String, List<Film>> filmBrackets = new HashMap<>();
		filmBrackets.put("All Films", films);
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
			List<Film> curList = filmBrackets.get(film.getYear());
			if (curList == null) {
				curList = new ArrayList<>();
				filmBrackets.put(film.getYear(), curList);
			}
			curList.add(film);
		}

		saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_YEAR + ".htm");

		// create overviews for each genre
		Map<String, List<Film>> genreMap = new HashMap<>();
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

		genres.remove(null);
		Collections.sort(genres, new Comparator<String>() {
			public int compare(String a, String b) {
				return genreMap.get(b).size() - genreMap.get(a).size();
			}
		});
		// order "no genre selected yet" towards the end
		genres.add(null);

		int i = 0;

		for (String genre : genres) {
			List<Film> filmsOfThisGenre = genreMap.get(genre);
			filmBrackets = new HashMap<>();
			if (genre == null) {
				genre = NO_GENRE_SELECTED;
			}
			filmBrackets.put(genre, filmsOfThisGenre);
			saveFilmsAsOverview(filmBrackets, filmpath + "/" + OVERVIEW_BY_GENRES + i + ".htm");
			i++;
		}

		saveGenreOverview(genres, filmpath + "/" + OVERVIEW_BY_GENRES + ".htm");

		System.out.println("Saved overview HTML files!");

		for (Film film : films) {
			saveFilmFile(film, filmpath + "/" + OVERVIEW_FILM + "_" + film.getNumber() + ".htm");
		}

		System.out.println("Saved inidividual film files!");
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

	private static StringBuilder getHtmlTop() {

		StringBuilder overview = new StringBuilder();
		overview.append("<html>");

		// add header
		overview.append("<head>");
		overview.append("<style>");
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
		overview.append("img {");
		overview.append("	width: 200pt;");
		overview.append("}");
		overview.append("img.bigpic {");
		overview.append("	width: 300pt;");
		overview.append("	padding-right: 10pt;");
		overview.append("}");
		overview.append("div.bracketTitle {");
		overview.append("	text-align: center;");
		overview.append("	font-size: 400%;");
		overview.append("}");
		overview.append("div.filmtitle {");
		overview.append("	text-align: center;");
		overview.append("	width: 200pt;");
		overview.append("}");
		overview.append("div.extrainfo {");
		overview.append("	text-align: center;");
		overview.append("	width: 200pt;");
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
		overview.append("   display: flex;");
		overview.append("   padding-top: 20pt;");
		overview.append("}");/*
		overview.append("div.sidebyside {");
		overview.append("   vertical-align: top;");
		overview.append("}");*/
		overview.append("div.left {");
		overview.append("   width: 310pt;");
		overview.append("}");
		overview.append("div.right {");
		overview.append("   width: 100%;");
		overview.append("}");
		overview.append("div.center {");
		overview.append("   text-align: center;");
		overview.append("}");
		overview.append("a.toplink {");
		overview.append("	font-size: 200%;");
		overview.append("	padding: 2pt 5pt 5pt 5pt;");
		overview.append("	margin: 10pt;");
		overview.append("	box-shadow: 5px 5px 5px #FFF, -5px 5px 5px #D8D, -5px -5px 5px #A0A, 5px -5px 5px #FCF;");
		overview.append("	background-color: white;");
		overview.append("	border-radius: 10pt;");
		overview.append("}");
		overview.append("a {");
		overview.append("	text-decoration: none;");
		overview.append("	color: #A0A;");
		overview.append("}");
		overview.append("</style>");
		overview.append("</head>");

		// add links to other pages
		overview.append("<body>");
		overview.append("<div class='linkcontainer'>");
		overview.append("<a class='toplink' href='" + OVERVIEW + ".htm'>All Films</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_AMAZINGNESS + ".htm'>By Amazingness</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_YEAR + ".htm'>By Year</a>");
		overview.append("<a class='toplink' href='" + OVERVIEW_BY_GENRES + ".htm'>Select Genre</a>");
		overview.append("</div>");

		return overview;
	}

	private static void saveGenreOverview(List<String> genres, String filename) {

		StringBuilder overview = getHtmlTop();

		overview.append("<div class='bracketTitle'>");
		overview.append("Select a Genre:");
		overview.append("</div>");

		// add genres
		int i = 0;
		for (String genre : genres) {
			if (genre == null) {
				genre = NO_GENRE_SELECTED;
			}
			overview.append("<div class='linkcontainer'>");
			overview.append("<a class='toplink' href='" + OVERVIEW_BY_GENRES + i + ".htm'>" + HTML.escapeHTMLstr(genre) + "</a>");
			overview.append("</div>");

			i++;
		}
		overview.append("</body>");
		overview.append("</html>");

		// save overview
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.setEncoding(TextEncoding.ISO_LATIN_1);
		overviewFile.saveContent(overview);
	}

	private static void saveFilmsAsOverview(Map<String, List<Film>> films, String filename) {

		StringBuilder overview = getHtmlTop();

		// add films
		for (Map.Entry<String, List<Film>> filmBracket : films.entrySet()) {
		    String filmBracketLabel = filmBracket.getKey();
		    List<Film> filmsInBracket = filmBracket.getValue();
			overview.append("<a name='" + filmBracketLabel + "'></a>");
			overview.append("<div class='bracketTitle'>");
			overview.append(HTML.escapeHTMLstr(filmBracketLabel));
			overview.append("</div>");

			appendFilmsToOverview(overview, filmsInBracket);
		}
		overview.append("</body>");
		overview.append("</html>");

		// save overview
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.setEncoding(TextEncoding.ISO_LATIN_1);
		overviewFile.saveContent(overview);
	}

	private static void appendFilmsToOverview(StringBuilder overview, List<Film> filmsInBracket) {

		overview.append("<div class='filmcontainer'>");
		for (Film film : filmsInBracket) {
			overview.append("<div class='film'>");
			overview.append("<div class='filmtitle'>");
			overview.append(HTML.escapeHTMLstr(film.getTitle()));
			overview.append("</div>");
			overview.append("<div class='extrainfo'>");
			overview.append(HTML.escapeHTMLstr(film.getYear()));
			overview.append(" &loz; ");
			overview.append(HTML.escapeHTMLstr(film.getAmazingnessShortText()));
			overview.append("</div>");
			overview.append("<a href='" + OVERVIEW_FILM + "_" + film.getNumber() + ".htm'>");
			if (film.getPreviewPic().contains("'")) {
				overview.append("<img src=\"" + film.getPreviewPic() + "\"/>");
			} else {
				overview.append("<img src='" + film.getPreviewPic() + "'/>");
			}
			overview.append("</a>");
			overview.append("</div>");
		}
		overview.append("</div>");
	}

	private static void saveFilmFile(Film film, String filename) {

		StringBuilder overview = getHtmlTop();

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
		overview.append("From: <a href='" + OVERVIEW_BY_YEAR + ".htm#" + HTML.escapeHTMLstr(film.getYear()) + "'>" + HTML.escapeHTMLstr(film.getYear()) + "</a>");
		overview.append("</div>");

		overview.append("<div class='filminfo'>");
		overview.append("Genres: " + HTML.escapeHTMLstr(film.getGenreText()));
		overview.append("</div>");

		overview.append("</div>");

		overview.append("</div>");

		if (film.getRelatedMovies().size() > 0) {
			overview.append("<div class='filminfo center'>");
			overview.append("Related Movies:");
			overview.append("</div>");

			appendFilmsToOverview(overview, film.getRelatedMovies());
		}

		overview.append("</body>");
		overview.append("</html>");

		// save the file
		SimpleFile overviewFile = new SimpleFile(filename);
		overviewFile.setEncoding(TextEncoding.ISO_LATIN_1);
		overviewFile.saveContent(overview);
	}

}
