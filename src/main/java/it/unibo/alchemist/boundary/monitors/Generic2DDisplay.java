/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * General purpose 2D display.
 * 
 * 
 * @param <T>
 *            The type of the concentration
 */
public class Generic2DDisplay<T> extends Abstract2DDisplay<T> {

    /**
     * 
     */
    private static final long serialVersionUID = -5045238077899938609L;

    /**
     * @see Abstract2DDisplay#Abstract2DDisplay()
     */
    public Generic2DDisplay() {
        super();
    }

    /**
     * @param step
     *           {@link Abstract2DDisplay#Abstract2DDisplay(int)}
     */
    public Generic2DDisplay(final int step) {
        super(step);
    }

    @Override
    protected void drawBackground(final Graphics2D g) {
        /*
         * Unused
         */
    }

    @Override
    protected void onFirstResizing() {
        getWormhole().setViewPosition(new Point2D.Double(0, getSize().getHeight()));
        getWormhole().optimalZoom();
        getZoomManager().setZoom(getWormhole().getZoom());
    }

}
