/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.logging;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

public class JOutputStream extends OutputStream {
    private final JTextArea file;

    public JOutputStream(final JTextArea t) {
        super();
        file = t;
    }

    @Override
    public void write(final byte[] data) throws IOException {
        file.append(new String(data));
        file.setCaretPosition(file.getDocument().getLength());
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        file.append(new String(b, off, len));
        file.setCaretPosition(file.getDocument().getLength());
    }

    @Override
    public void write(final int c) throws IOException {
        file.append(String.valueOf((char) c));
        file.setCaretPosition(file.getDocument().getLength());
    }

}
