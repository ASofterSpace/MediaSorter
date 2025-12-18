/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class WebLink {

	private final static String KEY_URI = "u";
	private final static String KEY_SOURCES = "s";
	private final static String KEY_REFERENCES = "r";

	private String uri;
	private List<String> sources;
	private List<String> references;


	public WebLink(Record rec) {
		uri = rec.getString(KEY_URI);
		sources = rec.getArrayAsStringList(KEY_SOURCES);
		references = rec.getArrayAsStringList(KEY_REFERENCES);
	}

	public Record toRecord() {
		Record rec = Record.emptyObject();
		rec.set(KEY_URI, uri);
		rec.set(KEY_SOURCES, sources);
		rec.set(KEY_REFERENCES, references);
		return rec;
	}

}
