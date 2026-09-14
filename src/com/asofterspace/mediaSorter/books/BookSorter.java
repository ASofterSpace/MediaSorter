/**
 * Unlicensed code created by A Softer Space, 2026
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.books;

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


public class BookSorter {

	private static final String BOOKS_HTM = "books.htm";

	private Directory serverDir;
	private Directory outputDir;

	private ConfigFile config;


	public BookSorter(Directory serverDir, Directory outputDir) {
		this.serverDir = serverDir;
		this.outputDir = outputDir;
		this.config = null;

		try {
			// load config
			config = new ConfigFile("bookSettings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON("{\"\"}"));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the book settings failed:");
			System.err.println(e);
			System.exit(1);
		}
	}

	public void run() {

		runUploadFile();

		System.out.println("Book sorting done!");
	}

	private void runUploadFile() {

		System.out.println("Starting book sorting for upload...");

		// load media files
		Map<String, Book> nameToBook = new HashMap<>();
		List<String> bookPaths = config.getList("bookpaths");

		for (String bookPath : bookPaths) {
			Directory bookEntryPointDir = new Directory(bookPath);
			boolean recursively = false;
			List<Directory> bookDirs = bookEntryPointDir.getAllDirectories(recursively);
			for (Directory bookDir : bookDirs) {
				String name = bookDir.getLocalDirname();
				File skipFile = new File(bookDir, MovieSorter.SKIP_FILE_NAME);
				if (skipFile.exists()) {
					continue;
				}
				Book cur = nameToBook.get(name);
				if (cur == null) {
					cur = new Book(name);
					nameToBook.put(name, cur);
				}
				cur.addDir(bookDir);
			}
		}

		List<String> bookNamesSorted = SortUtils.sort(nameToBook.keySet());

		StringBuilder bookHTML = new StringBuilder();

		for (String name : bookNamesSorted) {
			nameToBook.get(name).appendAsHtmlToUploadPage(bookHTML, 2);
		}

		TextFile bookBaseFile = new TextFile(serverDir, MovieSorter.MOVIES_AND_SERIES_HTM);
		String html = bookBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[HEADLINE]]", "Books");

		html = StrUtils.replaceAll(html, "[[SEE_ALSO_A]]", MovieSorter.getSeeAlsoHTML(true, true, true, false, true));

		html = StrUtils.replaceAll(html, "[[UPDATE_DATETIMESTAMP]]", DateUtils.serializeDateTime(DateUtils.now()));

		html = StrUtils.replaceAll(html, "[[CONTENT]]", bookHTML.toString());

		TextFile bookOutFile = new TextFile(outputDir, BOOKS_HTM);
		bookOutFile.saveContent(html);

		boolean doUpload = config.getBoolean("upload");
		if (doUpload) {
			System.out.println("Uploading books...");
			File uploadFile = new File("upload_books.sh");
			IoUtils.execute(uploadFile.getCanonicalFilename());
			System.out.println("Upload done!");
		}
	}
}
