/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.comics;

import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.io.SimpleFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Comic {

	private String name;

	private List<Directory> entrypoints = new ArrayList<>();

	private static int idCounter = 0;


	public Comic(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addDir(Directory entrypoint) {
		this.entrypoints.add(entrypoint);
	}

	public void appendAsHtmlToUploadPage(StringBuilder html, int depth) {

		idCounter++;
		String indentStr = " style='padding-left:" + (15*(depth-2)) + "pt;";
		boolean isEmpty = true;

		html.append("<h" + depth + indentStr + "cursor:pointer;' onclick='toggle(" + idCounter + ")'>");
		html.append(HTML.escapeHTMLstr(name));
		html.append(" <div id='tri" + idCounter + "' class='tri'>&gt;</div>");
		html.append("</h" + depth + ">");

		html.append("<div id='cont" + idCounter + "' style='display: none;'>");

		Map<String, Comic> nameToSubComic = new HashMap<>();
		List<String> subDirNames = new ArrayList<>();
		List<String> curFileNames = new ArrayList<>();

		for (Directory dir : entrypoints) {
			SimpleFile vstpuFile = new SimpleFile(dir, "VSTPU.stpu");
			vstpuFile.setISOorUTFreadAndUTFwriteEncoding(true);
			List<String> lines = vstpuFile.getContents();
			if (lines != null) {
				for (String curName : lines) {
					String trimName = curName.trim();
					if (trimName.equals("") || trimName.endsWith(":")) {
						continue;
					}
					Directory curDir = new Directory(dir, curName);
					if (curDir.exists()) {
						File skipFile = new File(curDir, MovieSorter.SKIP_FILE_NAME);
						if (skipFile.exists()) {
							continue;
						}
						Comic curSubComic = nameToSubComic.get(curName);
						if (curSubComic == null) {
							curSubComic = new Comic(curName);
							nameToSubComic.put(curName, curSubComic);
							subDirNames.add(curName);
						}
						curSubComic.addDir(curDir);
					} else {
						curFileNames.add(curName);
					}
				}
			}
		}

		for (String subDirName : subDirNames) {
			nameToSubComic.get(subDirName).appendAsHtmlToUploadPage(html, depth + 1);
			isEmpty = false;
		}

		depth++;
		indentStr = " style='padding-left:" + (15*(depth-2)) + "pt;";

		for (String curFileName : curFileNames) {
			html.append("<div" + indentStr + "'>");
			html.append(HTML.escapeHTMLstr(curFileName));
			html.append("</div>");
			isEmpty = false;
		}

		if (isEmpty) {
			html.append("<div" + indentStr + "'>");
			html.append("<i>(Mysteriously and ominously, this folder is empty.)</i>");
			html.append("</div>");
		}

		html.append("</div>");
	}

}
