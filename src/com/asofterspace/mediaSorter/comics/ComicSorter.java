/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.comics;

import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ComicSorter {

	private static final String COMICS_HTM = "comics.htm";

	private Directory serverDir;
	private Directory outputDir;

	private ConfigFile config;


	public ComicSorter(Directory serverDir, Directory outputDir) {
		this.serverDir = serverDir;
		this.outputDir = outputDir;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("comicSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the comic settings failed:");
			System.err.println(e);
			System.exit(1);
		}
	}

	public void run() {

		runUploadFile();

		System.out.println("Comic sorting done!");
	}

	private void runUploadFile() {

		System.out.println("Starting comic sorting for upload...");

		// load media files
		Map<String, Comic> nameToComic = new HashMap<>();
		List<String> subDirNames = new ArrayList<>();
		List<String> curFileNames = new ArrayList<>();
		List<String> comicPaths = config.getList("comicpaths");

		for (String comicPath : comicPaths) {
			Directory dir = new Directory(comicPath);
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
						Comic curSubComic = nameToComic.get(curName);
						if (curSubComic == null) {
							curSubComic = new Comic(curName);
							nameToComic.put(curName, curSubComic);
							subDirNames.add(curName);
						}
						curSubComic.addDir(curDir);
					} else {
						curFileNames.add(curName);
					}
				}
			}
		}

		StringBuilder comicHTML = new StringBuilder();

		int depth = 2;
		for (String subDirName : subDirNames) {
			nameToComic.get(subDirName).appendAsHtmlToUploadPage(comicHTML, depth);
		}

		String indentStr = " style='padding-left:" + (15*(depth-2)) + "pt;";
		for (String curFileName : curFileNames) {
			comicHTML.append("<div" + indentStr + "'>");
			comicHTML.append(HTML.escapeHTMLstr(curFileName));
			comicHTML.append("</div>");
		}

		TextFile comicBaseFile = new TextFile(serverDir, MovieSorter.MOVIES_AND_SERIES_HTM);
		String html = comicBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[HEADLINE]]", "Comics");

		html = StrUtils.replaceAll(html, "[[SEE_ALSO_A]]", MovieSorter.getSeeAlsoHTML(true, true, true, true, false));

		html = StrUtils.replaceAll(html, "[[UPDATE_DATETIMESTAMP]]", DateUtils.serializeDateTime(DateUtils.now()));

		html = StrUtils.replaceAll(html, "[[CONTENT]]", comicHTML.toString());

		TextFile comicOutFile = new TextFile(outputDir, COMICS_HTM);
		comicOutFile.saveContent(html);

		boolean doUpload = config.getBoolean("upload");
		if (doUpload) {
			System.out.println("Uploading comics...");
			File uploadFile = new File("upload_comics.sh");
			IoUtils.execute(uploadFile.getCanonicalFilename());
			System.out.println("Upload done!");
		}
	}
}
