/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import it.unibo.alchemist.boundary.wormhole.interfaces.IZoomManager;

/**
 * An <code>LinZoomManager</code> converts the sliding of any physical/virtual
 * device/control into a zoom rate through a linear function.<br>
 * Zoom = amount of slides * rate.
 * 

 */
public class LinZoomManager extends AbstractSlideInputManager implements IZoomManager {
    private final double rate;

    /**
     * Same of {@link #LinZoomManager(double, double)} but rate is 1.
     * 
     * @param z
     *            is the desired initial zoom
     */
    public LinZoomManager(final double z) {
        this(z, 1d);
    }

    /**
     * Initialize a new <code>LinZoomManager</code> instance with the parameters
     * in input.
     * 
     * @param z
     *            is the desired initial zoom
     * @param r
     *            is the linear factor
     */
    public LinZoomManager(final double z, final double r) {
        super(z / r);
        if (r == 0 || r < Double.MIN_NORMAL) {
            throw new IllegalStateException();
        }
        rate = r;
    }

    @Override
    public double getZoom() {
        return rate * getValue();
    }

    @Override
    public void setZoom(final double z) {
        setValue(z / rate);
    }
}
