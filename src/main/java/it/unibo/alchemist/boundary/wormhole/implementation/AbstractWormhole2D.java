/*
 * Copyright (C) 2010-2015, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;

import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * Partial implementation for the interface {@link IWormhole2D}.<br>
 * I am considering the particular case of the view as an entity into the
 * sceern-space: the y-axis grows on the bottom side of the screen.
 * 
 */
public abstract class AbstractWormhole2D implements IWormhole2D {
    private Dimension2D viewSize;
    private Dimension2D envSize;
    private Point2D position;
    private Point2D offset = new Point2D.Double(0d, 0d);
    private final Point2D originalOffset;
    private double zoom = 1d;
    private double angle;
    private double hRate = 1d;
    private double vRate = 1d;
    private Mode mode = Mode.ISOMETRIC;

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
    public AbstractWormhole2D(final Dimension2D vSize, final Dimension2D eSize, final Point2D o) {
        Objects.requireNonNull(vSize);
        Objects.requireNonNull(eSize);
        Objects.requireNonNull(o);
        viewSize = new DoubleDimension(vSize.getWidth(), vSize.getHeight());
        envSize = new DoubleDimension(eSize.getWidth(), eSize.getHeight());
        position = new Point2D.Double(0d, vSize.getHeight());
        offset = new Point2D.Double(o.getX(), o.getY());
        originalOffset = new Point2D.Double(o.getX(), o.getY());
    }

    @Override
    public Point2D getEnvOffset() {
        return offset;
    }

    private double getEnvRatio() {
        return envSize.getWidth() / envSize.getHeight();
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
            return viewSize.getWidth() / envSize.getWidth();
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
            return viewSize.getHeight() / envSize.getHeight();
        } else {
            return vRate;
        }
    }

    /**
     * Gets the original offset.
     * 
     * @return a {@link Point2D}
     */
    protected Point2D getOriginalOffset() {
        return originalOffset;
    }

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
        return (Dimension2D) viewSize.clone();
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
    public void setEnvOffset(final Point2D point) {
        offset = new Point2D.Double(point.getX(), point.getY());
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
    protected void setEnvPositionWithoutMoving(final Point2D envPoint) {
        setViewPositionWithoutMoving(getViewPoint(new Point2D.Double(envPoint.getX(), envPoint.getY())));
    }

    @Override
    public void setEnvSize(final Dimension2D size) {
        envSize = new DoubleDimension(size);
    }

    @Override
    public void setHRate(final double value) {
        if (mode == Mode.ISOMETRIC) {
            hRate = 1d;
        } else if (mode == Mode.ADAPT_TO_VIEW) {
            hRate = getNIHorizontalRatio();
        } else {
            hRate = value;
        }
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
            zoom = viewSize.getHeight() / envSize.getHeight();
        } else {
            zoom = viewSize.getWidth() / envSize.getWidth();
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
    protected void setViewPositionWithoutMoving(final Point2D viewPoint) {
        final Point2D envDelta = NSEAlg2DHelper.variation(getEnvPoint(new Point2D.Double(viewPoint.getX(), viewPoint.getY())), getEnvPoint(position));
        position = new Point2D.Double(viewPoint.getX(), viewPoint.getY());
        offset = NSEAlg2DHelper.sum(offset, envDelta);
    }

    @Override
    public void setViewSize(final Dimension2D size) {
        viewSize = new DoubleDimension(size.getWidth(), size.getHeight());
    }

    @Override
    public void setVRate(final double value) {
        if (mode == Mode.ISOMETRIC) {
            vRate = 1d;
        } else if (mode == Mode.ADAPT_TO_VIEW) {
            vRate = getNIVerticalRatio();
        } else {
            vRate = value;
        }
    }

    @Override
    public void setZoom(final double value) {
        if (value < 0d) {
            zoom = 0d;
        }
        zoom = value;
    }
}
