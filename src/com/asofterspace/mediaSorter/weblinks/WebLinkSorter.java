/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.toolbox.coders.UrlEncoder;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;


public class WebLinkSorter {

	private final static String KEY_ASS_BROWSER_BASE_PATH = "assBrowserBasePath";
	private final static String KEY_ASS_WORKBENCH_BASE_PATH = "assWorkbenchBasePath";
	private final static String KEY_ASS_WORKBENCH_LOCAL_LOG_PATH = "assWorkbenchLocalLogPath";
	private final static String KEY_DIRS_WITH_TXT_SOURCE_FILES = "directoriesWithTextSourceFiles";
	private final static String KEY_DIRS_WITH_STPU_SOURCE_FILES = "directoriesWithStpuSourceFiles";

	private final static String INDEX_HTM = "index.htm";

	private Directory serverDir;
	private Directory outputDir;

	private WebLinkDatabase database;

	private ConfigFile config;

	private String assBrowserBasePath;
	private String assWorkbenchBasePath;
	private String assWorkbenchLocalLogPath;

	private List<String> pagesWithExtendedURIs = new ArrayList<>();


	public WebLinkSorter(WebLinkDatabase database, Directory serverDir, Directory outputDir) {
		this.database = database;
		this.serverDir = serverDir;
		this.outputDir = outputDir;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("weblinkSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON(
					"{\"" + KEY_DIRS_WITH_TXT_SOURCE_FILES + "\":[]," +
					"\"" + KEY_DIRS_WITH_STPU_SOURCE_FILES + "\\:[]}"
				));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the weblink settings failed:");
			System.err.println(e);
			System.exit(1);
		}

		assBrowserBasePath = config.getValue(KEY_ASS_BROWSER_BASE_PATH);
		assWorkbenchBasePath = config.getValue(KEY_ASS_WORKBENCH_BASE_PATH);
		assWorkbenchLocalLogPath = config.getValue(KEY_ASS_WORKBENCH_LOCAL_LOG_PATH);

		pagesWithExtendedURIs.add("youtube.");
	}

	public void run() {
		System.out.println("Starting weblink sorting...");

		List<WebLink> links = new ArrayList<>();

		// add links from manual sources (a json file where we just add sources / references directly)
		links.addAll(database.getManuallyStoredLinks());

		// add links from /data/ for video sources, podcasts etc. (search .txt files in there)
		links.addAll(getLinksFromDirectoriesWithTextSourceFiles());

		// add links from Desktop (search .stpu files in there)
		links.addAll(getLinksFromDirectoriesWithStpuSourceFiles());

		// add links from Workbench (search .json files in there)
		links.addAll(getLinksWorkbenchSourceFiles());


		// TODO run through:
		// workbench: load snailed JSON

		links = integrateLinks(links);

		// write all findings into a JavaScript file that is then loaded by a static viewer HTML file that
		// allows searching for URLs and returns the best matches
		writeLinksToOutputFile(links);

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
			System.out.println("Checking directory " + dirPath + " for web links in TXT files...");
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

	private List<WebLink> getLinksFromDirectoriesWithStpuSourceFiles() {

		List<WebLink> result = new ArrayList<>();
		List<String> dirPaths = config.getList(KEY_DIRS_WITH_STPU_SOURCE_FILES);

		for (String dirPath : dirPaths) {
			System.out.println("Checking directory " + dirPath + " for web links in STPU files...");
			Directory curDir = new Directory(dirPath);
			String endStr = ".stpu";
			boolean recursively = true;
			List<File> textFiles = curDir.getAllFilesEndingWith(endStr, recursively);
			for (File curFile : textFiles) {
				String localFileName = curFile.getLocalFilename();
				if (!"VSTPU.stpu".equals(localFileName)) {
					TextFile textFile = new TextFile(curFile);
					String content = textFile.getContent();
					String localLocation =
						assBrowserBasePath + "?path=" +
						UrlEncoder.encodePath(textFile.getParentDirectory().getDirname()) +
						"&file=" + UrlEncoder.encodePath(localFileName);
					result.addAll(getLinksFromText(content, localLocation));
				}
			}
		}

		return result;
	}

	private List<WebLink> getLinksWorkbenchSourceFiles() {

		List<WebLink> result = new ArrayList<>();

		System.out.println("Checking directory " + assWorkbenchLocalLogPath + " for web links in JSON files...");
		Directory curDir = new Directory(assWorkbenchLocalLogPath);
		String endStr = ".json";
		boolean recursively = false;
		List<File> textFiles = curDir.getAllFilesEndingWith(endStr, recursively);
		for (File curFile : textFiles) {
			String localFileName = curFile.getLocalFilename();
			JsonFile jsonFile = new JsonFile(curFile);
			try {
				JSON json = jsonFile.getAllContents();
				String content = json.getString("content");
				if (content != null) {
					Integer id = json.getInteger("id");
					String project = json.getString("project");
					String localLocation =
						assWorkbenchBasePath + "/projects/" + project + "/?open=logbook&id=" + id;
					result.addAll(getLinksFromText(content, localLocation));
				}
			} catch (JsonParseException e) {
				// ignore this file
			}
		}

		return result;
	}

	// for each:
	// check if there are links in there (starting with http:// or https://)
	// strip links of tracking info, then store
	// if link is preceded by source: above (so going lines up until an empty line is encountered),
	// then store as actual source for the link, otherwise as reference to it
	private List<WebLink> getLinksFromText(String text, String localLocation) {
		List<WebLink> result = new ArrayList<>();
		List<String> urisAlreadyAdded = new ArrayList<>();
		String emptyLineMarker = "\n\n";

		for (int sCount = 0; sCount < 2; sCount++) {
			String curLinkStartStr = "http://";
			if (sCount > 0) {
				curLinkStartStr = "https://";
			}
			int index = text.indexOf(curLinkStartStr);

			// only if we find anything of interest...
			if (index > 0) {
				// ... if the text does contain nonsense line endings ...
				if (text.contains("\r")) {
					// ... just replace them with linuxy line endings
					// (don't properly convert but replace in-line for simplicity)
					text = text.replace('\r', '\n');
					// and then each newline will be \n\n, so an empty line will be \n\n\n\n
					emptyLineMarker = "\n\n\n\n";
				}
			}

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
				// for some pages, we do NOT want to drop everything after ?:
				if (nextQuestMark >= 0) {
					String tempUri = text.substring(index, nextQuestMark);
					// ... namely, for youtube we don't want to do so:
					for (String pageWithExtendedURIs : pagesWithExtendedURIs) {
						if (tempUri.contains(pageWithExtendedURIs)) {
							nextQuestMark = text.indexOf("&", index);
						}
					}
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
				// ensure link is just added once even if is several times in the text
				if (!urisAlreadyAdded.contains(uri)) {
					urisAlreadyAdded.add(uri);
					WebLink newLink = new WebLink(uri);
					// check if in a previous line we have the text "source:" to
					// determine whether this is a source or just a reference -
					// but just up to an empty line, so that if there is a link
					// far later it is again just classified as reference
					int prevEmptyLine = text.lastIndexOf(emptyLineMarker, index);
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
				}
				index = text.indexOf(curLinkStartStr, index);
			}
		}

		return result;
	}

	private void writeLinksToOutputFile(List<WebLink> links) {

		// generate index file - just statically the one from the server dir
		TextFile indexFile = new TextFile(serverDir, INDEX_HTM);
		String content = indexFile.getContent();
		StringBuilder sbPagesWithExtendedURIs = new StringBuilder();
		sbPagesWithExtendedURIs.append("[");
		String sep = "";
		for (String pageWithExtendedURIs : pagesWithExtendedURIs) {
			sbPagesWithExtendedURIs.append(sep);
			sep = ", ";
			sbPagesWithExtendedURIs.append("\"");
			sbPagesWithExtendedURIs.append(pageWithExtendedURIs);
			sbPagesWithExtendedURIs.append("\"");
		}
		sbPagesWithExtendedURIs.append("]");
		content = StrUtils.replaceAll(content, "[[PAGES_WITH_EXT_URIS]]", sbPagesWithExtendedURIs.toString());
		TextFile outputIndexFile = new TextFile(outputDir, INDEX_HTM);
		outputIndexFile.saveContent(content);

		// generate data file
		TextFile dataFile = new TextFile(outputDir, "data.js");
		StringBuilder dataOut = new StringBuilder();
		dataOut.append("window.links = [");
		for (WebLink link : links) {
			link.appendTo(dataOut);
		}
		dataOut.append("];");
		dataFile.saveContent(dataOut.toString());
	}

}
