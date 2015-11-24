/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.monitors;

import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.boundary.monitors.ExportInspector;
import it.unibo.alchemist.utils.L;

import java.lang.reflect.Modifier;
import java.util.Set;

import javax.swing.JComboBox;

import org.reflections.Reflections;

public class OutputMonitorSelector<T> extends JComboBox<Class<? extends OutputMonitor<T>>> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final Reflections REFLECTIONS = new Reflections("it.unibo.alchemist");
    @SuppressWarnings("rawtypes")
    private static final Set<Class<? extends OutputMonitor>> MONITORS = REFLECTIONS.getSubTypesOf(OutputMonitor.class);

    public OutputMonitorSelector() {
        super();
        update();
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public final void update() {
        for (@SuppressWarnings("rawtypes") final Class<? extends OutputMonitor> c : MONITORS) {
            if (!GraphicalOutputMonitor.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()) && c.isAnnotationPresent(ExportInspector.class)) {
                try {
                    c.getConstructor();
                    addItem((Class<? extends OutputMonitor<T>>) c);
                } catch (NoSuchMethodException e) {
                    L.log(c + " cannot be added to the GUI: it has no default constructor.");
                }
            }
        }
    }
}