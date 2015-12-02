/*
 * Copyright (C) 2010-2015, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.utils.L;

/**
 * Partial implementation for the interface {@link IWormhole2D}.<br>
 * I am considering the particular case of the view as an entity into the
 * sceern-space: the y-axis grows on the bottom side of the screen.
 * 
 */
public class Wormhole2D implements IWormhole2D {
//    private Dimension2D viewSize;
//    private final Dimension2D envSize;
    private Point2D position;
    private Point2D effectCenter = new Point2D.Double(0, 0);
//    private Point2D offset = new Point2D.Double(0d, 0d);
//    private final Point2D originalOffset;
    private double zoom = 1d;
    private double angle;
    private double hRate = 1d;
    private double vRate = 1d;
    private Mode mode = Mode.ISOMETRIC;
    
    private final IEnvironment<?> model;
    private final Component view;

    /**
     * Initializes a new instance directly setting the size of both view and
     * environment, and the offset too.
     * 
     * @param vSize
     *            is the size of the view
     * @param eSize
     *            is the size of the environment
     * @param o
     *            is the offset
     * 
     * @see IWormhole2D
     */
    public Wormhole2D(final IEnvironment<?> env, final Component comp) {
        model = env;
        view = comp;
//        Objects.requireNonNull(vSize);
//        Objects.requireNonNull(eSize);
//        Objects.requireNonNull(o);
//        viewSize = new DoubleDimension(vSize.getWidth(), vSize.getHeight());
//        envSize = new DoubleDimension(eSize.getWidth(), eSize.getHeight());
        position = new Point2D.Double(0d, comp.getHeight());
//        offset = new Point2D.Double(o.getX(), o.getY());
//        originalOffset = new Point2D.Double(o.getX(), o.getY());
    }

    private double getEnvRatio() {
        final double[] size = model.getSize();
        return size[0] / size[1];
    }

    /**
     * @return the horizontal stretch rate
     */
    protected double getHRate() {
        return hRate;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    /**
     * Gets the viewWidth / envWidth ratio.<br>
     * NI = Not Isometric.
     * 
     * @return a <code>double</code> value representing the horizontal ratio for
     *         Not Isometric mode
     */
    private double getNIHorizontalRatio() {
        if (mode == Mode.ISOMETRIC) {
            return 1d;
        } else if (mode == Mode.ADAPT_TO_VIEW) {
            return view.getWidth() / model.getSize()[0];
        } else {
            return hRate;
        }
    }

    /**
     * Gets the viewHeight / envHeight ratio.<br>
     * NI = Not Isometric.
     * 
     * @return a <code>double</code> value representing the vertical ratio for
     *         Not Isometric mode
     */
    private double getNIVerticalRatio() {
        if (mode == Mode.ISOMETRIC) {
            return 1d;
        } else if (mode == Mode.ADAPT_TO_VIEW) {
            return view.getHeight() / model.getSize()[1];
        } else {
            return vRate;
        }
    }

    /**
     * Gets the original offset.
     * 
     * @return a {@link Point2D}
     */
//    protected Point2D getOriginalOffset() {
//        return originalOffset;
//    }

    /**
     * Gets the rotation angle, in radians.
     * 
     * @return a <code>double</code> value representing an angle expressed with
     *         radians
     * @see #setRotation(double)
     */
    protected double getRotation() {
        return angle;
    }

    @Override
    public Point2D getViewPosition() {
        return (Point2D) position.clone();
    }

    @Override
    public Dimension2D getViewSize() {
        return view.getSize();
    }

    /**
     * Gets the vertical stretch rate.
     * 
     * @return a <code>double</code> value representing the vertical stretch
     *         rate
     */
    protected double getVRate() {
        return vRate;
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    @Override
    public boolean isInsideView(final Point2D viewPoint) {
        final double x = viewPoint.getX();
        final double y = viewPoint.getY();
        final Dimension2D vs = getViewSize();
        return x >= 0 && x <= vs.getWidth() && y >= 0 && y <= vs.getHeight();
    }

    @Override
    public void setDeltaViewPosition(final Point2D delta) {
        position = NSEAlg2DHelper.sum(position, delta);
    }

    @Override
    public void setEnvPosition(final Point2D pos) {
        setViewPosition(getViewPoint(new Point2D.Double(pos.getX(), pos.getY())));
    }

    /**
     * Changes the point referred ad 'position'.
     * 
     * @param envPoint
     *            is a {@link Point2D} into the env-space
     */
    private void setEnvPositionWithoutMoving(final Point2D envPoint) {
        setViewPositionWithoutMoving(getViewPoint(new Point2D.Double(envPoint.getX(), envPoint.getY())));
    }

    /**
     * Allows child-classes to modify the {@link #mode} field.
     * 
     * @param m
     *            is the new {@link Mode}
     */
    protected void setMode(final Mode m) {
        mode = m;
        if (m == Mode.ADAPT_TO_VIEW) {
            vRate = getNIVerticalRatio();
            hRate = getNIHorizontalRatio();
        }
    }

    @Override
    public void optimalZoom() {
        if (getEnvRatio() <= 1) {
            zoom = view.getHeight() / model.getSize()[1];
        } else {
            zoom = view.getWidth() / model.getSize()[0];
        }

    }

    @Override
    public void setRotation(final double rad) {
        angle = rad % NSEAlg2DHelper.PI2;
    }

    @Override
    public void setViewPosition(final Point2D point) {
        position = new Point2D.Double(point.getX(), point.getY());
    }

    /**
     * Changes the point referred ad 'position'.
     * 
     * @param viewPoint
     *            is a {@link Point2D} into the view-space
     */
    private void setViewPositionWithoutMoving(final Point2D viewPoint) {
        final Point2D envDelta = NSEAlg2DHelper.variation(getEnvPoint(new Point2D.Double(viewPoint.getX(), viewPoint.getY())), getEnvPoint(position));
        position = new Point2D.Double(viewPoint.getX(), viewPoint.getY());
        effectCenter = NSEAlg2DHelper.sum(effectCenter, envDelta);
    }

//    @Override
//    public void setViewSize(final Dimension2D size) {
//        viewSize = new DoubleDimension(size.getWidth(), size.getHeight());
//    }

    @Override
    public void rotateAroundPoint(final Point2D p, final double a) {
        final Point2D orig = effectCenter;
        setViewPositionWithoutMoving(p);
        setRotation(a);
        setEnvPositionWithoutMoving(orig);
    }

    @Override
    public void zoomOnPoint(final Point2D p, final double z) {
        final Point2D orig = effectCenter;
        setViewPositionWithoutMoving(p);
        setZoom(z);
        setEnvPositionWithoutMoving(orig);
    }

    @Override
    public void setZoom(final double value) {
        if (value < 0d) {
            zoom = 0d;
        }
        zoom = value;
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
        return NSEAlg2DHelper.sum(vp, getEffectApplicationCenter());
    }

    @Override
    public Point2D getViewPoint(final Point2D envPoint) {
        final Point2D ep = NSEAlg2DHelper.subtract(envPoint, getEffectApplicationCenter());
        final AffineTransform t = calculateTransform();
        t.transform(ep, ep);
        return ep;
    }

    private Point2D getEffectApplicationCenter() {
        return effectCenter;
    }

}