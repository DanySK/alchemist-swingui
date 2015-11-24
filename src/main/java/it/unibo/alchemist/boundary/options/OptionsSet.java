/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.options;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class OptionsSet implements Serializable {

    private static final long serialVersionUID = -4377259625620844578L;

    private static final IOption<?>[] OPTIONS = {  };

    private final Map<String, IOption<?>> opts = new ConcurrentHashMap<>();

    /**
     * 
     */
    public OptionsSet() {
        for (final IOption<?> o : OPTIONS) {
            opts.put(o.getName(), o);
        }
    };

    /**
     * @param name
     *            the name of the option to retrieve
     * @return the option, or null if it does not exists
     */
    public IOption<?> get(final String name) {
        return opts.get(name);
    }

    /**
     * @return returns the whole options set as list
     */
    public IOption<?>[] getSet() {
        return Arrays.copyOf(OPTIONS, OPTIONS.length);
    }

}
