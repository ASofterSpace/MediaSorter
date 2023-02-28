/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Film {

	private Date additionDate;

	// the human-readable title of the film
	private String title;

	// the filename that the movie index file had
	private String filename;

	private int number;

	private Integer amazingness;

	private String year;

	private List<String> genres;

	private String previewPic;

	private String review;

	private List<String> relatedMovieNames;

	private List<Film> relatedMovies;

	private List<Film> similarlyNamedMovies;

	private List<FilmLocation> filmLocations;

	private Boolean bechdel = null;
	private Map<String, String> bechdelTimes = new HashMap<>();


	public Film(File baseFile, String title, String filename, int number) {

		additionDate = baseFile.getCreationDate();
		Date otherDate = baseFile.getChangeDate();
		if (otherDate.before(additionDate)) {
			additionDate = otherDate;
		}
		String picFileName = baseFile.getFilename();
		if (picFileName.endsWith(".stpu")) {
			picFileName = picFileName.substring(0, picFileName.lastIndexOf("."));
		}
		picFileName += "_1.jpg";
		File picFile = new File(picFileName);
		if (picFile.exists()) {
			otherDate = picFile.getCreationDate();
			if (otherDate.before(additionDate)) {
				additionDate = otherDate;
			}
			otherDate = picFile.getChangeDate();
			if (otherDate.before(additionDate)) {
				additionDate = otherDate;
			}
		}
		this.title = title;
		this.filename = filename;
		this.number = number;
		this.genres = new ArrayList<>();
		this.relatedMovieNames = new ArrayList<>();
		this.relatedMovies = new ArrayList<>();
		this.similarlyNamedMovies = new ArrayList<>();
		this.filmLocations = new ArrayList<>();
	}

	public String getTitle() {
		return title;
	}

	public int getNumber() {
		return number;
	}

	public Integer getAmazingness() {
		return amazingness;
	}

	public String getAmazingnessShortText() {
		if (amazingness == null) {
			return "?";
		}
		return amazingness + "/10";
	}

	public String getAmazingnessLongText() {
		if (amazingness == null) {
			return "Not yet graded";
		}
		return amazingness + " out of 10";
	}

	public String getAmazingnessBracket() {
		if (amazingness == null) {
			return "Not Yet Graded";
		}
		return amazingness + " out of 10";
	}

	public void setAmazingness(Integer amazingness) {
		this.amazingness = amazingness;
	}

	public String getYear() {
		return year;
	}

	public String getAdditionYearAndMonth() {
		if (additionDate == null) {
			return null;
		}
		return DateUtils.getYear(additionDate) + " / " +
			StrUtils.leftPad0(DateUtils.getMonth(additionDate), 2);
	}

	// if the database has this film, take the database's value for addition date
	// if not, take this date and store it in the database
	public void consolidateAdditionDateWithDatabase(Database database) {
		Date dbDate = database.getFilmDate(title);
		if (dbDate == null) {
			database.storeFilmDate(title, additionDate);
		} else {
			this.additionDate = dbDate;
		}
	}

	public void setYear(String year) {
		this.year = year;
	}

	private String toGenreLink(String genreStr, String key, Map<String, String> genreToKeyMap) {
		return "<a href='" + Main.OVERVIEW_BY_GENRES + "_" +
				genreToKeyMap.get(key) + ".htm'>" + HTML.escapeHTMLstr(genreStr) + "</a>";
	}

	public String getGenreHTML(Map<String, String> genreToKeyMap) {
		if ((genres.size() < 1) || (genres.get(0) == null)) {
			return toGenreLink("No genre selected yet", null, genreToKeyMap);
		}

		StringBuilder result = new StringBuilder();
		boolean firstElem = true;
		for (String genre : genres) {
			if (genre != null) {
				if (firstElem) {
					firstElem = false;
				} else {
					result.append(" / ");
				}
				result.append(toGenreLink(genre, genre, genreToKeyMap));
			}
		}
		return result.toString();
	}

	public List<String> getGenres() {
		return genres;
	}

	public void addGenre(String genre) {
		this.genres.add(genre);
	}

	public String getPreviewPic() {
		return previewPic;
	}

	public void setPreviewPic(String previewPic) {
		this.previewPic = previewPic;
	}

	public String getReview() {
		if ((review == null) || review.startsWith("Not seen yet")) {
			return "Not yet reviewed";
		}
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public void addRelatedMovieName(String name) {
		relatedMovieNames.add(name);
	}

	private boolean isNameSimilar(Film other) {
		String thisTitle = StrUtils.replaceAll(StrUtils.replaceAll(title.toLowerCase().trim(), ":", " "), "-", " ");
		List<String> thisTitleWords = StrUtils.split(thisTitle, " ");

		String otherTitle = StrUtils.replaceAll(StrUtils.replaceAll(other.title.toLowerCase().trim(), ":", " "), "-", " ");
		List<String> otherTitleWords = StrUtils.split(otherTitle, " ");

		// ignore any words below this length, such as articles
		int IGNORE_BELOW_LEN = 4;

		for (String thisTitleWord : thisTitleWords) {
			if (thisTitleWord.length() < IGNORE_BELOW_LEN) {
				continue;
			}
			for (String otherTitleWord : otherTitleWords) {
				if (otherTitleWord.length() < IGNORE_BELOW_LEN) {
					continue;
				}
				if (thisTitleWord.equals(otherTitleWord)) {
					return true;
				}
			}
		}

		return false;
	}

	public void resolveRelatedMovies(List<Film> films) {
		for (String name : relatedMovieNames) {
			boolean foundIt = false;
			for (Film film : films) {
				if (name.equals(film.filename)) {
					relatedMovies.add(film);
					foundIt = true;
					break;
				}
			}
			if (!foundIt) {
				System.err.println("For " + title + " could not find related movie " + name + "!");
			}
		}

		for (Film film : films) {
			if (isNameSimilar(film)) {
				similarlyNamedMovies.add(film);
			}
		}
	}

	public List<Film> getRelatedMovies() {
		return relatedMovies;
	}


	public List<Film> getSimilarlyNamedMovies() {
		return similarlyNamedMovies;
	}

	public void addFilmLocation(FilmLocation filmLocation) {
		filmLocations.add(filmLocation);
	}

	public List<FilmLocation> getFilmLocations() {
		return filmLocations;
	}

	public Boolean getBechdel() {
		return bechdel;
	}

	public void setBechdel(String bechdelStr) {
		if (bechdelStr != null) {
			String[] bechdelStrs = bechdelStr.split(",");
			bechdelStr = bechdelStr.toLowerCase();

			for (int i = 1; i < bechdelStrs.length; i++) {
				String cur = bechdelStrs[i].trim();
				if (cur.contains(":")) {
					String key = cur;
					String value = "";
					if (cur.contains(" ")) {
						key = cur.substring(0, cur.indexOf(" ")).trim();
						value = cur.substring(cur.indexOf(" ")).trim();
						if (value.startsWith("(")) {
							value = value.substring(1).trim();
						}
						if (value.endsWith(")")) {
							value = value.substring(0, value.length() - 1).trim();
						}
					}
					key = key.toLowerCase();
					value = value.toLowerCase();
					this.bechdelTimes.put(key, value);
				}
			}

			if (bechdelStr.startsWith("no")) {
				this.bechdel = false;
				return;
			}
			if (bechdelStr.startsWith("unknown")) {
				this.bechdel = null;
				return;
			}
			if (bechdelStr.startsWith("yes")) {
				this.bechdel = true;
				return;
			}
		}
		System.err.println("BechdelStr could not be interpreted in movie " + title + ": \"" + bechdelStr + "\"");
	}

	public String getBechdelText() {

		StringBuilder result = new StringBuilder();

		if (bechdel == null) {

			result.append("? No idea if it passes!");

		} else {

			if (bechdel) {
				result.append("&#x2640; ");

				String bechdelTime = null;
				for (Map.Entry<String, String> entry : bechdelTimes.entrySet()) {
					bechdelTime = entry.getKey();
				}

				if (bechdelTime == null) {
					result.append("A");
				} else {
					result.append("Starting at " + bechdelTime + ", a");
				}
				result.append("t least two women who are named characters are talking for at least one minute about a topic other than a man. :)");

			} else {

				result.append("&#x2620; Does not pass!");

				for (Map.Entry<String, String> entry : bechdelTimes.entrySet()) {
					result.append(" Starting at " + entry.getKey() + ", ");
					String value = entry.getValue();
					switch (value) {
						case "not a named character":
							result.append("women are talking but they are not all named characters.");
							break;
						case "less than a minute":
							result.append("women are talking but for less than a minute.");
							break;
						case "talking about a man":
							result.append("women are talking but the topic of their conversation is a man.");
							break;
						default:
							result.append("women are talking but it does not count.");
							System.err.println("BechdelStr value '" + value + "' could not be interpreted in movie " + title + "!");
							break;
					}
				}
			}
		}

		return result.toString();
	}

	public Set<String> getLanguages() {
		Set<String> languages = new HashSet<>();
		for (FilmLocation loc : filmLocations) {
			for (String lang : loc.getLanguages()) {
				languages.add(lang);
			}
		}
		return languages;
	}

	public String getLanguageShortText() {
		Set<String> langs = getLanguages();
		String result = "";
		String sep = "";
		for (String lang : langs) {
			result += sep;
			sep = ", ";
			if (lang == null) {
				result += "-";
			} else {
				switch (lang) {
					case "none":
						result += "-";
						break;
					case "German":
						result += "DE";
						break;
					case "Japanese":
						result += "JP";
						break;
					case "Spanish":
						result += "ES";
						break;
					case "Icelandic":
						result += "IS";
						break;
					default:
						result += lang.substring(0, 2).toUpperCase();
				}
			}
		}
		return result;
	}

	public String getLocationHTML(Map<String, Integer> langToNumberMap) {
		if (filmLocations.size() < 1) {
			System.err.println("We do not actually have " + title + "!");
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (FilmLocation loc : filmLocations) {
			result.append("<br>");
			String langStr = HTML.escapeHTMLstr(loc.getLanguageText());
			for (String language : langToNumberMap.keySet()) {
				if (langStr.contains(language)) {
					langStr = langStr.replace(
						language,
						"<a href='" + Main.OVERVIEW_BY_LANGUAGES + langToNumberMap.get(language) + ".htm'>" + language + "</a>"
					);
				}
			}
			result.append("Language: " + langStr);
			result.append("<br>");
			result.append("Subtitles: " + HTML.escapeHTMLstr(loc.getSubtitleText()));
			result.append("<br>");
			if (loc.hasQualityNotice()) {
				result.append("Quality: " + HTML.escapeHTMLstr(loc.getQualityNoticeText()));
				result.append("<br>");
			}
			if (loc.hasEdition()) {
				result.append("Edition: " + HTML.escapeHTMLstr(loc.getEditionText()));
				result.append("<br>");
			}
			if (loc.hasNote()) {
				result.append("Note: " + HTML.escapeHTMLstr(loc.getNoteText()));
				result.append("<br>");
			}
			result.append(loc.getLocationHTMLstr());
			result.append("<br>");
		}
		return result.toString();
	}

	public void consolidate() {
		// could consolide some stuff here, if I wanted to xD
	}

	public void appendAsHtmlToOverview(StringBuilder overview) {
		String aHref = "<a href='" + Main.OVERVIEW_FILM + "_" + this.getNumber() + ".htm'>";

		Boolean bechdelTrueOrFalseOnly = this.getBechdel();
		if (bechdelTrueOrFalseOnly == null) {
			bechdelTrueOrFalseOnly = false;
		}
		overview.append("<div class='film bechdel" + bechdelTrueOrFalseOnly + "film'>");

		overview.append("<div class='filmtitle'>");
		overview.append(aHref);
		overview.append(HTML.escapeHTMLstr(this.getTitle()));
		overview.append("</a>");
		overview.append("</div>");

		overview.append("<div class='extrainfo'>");
		overview.append(HTML.escapeHTMLstr(this.getYear()));
		overview.append(" &loz; ");
		overview.append(HTML.escapeHTMLstr(this.getLanguageShortText()));
		overview.append(" &loz; ");
		if (this.getBechdel() == null) {
			overview.append("?");
		} else {
			if (this.getBechdel()) {
				overview.append("<span style='position:relative'>");
				overview.append("<span style='position:absolute;bottom:1px;'>&#x2640;</span>");
				overview.append("<span style='position:absolute;bottom:2px;'>&#x2640;</span>");
				overview.append("<span style='position:absolute;bottom:1px;left:-1px'>&#x2640;</span>");
				overview.append("<span style='position:absolute;bottom:2px;left:-1px'>&#x2640;</span>");
				overview.append("&nbsp;&nbsp;");
				overview.append("</span>");
			} else {
				overview.append("&#x2620;");
			}
		}
		overview.append(" &loz; ");
		overview.append(HTML.escapeHTMLstr(this.getAmazingnessShortText()));
		overview.append("</div>");

		overview.append(aHref);
		if (this.getPreviewPic().contains("'")) {
			overview.append("<img src=\"" + this.getPreviewPic() + "\"/>");
		} else {
			overview.append("<img src='" + this.getPreviewPic() + "'/>");
		}
		overview.append("</a>");

		overview.append("</div>");
	}

}
