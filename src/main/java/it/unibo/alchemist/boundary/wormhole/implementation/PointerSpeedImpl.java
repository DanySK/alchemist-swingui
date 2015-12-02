/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import it.unibo.alchemist.boundary.wormhole.interfaces.PointerSpeed;

import java.awt.geom.Point2D;

/**
 * Implementation for {@link PointerSpeed} interface.
 * 
 */
public class PointerSpeedImpl implements PointerSpeed {
    private Point2D oldPosition = new Point2D.Double();
    private Point2D position = new Point2D.Double();

    @Override
    public Point2D getCurrentPosition() {
        return (Point2D) position.clone();
    }

    @Override
    public Point2D getOldPosition() {
        return (Point2D) oldPosition.clone();
    }

    @Override
    public Point2D getVariation() {
        return new Point2D.Double(position.getX() - oldPosition.getX(), position.getY() - oldPosition.getY());
    }

    @Override
    public void setCurrentPosition(final Point2D point) {
        oldPosition = position;
        position = (Point2D) point.clone();
    }

}
