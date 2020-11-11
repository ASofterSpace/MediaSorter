/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.Record;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Database {

	private static final String DB_FILE_NAME = "database.json";

	private Directory dataDir;

	private JsonFile dbFile;

	private JSON root;

	private Map<String, Date> filmDates;


	public Database(Directory dataDir) {

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

		this.filmDates = new HashMap<>();
		Map<String, Object> filmDateObjs = root.getObjectMap("filmDates");
		for (Map.Entry<String, Object> entry : filmDateObjs.entrySet()) {
			Object val = entry.getValue();
			if (val instanceof Date) {
				filmDates.put(entry.getKey(), (Date) val);
			}
		}
	}

	public Record getRoot() {
		return root;
	}

	public Date getFilmDate(String title) {
		if (filmDates == null) {
			return null;
		}
		return filmDates.get(title);
	}

	public void storeFilmDate(String title, Date additionDate) {
		if (filmDates == null) {
			filmDates = new HashMap<>();
		}
		filmDates.put(title, additionDate);
	}

	public void save() {

		root.makeObject();

		root.set("filmDates", filmDates);

		dbFile.setAllContents(root);
		dbFile.save();
	}
}
