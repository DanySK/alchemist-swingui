/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui;

import it.unibo.alchemist.utils.L;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class FileExplorer implements Serializable {

    private static final long serialVersionUID = -1823016369233397745L;
    private int spcCount;
    private final List<File> fileList;

    /**
     * 
     */
    public FileExplorer() {
        spcCount = -1;
        fileList = new ArrayList<>();
    }

    /**
     * 
     * @param aFile
     *            the starting file
     * @return the list of all the files contained
     */
    public List<File> process(final File aFile) {
        spcCount++;
        if (aFile.isFile()) {
            fileList.add(aFile);
        } else if (aFile.isDirectory()) {
            final File[] listOfFiles = aFile.listFiles();
            if (listOfFiles != null) {
                for (final File listOfFile : listOfFiles) {
                    process(listOfFile);
                }
            } else {
                L.warn(aFile + " [ACCESS DENIED]");
            }
        }
        spcCount--;
        if (spcCount == -1) {
            return fileList;
        } else {
            return null;
        }
    }
}
