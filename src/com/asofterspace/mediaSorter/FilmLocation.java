/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

public class FilmLocation {

	private Film film;

	private String languageStr;
	private String subtitleLangStr;
	private String locationStr;


	public FilmLocation(Film film) {
		this.film = film;
	}

	public void parseLanguages(String line) {
		if (languageStr != null) {
			System.err.println(film.getTitle() + " contains several language declarations!");
		}
		languageStr = line;
	}

	public void parseSubtitleLanguages(String line) {
		if (subtitleLangStr != null) {
			System.err.println(film.getTitle() + " contains several subtitle language declarations!");
		}
		subtitleLangStr = line;
	}

	public void parseLocation(String line) {
		if (locationStr != null) {
			System.err.println(film.getTitle() + " contains several location declarations!");
		}
		locationStr = line;
	}

	public String getLanguageText() {
		return languageStr;
	}

	public String getSubtitleText() {
		return subtitleLangStr;
	}

	public String getLocationText() {
		return locationStr;
	}

}
