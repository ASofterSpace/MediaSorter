/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.audiobooks;

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


public class AudiobookSorter {

	private static final String AUDIOBOOKS_HTM = "audiobooks.htm";

	private Directory serverDir;
	private Directory outputDir;

	private ConfigFile config;


	public AudiobookSorter(Directory serverDir, Directory outputDir) {
		this.serverDir = serverDir;
		this.outputDir = outputDir;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("audiobookSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the audiobook settings failed:");
			System.err.println(e);
			System.exit(1);
		}
	}

	public void run() {

		runUploadFile();

		System.out.println("Audiobook sorting done!");
	}

	private void runUploadFile() {

		System.out.println("Starting audiobook sorting for upload...");

		// load media files
		Map<String, Audiobook> nameToAudiobook = new HashMap<>();
		List<String> audiobookPaths = config.getList("audiobookpaths");

		for (String audiobookPath : audiobookPaths) {
			Directory audiobookEntryPointDir = new Directory(audiobookPath);
			boolean recursively = false;
			List<Directory> audiobookDirs = audiobookEntryPointDir.getAllDirectories(recursively);
			for (Directory audiobookDir : audiobookDirs) {
				String name = audiobookDir.getLocalDirname();
				Audiobook cur = nameToAudiobook.get(name);
				if (cur == null) {
					cur = new Audiobook(name);
					nameToAudiobook.put(name, cur);
				}
				cur.addDir(audiobookDir);
			}
		}

		List<String> audiobookNamesSorted = SortUtils.sort(nameToAudiobook.keySet());

		StringBuilder audiobookHTML = new StringBuilder();

		for (String name : audiobookNamesSorted) {
			nameToAudiobook.get(name).appendAsHtmlToUploadPage(audiobookHTML, 2);
		}

		TextFile audiobookBaseFile = new TextFile(serverDir, MovieSorter.MOVIES_AND_SERIES_HTM);
		String html = audiobookBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[HEADLINE]]", "Audiobooks");

		html = StrUtils.replaceAll(html, "[[SEE_ALSO_A]]", MovieSorter.getSeeAlsoHTML(true, true, false));

		html = StrUtils.replaceAll(html, "[[UPDATE_DATETIMESTAMP]]", DateUtils.serializeDateTime(DateUtils.now()));

		html = StrUtils.replaceAll(html, "[[CONTENT]]", audiobookHTML.toString());

		TextFile audiobookOutFile = new TextFile(outputDir, AUDIOBOOKS_HTM);
		audiobookOutFile.saveContent(html);

		boolean doUpload = config.getBoolean("upload");
		if (doUpload) {
			System.out.println("Uploading audiobooks...");
			File uploadFile = new File("upload_audiobooks.sh");
			IoUtils.execute(uploadFile.getCanonicalFilename());
			System.out.println("Upload done!");
		}
	}
}
