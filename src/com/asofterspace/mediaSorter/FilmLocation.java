/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.io.HTML;

import java.util.ArrayList;
import java.util.List;


public class FilmLocation {

	private Film film;

	private String languageStr;
	private String subtitleLangStr;
	private String qualityStr;
	private String editionStr;
	private List<String> locationStrs;


	public FilmLocation(Film film) {
		this.film = film;
		this.locationStrs = new ArrayList<>();
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

	public void parseQuality(String line) {
		qualityStr = line;
	}

	public void parseEdition(String line) {
		editionStr = line;
	}

	public void parseLocation(String line) {
		line = line.substring(10);
		line = line.substring(0, line.length() - 1);
		if (locationStrs.size() == 1) {
			System.out.println("Btw., " + film.getTitle() + " contains several locations");
		}
		locationStrs.add(line);
	}

	public String getLanguageText() {
		return languageStr;
	}

	public String getSubtitleText() {
		return subtitleLangStr;
	}

	public boolean hasQualityNotice() {
		return qualityStr != null;
	}

	public String getQualityNoticeText() {
		return qualityStr;
	}

	public boolean hasEdition() {
		return editionStr != null;
	}

	public String getEditionText() {
		return editionStr;
	}

	public String getLocationHTMLstr() {
		if (locationStrs.size() == 1) {
			return "Location: " + HTML.escapeHTMLstr(locationStrs.get(0));
		}
		StringBuilder result = new StringBuilder();
		String sep = "";
		int i = 1;
		for (String locationStr : locationStrs) {
			result.append(sep);
			sep = "<br>";
			result.append(HTML.escapeHTMLstr("Location #" + i + ": " + locationStr));
			i++;
		}
		return result.toString();
	}

}
