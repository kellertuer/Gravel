package io;

import java.io.File;

public interface TeXWriter {

	public abstract String saveToFile(File f);

	public abstract boolean isWholeDoc();

	public abstract void setWholedoc(boolean wholedoc);

}