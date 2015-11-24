/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.options;

/**
 * @param <T>
 */
public abstract class GenericOption<T> implements IOption<T> {

    private static final long serialVersionUID = -2368088259183945173L;
    private T val;

    /**
     * @param defaultsTo
     *            the default value for this option
     */
    public GenericOption(final T defaultsTo) {
        val = defaultsTo;
    }

    @Override
    public T getVal() {
        return val;
    }

    @Override
    public void setVal(final T value) {
        val = value;
    }

    @Override
    public String toString() {
        return getName() + ": " + val.toString();
    }

}
