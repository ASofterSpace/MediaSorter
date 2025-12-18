/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.List;


public class WebLinkDatabase {

	private static final String DB_FILE_NAME = "weblinkDatabase.json";

	private Directory dataDir;

	private JsonFile dbFile;

	private JSON root;

	private List<WebLink> manuallyStoredLinks;


	public WebLinkDatabase(Directory dataDir) {

		this.dataDir = dataDir;

		dataDir.create();

		this.dbFile = new JsonFile(dataDir, DB_FILE_NAME);
		this.dbFile.createParentDirectory();
		try {
			this.root = dbFile.getAllContents();
		} catch (JsonParseException e) {
			System.err.println("Oh no!");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		this.manuallyStoredLinks = new ArrayList<>();
		List<Record> mslRecs = root.getArray("manuallyStoredLinks");
		for (Record mslRec : mslRecs) {
			manuallyStoredLinks.add(new WebLink(mslRec));
		}
	}

	public void save() {

		root.makeObject();

		List<Record> mslRecs = new ArrayList<>();
		for (WebLink webLink : manuallyStoredLinks) {
			mslRecs.add(webLink.toRecord());
		}
		root.set("manuallyStoredLinks", mslRecs);

		dbFile.setAllContents(root);
		dbFile.save();
	}
}
