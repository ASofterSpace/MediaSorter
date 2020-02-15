/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import java.util.List;
import java.util.ArrayList;
import com.asofterspace.toolbox.utils.StrUtils;


public class Film {

	private String title;

	private int number;

	private Integer amazingness;

	private String year;

	private List<String> genres;

	private String previewPic;


	public Film(String title, int number) {
		this.title = title;
		this.number = number;
		this.genres = new ArrayList<>();
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
}
