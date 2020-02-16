/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.io.HTML;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Film {

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


	public Film(String title, String filename, int number) {
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

	public void setYear(String year) {
		this.year = year;
	}

	private String toGenreLink(String genreStr, String key, String filmpath, Map<String, Integer> genreToNumberMap) {
		return "<a href='" + filmpath + "/" + Main.OVERVIEW_BY_GENRES +
				genreToNumberMap.get(key) + ".htm'>" + HTML.escapeHTMLstr(genreStr) + "</a>";
	}

	public String getGenreHTML(String filmpath, Map<String, Integer> genreToNumberMap) {
		if ((genres.size() < 1) || (genres.get(0) == null)) {
			return toGenreLink("No genre selected yet", null, filmpath, genreToNumberMap);
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
				result.append(toGenreLink(genre, genre, filmpath, genreToNumberMap));
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

	public String getLocationHTML(String filmpath) {
		if (filmLocations.size() < 1) {
			System.err.println("We do not actually have " + title + "!");
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (FilmLocation loc : filmLocations) {
			result.append("<br>");
			result.append("Language: " + HTML.escapeHTMLstr(loc.getLanguageText()));
			result.append("<br>");
			result.append("Subtitles: " + HTML.escapeHTMLstr(loc.getSubtitleText()));
			result.append("<br>");
			result.append(loc.getLocationHTMLstr());
			result.append("<br>");
		}
		return result.toString();
	}
}
