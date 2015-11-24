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
import java.util.List;

/**
 * @param <T>
 */
public interface IOption<T> extends Serializable {

    /**
     * @return default value for this option
     */
    T getDefaultVal();

    /**
     * @return the name of the option
     */
    String getName();

    /**
     * @return all the possible values for this option
     */
    List<? extends T> getPossibleValues();

    /**
     * @return current value for this option
     */
    T getVal();

    /**
     * @param value
     *            the value to adopt
     */
    void setVal(T value);

}
