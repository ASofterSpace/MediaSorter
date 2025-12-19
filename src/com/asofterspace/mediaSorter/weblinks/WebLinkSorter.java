/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.TextFile;

import java.util.ArrayList;
import java.util.List;


public class WebLinkSorter {

	private final static String KEY_DIRS_WITH_TXT_SOURCE_FILES = "directoriesWithTextSourceFiles";

	private WebLinkDatabase database;

	private ConfigFile config;


	public WebLinkSorter(WebLinkDatabase database) {
		this.database = database;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("weblinkSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"" + KEY_DIRS_WITH_TXT_SOURCE_FILES + "\":[]}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the weblink settings failed:");
			System.err.println(e);
			System.exit(1);
		}
	}

	public void run() {
		System.out.println("Starting weblink sorting...");

		List<WebLink> links = new ArrayList<>();

		// add links from manual sources (a json file where we just add sources / references directly)
		links.addAll(database.getManuallyStoredLinks());

		// add links from /data/ for video sources, podcasts etc. (search .txt files in there)
		links.addAll(getLinksFromDirectoriesWithTextSourceFiles());


		// TODO

		// run through:
		// /Desktop/ and open all .stpu files
		// workbench: load JSON of entries directly
		// workbench: load snailed JSON

		// for each:
		// check if there are links in there (starting with http:// or https://)
		// strip links of tracking info, then store
		// if link is preceded by source: above (so going lines up until an empty line is encountered),
		// then store as main source, otherwise as mentioned

		links = integrateLinks(links);

		// write all findings into a JavaScript file that is then loaded by a static viewer HTML file that
		// allows searching for URLs and returns the best matches

		Directory outputDir = new Directory("output");
		TextFile dataFile = new TextFile(outputDir, "data.js");
		StringBuilder dataOut = new StringBuilder();
		dataOut.append("links = [");
		for (WebLink link : links) {
			link.appendTo(dataOut);
		}
		dataOut.append("];");
		dataFile.saveContent(dataOut.toString());

		System.out.println("Weblink sorting done!");
	}

	private List<WebLink> integrateLinks(List<WebLink> links) {
		List<WebLink> result = new ArrayList<>();
		for (WebLink newLink : links) {
			boolean found = false;
			for (WebLink oldLink : result) {
				if (oldLink.tryToIntegrate(newLink)) {
					found = true;
					break;
				}
			}
			if (!found) {
				result.add(newLink);
			}
		}
		return result;
	}

	private List<WebLink> getLinksFromDirectoriesWithTextSourceFiles() {

		List<WebLink> result = new ArrayList<>();
		List<String> dirPaths = config.getList(KEY_DIRS_WITH_TXT_SOURCE_FILES);

		for (String dirPath : dirPaths) {
			System.out.println("Checking directory " + dirPath + " for web links...");
			Directory curDir = new Directory(dirPath);
			String endStr = ".txt";
			boolean recursively = true;
			List<File> textFiles = curDir.getAllFilesEndingWith(endStr, recursively);
			for (File curFile : textFiles) {
				TextFile textFile = new TextFile(curFile);
				String content = textFile.getContent();
				result.addAll(getLinksFromText(content, textFile.getFilename()));
			}
		}

		return result;
	}

	private List<WebLink> getLinksFromText(String text, String localLocation) {
		List<WebLink> result = new ArrayList<>();

		for (int sCount = 0; sCount < 2; sCount++) {
			String curLinkStartStr = "http://";
			if (sCount > 0) {
				curLinkStartStr = "https://";
			}
			int index = text.indexOf(curLinkStartStr);
			while (index > 0) {
				index += curLinkStartStr.length();
				int nextSpace = text.indexOf(" ", index);
				int nextTab = text.indexOf("\t", index);
				int nextNewLine = text.indexOf("\n", index);
				int nextQuestMark = text.indexOf("?", index);
				if (nextSpace < 0) {
					nextSpace = text.length();
				}
				if (nextTab < 0) {
					nextTab = text.length();
				}
				if (nextNewLine < 0) {
					nextNewLine = text.length();
				}
				if (nextQuestMark < 0) {
					nextQuestMark = text.length();
				}
				int endOfLink = nextSpace;
				if (nextTab < endOfLink) {
					endOfLink = nextTab;
				}
				if (nextNewLine < endOfLink) {
					endOfLink = nextNewLine;
				}
				if (nextQuestMark < endOfLink) {
					endOfLink = nextQuestMark;
				}
				String uri = text.substring(index, endOfLink).trim();
				WebLink newLink = new WebLink(uri);
				// check if in a previous line we have the text "source:" to
				// determine whether this is a source or just a reference -
				// but just up to an empty line, so that if there is a link
				// far later it is again just classified as reference
				int prevEmptyLine = text.lastIndexOf("\n\n", index);
				if (prevEmptyLine < 0) {
					prevEmptyLine = 0;
				}
				int sourceIndex = text.indexOf("source:", prevEmptyLine);
				if ((sourceIndex >= 0) && (sourceIndex < index)) {
					newLink.addSource(localLocation);
				} else {
					newLink.addReference(localLocation);
				}
				result.add(newLink);
				index = text.indexOf(curLinkStartStr, index);
			}
		}

		return result;
	}

}
