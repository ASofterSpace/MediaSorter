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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Film {

	private String additionYearAndMonth;

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

	private List<FilmLocation> filmLocations;


	public Film(File baseFile, String title, String filename, int number) {

		Date additionDate = baseFile.getCreationDate();
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
		this.additionYearAndMonth = DateUtils.getYear(additionDate) + " / " +
			StrUtils.leftPad0(DateUtils.getMonth(additionDate), 2);

		this.title = title;
		this.filename = filename;
		this.number = number;
		this.genres = new ArrayList<>();
		this.relatedMovieNames = new ArrayList<>();
		this.relatedMovies = new ArrayList<>();
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
		return additionYearAndMonth;
	}

	public void setYear(String year) {
		this.year = year;
	}

	private String toGenreLink(String genreStr, String key, Map<String, Integer> genreToNumberMap) {
		return "<a href='" + Main.OVERVIEW_BY_GENRES +
				genreToNumberMap.get(key) + ".htm'>" + HTML.escapeHTMLstr(genreStr) + "</a>";
	}

	public String getGenreHTML(Map<String, Integer> genreToNumberMap) {
		if ((genres.size() < 1) || (genres.get(0) == null)) {
			return toGenreLink("No genre selected yet", null, genreToNumberMap);
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
				result.append(toGenreLink(genre, genre, genreToNumberMap));
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
	}

	public List<Film> getRelatedMovies() {
		return relatedMovies;
	}

	public void addFilmLocation(FilmLocation filmLocation) {
		filmLocations.add(filmLocation);
	}

	public List<FilmLocation> getFilmLocations() {
		return filmLocations;
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
}
