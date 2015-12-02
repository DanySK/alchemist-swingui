/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.utils.L;

import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * <code>NSEWormhole2D</code> = No Side Effects Wormhole2D.<br>
 * Complete implementation for the {@link AbstractWormhole2D} class.
 * 

 */
public class NSEWormhole extends AbstractWormhole2D {

    /**
     * Initializes a new <code>NSEWormhole2D</code> instance directly setting
     * the size of both view and environment, and the offset too.
     * 
     * @param viewSize
     *            is the size of the view
     * @param envSize
     *            is the size of the environment
     * @param offset
     *            is the offset
     * 
     * @see IWormhole2D
     */
    public NSEWormhole(final Dimension2D viewSize, final Dimension2D envSize, final Point2D offset) {
        super(viewSize, envSize, offset);
    }

    /**
     * Calculates the {@link AffineTransform} that allows the wormhole to
     * convert points from env-space to view-space.
     * 
     * @return an {@link AffineTransform} object
     */
    protected AffineTransform calculateTransform() {
        final AffineTransform t;
        if (getMode() == Mode.ISOMETRIC) {
            t = new AffineTransform(getZoom(), 0d, 0d, -getZoom(), getViewPosition().getX(), getViewPosition().getY());
        } else {
            t = new AffineTransform(getZoom() * getHRate(), 0d, 0d, -getZoom() * getVRate(), getViewPosition().getX(), getViewPosition().getY());
        }
        t.concatenate(AffineTransform.getRotateInstance(getRotation()));
        return t;
    }

    @Override
    public Point2D getEnvPoint(final Point2D viewPoint) {
        final Point2D vp = new Point2D.Double(viewPoint.getX(), viewPoint.getY());
        final AffineTransform t = calculateTransform();
        try {
            t.inverseTransform(vp, vp);
        } catch (final NoninvertibleTransformException e) {
            L.error(e.getMessage());
        }
        return NSEAlg2DHelper.sum(vp, getEnvOffset());
    }

    @Override
    public Point2D getViewPoint(final Point2D envPoint) {
        final Point2D ep = NSEAlg2DHelper.subtract(envPoint, getEnvOffset());
        final AffineTransform t = calculateTransform();
        t.transform(ep, ep);
        return ep;
    }

    @Override
    public void rotateAroundPoint(final Point2D p, final double a) {
        setViewPositionWithoutMoving(p);
        setRotation(a);
        setEnvPositionWithoutMoving(getOriginalOffset());
    }

    @Override
    public void zoomOnPoint(final Point2D p, final double z) {
        setViewPositionWithoutMoving(p);
        setZoom(z);
        setEnvPositionWithoutMoving(getOriginalOffset());
    }

}
