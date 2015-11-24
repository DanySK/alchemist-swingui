/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.logging;

import it.unibo.alchemist.boundary.logging.JPrintStream;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class JHandler extends Handler {
    private static final String PATTERN = "[logger] level:\nmessage\n";
    private final JPrintStream jps;

    public JHandler(final JPrintStream s) {
        super();
        jps = s;
    }

    @Override
    public void close() throws SecurityException {
        // TODO Auto-generated method stub

    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public void publish(final LogRecord r) {
        final String p1 = PATTERN.replace("logger", r.getLoggerName());
        final String p2 = p1.replace("level", r.getLevel().toString());
        final String line = p2.replace("message", r.getMessage());
        jps.println(line);
    }

}
