/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.series;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.utils.SortUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Series {

	private String name;

	private List<Directory> entrypoints = new ArrayList<>();

	private static int idCounter = 0;


	public Series(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addDir(Directory entrypoint) {
		this.entrypoints.add(entrypoint);
	}

	public void appendHTML(StringBuilder html, int depth) {

		idCounter++;
		String indentStr = " style='padding-left:" + (15*(depth-2)) + "pt;";

		html.append("<h" + depth + indentStr + "cursor:pointer;' onclick='toggle(" + idCounter + ")'>");
		html.append(HTML.escapeHTMLstr(name));
		html.append("</h" + depth + ">");

		html.append("<div id='cont" + idCounter + "' style='display: none;'>");

		Map<String, Series> nameToSubSeries = new HashMap<>();

		for (Directory dir : entrypoints) {
			boolean recursively = false;
			List<Directory> curDirs = dir.getAllDirectories(recursively);
			for (Directory curDir : curDirs) {
				String curName = curDir.getLocalDirname();
				Series curSubSeries = nameToSubSeries.get(curName);
				if (curSubSeries == null) {
					curSubSeries = new Series(curName);
					nameToSubSeries.put(curName, curSubSeries);
				}
				curSubSeries.addDir(curDir);
			}
		}

		List<String> subDirNames = SortUtils.sort(nameToSubSeries.keySet());

		for (String subDirName : subDirNames) {
			nameToSubSeries.get(subDirName).appendHTML(html, depth + 1);
		}

		List<String> curFileNames = new ArrayList<>();

		for (Directory dir : entrypoints) {
			boolean recursively = false;
			List<File> curFiles = dir.getAllFiles(recursively);
			for (File curFile : curFiles) {
				String filename = curFile.getLocalFilename();
				if ((!filename.endsWith(".txt") && !filename.endsWith(".stpu") && !filename.endsWith(".jpg")) &&
					(!curFileNames.contains(filename))) {
					curFileNames.add(filename);
				}
			}
		}

		curFileNames = SortUtils.sort(curFileNames);

		for (String curFileName : curFileNames) {
			html.append("<div" + indentStr + "'>");
			html.append(HTML.escapeHTMLstr(curFileName));
			html.append("</div>");
		}

		html.append("</div>");
	}

}
