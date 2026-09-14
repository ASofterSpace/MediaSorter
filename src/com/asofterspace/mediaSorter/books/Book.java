/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.books;

import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.utils.SortUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Book {

	private String name;

	private List<Directory> entrypoints = new ArrayList<>();

	private static int idCounter = 0;


	public Book(String name) {
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

		Map<String, Book> nameToSubBook = new HashMap<>();

		for (Directory dir : entrypoints) {
			boolean recursively = false;
			List<Directory> curDirs = dir.getAllDirectories(recursively);
			for (Directory curDir : curDirs) {
				String curName = curDir.getLocalDirname();
				File skipFile = new File(curDir, MovieSorter.SKIP_FILE_NAME);
				if (skipFile.exists()) {
					continue;
				}
				Book curSubBook = nameToSubBook.get(curName);
				if (curSubBook == null) {
					curSubBook = new Book(curName);
					nameToSubBook.put(curName, curSubBook);
				}
				curSubBook.addDir(curDir);
			}
		}

		List<String> subDirNames = SortUtils.sort(nameToSubBook.keySet());

		for (String subDirName : subDirNames) {
			nameToSubBook.get(subDirName).appendAsHtmlToUploadPage(html, depth + 1);
			isEmpty = false;
		}

		List<String> curFileNames = new ArrayList<>();

		for (Directory dir : entrypoints) {
			boolean recursively = false;
			List<File> curFiles = dir.getAllFiles(recursively);
			for (File curFile : curFiles) {
				String filename = curFile.getLocalFilename();
				String justname = filename;
				int dotIndex = filename.lastIndexOf(".");
				if (dotIndex >= 0) {
					justname = filename.substring(0, dotIndex);
				}
				if ((filename.endsWith(".pdf") || filename.endsWith(".epub")) &&
					(!curFileNames.contains(filename))) {
					curFileNames.add(justname);
				}
			}
		}

		curFileNames = SortUtils.sort(curFileNames);

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
