/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import java.util.List;
import java.util.ArrayList;
import com.asofterspace.toolbox.utils.StrUtils;


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


	public Film(String title, String filename, int number) {
		this.title = title;
		this.filename = filename;
		this.number = number;
		this.genres = new ArrayList<>();
		this.relatedMovieNames = new ArrayList<>();
		this.relatedMovies = new ArrayList<>();
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
			return "not yet graded";
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

	public String getGenreText() {
		if (genres.size() < 1) {
			return "???";
		}
		return StrUtils.join(genres, " / ");
	}

	public List<String> getGenres() {
		return genres;
	}

	public void addGenre(String genre) {
		if (genre != null) {
			this.genres.add(genre);
		}
	}

	public String getPreviewPic() {
		return previewPic;
	}

	public void setPreviewPic(String previewPic) {
		this.previewPic = previewPic;
	}

	public String getReview() {
		if ((review == null) || "Not seen yet!".equals(review)) {
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
}
