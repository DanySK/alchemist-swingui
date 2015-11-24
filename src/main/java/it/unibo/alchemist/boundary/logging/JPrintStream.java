/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.logging;

import java.io.PrintStream;

import javax.swing.JTextArea;

public class JPrintStream extends PrintStream {

    public JPrintStream(final JTextArea jta) {
        super(new JOutputStream(jta));
    }

}
