/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;


public class FilmLocation {

	private Film film;
	private String fileLocationOrigin;
	private String fileLocationOriginAlt;

	private String languageStr;
	private List<String> languages;
	private String subtitleLangStr;
	private String qualityStr;
	private String editionStr;
	private String noteStr;
	private List<String> locationStrs;


	public FilmLocation(Film film, String fileLocationOrigin, String fileLocationOriginAlt) {
		this.film = film;
		this.fileLocationOrigin = fileLocationOrigin;
		this.fileLocationOriginAlt = fileLocationOriginAlt;
		this.locationStrs = new ArrayList<>();
		this.languages = new ArrayList<>();
	}

	public void parseLanguages(String line) {
		if (languageStr != null) {
			System.err.println(film.getTitle() + " contains several language declarations!");
		}
		languageStr = line;

		// extract distinct languages... we first split on |, then take the first for each,
		// so e.g. English and French | German will turn into {English, German} (as the
		// first each is the "main" one)
		String[] lines = line.split("\\|");
		for (String curLine : lines) {
			curLine = curLine.trim();
			if (curLine.contains(" ")) {
				curLine = curLine.substring(0, curLine.indexOf(" "));
			}
			if (curLine.endsWith(",")) {
				curLine = curLine.substring(0, curLine.length() - 1);
			}
			languages.add(curLine.trim());
		}
	}

	public void parseSubtitleLanguages(String line) {
		if (subtitleLangStr != null) {
			System.err.println(film.getTitle() + " contains several subtitle language declarations!");
		}
		subtitleLangStr = line;
	}

	public void parseQuality(String line) {
		qualityStr = line.trim();
	}

	public void parseEdition(String line) {
		editionStr = line.trim();
	}

	public void parseNote(String line) {
		noteStr = line.trim();
	}

	public void parseLocation(String line) {
		line = line.substring(10);
		line = line.substring(0, line.length() - 1);
		line = StrUtils.replaceAll(line, "\\", "/");
		if (locationStrs.size() == 1) {
			System.out.println("Btw., " + film.getTitle() + " contains several locations");
		}
		locationStrs.add(line);
	}

	public String getLanguageText() {
		return languageStr;
	}

	public List<String> getLanguages() {
		return languages;
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

	public boolean hasNote() {
		return noteStr != null;
	}

	public String getNoteText() {
		return noteStr;
	}

	public String getLocationHTMLstr() {
		String result = getLocationHTMLmainStr();
		String container = locationStrs.get(0);
		if (container.contains("\\")) {
			container = container.substring(0, container.lastIndexOf("\\"));
		}
		if (container.contains("/")) {
			container = container.substring(0, container.lastIndexOf("/"));
		}
		Directory containerDir = new Directory(fileLocationOrigin + container);
		if (containerDir.exists()) {
			result += "<br>Contained in: <a href='file:///" + fileLocationOrigin + HTML.escapeHTMLstr(container) + "'>" + HTML.escapeHTMLstr(container) + "</a>";
			return result;
		}
		if (fileLocationOriginAlt != null) {
			containerDir = new Directory(fileLocationOriginAlt + container);
			if (containerDir.exists()) {
				result += "<br>Contained in: <a href='file:///" + fileLocationOriginAlt + HTML.escapeHTMLstr(container) + "'>" + HTML.escapeHTMLstr(container) + "</a>";
				return result;
			}
		}
		System.err.println("Folder " + fileLocationOrigin + container + " does not exist!");
		result += "<br>Contained in: <a href='file:///" + fileLocationOrigin + HTML.escapeHTMLstr(container) + "'>" + HTML.escapeHTMLstr(container) + "</a>";
		return result;
	}

	private String getLocationHTMLmainStr() {
		if (locationStrs.size() == 1) {
			String origin = checkExistence(locationStrs.get(0));
			return "Location: <a href='file:///" + origin + HTML.escapeHTMLstr(locationStrs.get(0)) + "'>" + HTML.escapeHTMLstr(locationStrs.get(0)) + "</a>";
		}
		StringBuilder result = new StringBuilder();
		String sep = "";
		int i = 1;
		for (String locationStr : locationStrs) {
			result.append(sep);
			sep = "<br>";
			String origin = checkExistence(locationStr);
			result.append("Location #" + i + ": <a href='file:///" + origin + HTML.escapeHTMLstr(locationStr) + "'>" + HTML.escapeHTMLstr(locationStr) + "</a>");
			i++;
		}
		return result.toString();
	}

	private String checkExistence(String fileName) {
		File mediaFile = new File(new Directory(fileLocationOrigin), fileName);
		if (mediaFile.exists()) {
			Main.foundMediaFileIn(mediaFile.getCanonicalFilename());
			return fileLocationOrigin;
		}

		if (fileLocationOriginAlt != null) {
			mediaFile = new File(new Directory(fileLocationOriginAlt), fileName);
			if (mediaFile.exists()) {
				Main.foundMediaFileIn(mediaFile.getCanonicalFilename());
				return fileLocationOriginAlt;
			}
		}

		System.err.println("File " + mediaFile.getAbsoluteFilename() + " does not exist!");
		return fileLocationOrigin;
	}

}
