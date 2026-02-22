/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.series;

import com.asofterspace.mediaSorter.movies.MovieSorter;
import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.SortUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SeriesSorter {

	private static final String SERIES_HTM = "series.htm";

	private Directory serverDir;
	private Directory outputDir;

	private ConfigFile config;


	public SeriesSorter(Directory serverDir, Directory outputDir) {
		this.serverDir = serverDir;
		this.outputDir = outputDir;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("seriesSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the series settings failed:");
			System.err.println(e);
			System.exit(1);
		}
	}

	public void run() {

		runUploadFile();

		System.out.println("Series sorting done!");
	}

	private void runUploadFile() {

		System.out.println("Starting series sorting for upload...");

		// load media files
		Map<String, Series> nameToSeries = new HashMap<>();
		List<String> seriesPaths = config.getList("seriespaths");

		for (String seriesPath : seriesPaths) {
			Directory seriesEntryPointDir = new Directory(seriesPath);
			boolean recursively = false;
			List<Directory> seriesDirs = seriesEntryPointDir.getAllDirectories(recursively);
			for (Directory seriesDir : seriesDirs) {
				String name = seriesDir.getLocalDirname();
				Series cur = nameToSeries.get(name);
				if (cur == null) {
					cur = new Series(name);
					nameToSeries.put(name, cur);
				}
				cur.addDir(seriesDir);
			}
		}

		List<String> seriesNamesSorted = SortUtils.sort(nameToSeries.keySet());

		StringBuilder seriesHTML = new StringBuilder();

		for (String name : seriesNamesSorted) {
			nameToSeries.get(name).appendHTML(seriesHTML, 2);
		}

		TextFile seriesBaseFile = new TextFile(serverDir, MovieSorter.MOVIES_AND_SERIES_HTM);
		String html = seriesBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[HEADLINE]]", "Series");

		html = StrUtils.replaceAll(html, "[[SEE_ALSO_A]]", "href='movies.htm'>:: click for [movies]");

		html = StrUtils.replaceAll(html, "[[UPDATE_DATETIMESTAMP]]", DateUtils.serializeDateTime(DateUtils.now()));

		html = StrUtils.replaceAll(html, "[[CONTENT]]", seriesHTML.toString());

		TextFile seriesOutFile = new TextFile(outputDir, SERIES_HTM);
		seriesOutFile.saveContent(html);

		boolean doUpload = config.getBoolean("upload");
		if (doUpload) {
			System.out.println("Uploading series...");
			File uploadFile = new File("upload_series.sh");
			IoUtils.execute(uploadFile.getCanonicalFilename());
			System.out.println("Upload done!");
		}
	}
}
