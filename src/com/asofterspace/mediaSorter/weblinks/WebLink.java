/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.mediaSorter.weblinks;

import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;


public class WebLink {

	private final static String KEY_URI = "u";
	private final static String KEY_SOURCES = "s";
	private final static String KEY_REFERENCES = "r";

	private String uri;
	private List<String> sources;
	private List<String> references;


	public WebLink(String uri) {
		this.uri = uri;
		this.sources = new ArrayList<>();
		this.references = new ArrayList<>();
	}

	public WebLink(Record rec) {
		this.uri = rec.getString(KEY_URI);
		this.sources = rec.getArrayAsStringList(KEY_SOURCES);
		this.references = rec.getArrayAsStringList(KEY_REFERENCES);
	}

	public Record toRecord() {
		Record rec = Record.emptyObject();
		rec.set(KEY_URI, uri);
		rec.set(KEY_SOURCES, sources);
		rec.set(KEY_REFERENCES, references);
		return rec;
	}

	public void appendTo(StringBuilder sb) {
		sb.append("{u: \"" + escape(uri) + "\", s: [");
		for (String source : sources) {
			sb.append("\"" + escape(source) + "\",");
		}
		sb.append("], r: [");
		for (String reference : references) {
			sb.append("\"" + escape(reference) + "\",");
		}
		sb.append("]},\n");
	}

	private String escape(String str) {
		return StrUtils.replaceAll(StrUtils.replaceAll(str, "\"", "\\\""), "\\\\\"", "\\\"");
	}

	public void addSource(String newSource) {
		sources.add(newSource);
	}

	public void addReference(String newReference) {
		sources.add(newReference);
	}

	public boolean tryToIntegrate(WebLink other) {
		if (uri.equals(other.uri)) {
			sources.addAll(other.sources);
			references.addAll(other.references);
			return true;
		}
		return false;
	}

}
