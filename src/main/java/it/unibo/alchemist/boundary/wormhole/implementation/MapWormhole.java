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
import it.unibo.alchemist.utils.MathUtils;

import java.awt.geom.Point2D;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;

/**
 * Wormhole used for maps rendering.
 * 

 */
public class MapWormhole extends AbstractNSEWormhole2D {
    private final MapViewPosition mapModel;

    /**
     * Initializes a new {@link MapWormhole} copying the state of the one in
     * input.
     * 
     * @param w
     *            is the previous {@link IWormhole2D}
     * @param m
     *            is the {@link Model} object used to handle the map
     */
    public MapWormhole(final IWormhole2D w, final MapViewPosition m) {
        super(w.getViewSize(), w.getEnvSize(), w.getEnvOffset());

        mapModel = m;
        super.setMode(Mode.MAP);
        super.setViewPosition(new Point2D.Double(getViewSize().getWidth() / 2, getViewSize().getHeight() / 2));
    }

    @Override
    public Point2D getEnvPoint(final Point2D viewPoint) {
        final LatLong l = mapModel.getCenter();
        final Point2D c = new Point2D.Double(
                MercatorProjection.longitudeToPixelX(l.longitude, mapModel.getZoomLevel()),
                MercatorProjection.latitudeToPixelY(l.latitude, mapModel.getZoomLevel()));
        final Point2D vc = getViewPosition();
        final Point2D d = NSEAlg2DHelper.subtract(viewPoint, vc);
        final Point2D p = NSEAlg2DHelper.sum(d, c);
        return new Point2D.Double(
                MercatorProjection.pixelXToLongitude(p.getX(), mapModel.getZoomLevel()),
                MercatorProjection.pixelYToLatitude(p.getY(), mapModel.getZoomLevel()));
    }

    @Override
    public Point2D getEnvPosition() {
        final LatLong c = mapModel.getCenter();
        return new Point2D.Double(c.longitude, c.latitude);
    }

    @Override
    public Point2D getViewPoint(final Point2D envPoint) {
        final LatLong l = mapModel.getCenter();
        final Point2D p = new Point2D.Double(
                MercatorProjection.longitudeToPixelX(envPoint.getX(), mapModel.getZoomLevel()),
                MercatorProjection.latitudeToPixelY(envPoint.getY(), mapModel.getZoomLevel()));
        final Point2D c = new Point2D.Double(
                MercatorProjection.longitudeToPixelX(l.longitude, mapModel.getZoomLevel()),
                MercatorProjection.latitudeToPixelY(l.latitude, mapModel.getZoomLevel()));
        final Point2D d = NSEAlg2DHelper.subtract(p, c);
        final Point2D vc = getViewPosition();
        return new Point2D.Double(vc.getX() + d.getX(), vc.getY() + d.getY());
    }

    @Override
    public Point2D getViewPosition() {
        return new Point2D.Double(getViewSize().getWidth() / 2, getViewSize().getHeight() / 2);
    }

    @Override
    public void rotateAroundPoint(final Point2D p, final double a) {
        throw new IllegalStateException();
    }

    @Override
    public void setDeltaViewPosition(final Point2D delta) {
        mapModel.moveCenter(delta.getX(), delta.getY());
    }

    @Override
    public void setEnvPosition(final Point2D ep) {
        LatLong center;
        try {
            center = new LatLong(ep.getY(), ep.getX());
        } catch (IllegalArgumentException e) {
            center = new LatLong(0, 0);
        }
        mapModel.setCenter(center);
    }

    @Override
    public void setOptimalZoomRate() {
        final Point2D e = (Point2D) getOriginalOffset().clone();
        Point2D v;
        if (getZoom() > 1) {
            setZoom(1);
        }
        for (v = getViewPoint(e); isInsideView(v) && getZoom() < 14; v = getViewPoint(e)) {
            setZoom(getZoom() + 1);
        }
        setZoom(getZoom() - 1);
    }

    @Override
    public void setViewPosition(final Point2D p) {
        setDeltaViewPosition(NSEAlg2DHelper.subtract(p, getViewPosition()));
    }

    @Override
    public void setZoom(final double z) {
        final double zoom = MathUtils.forceRange(z, 0, Byte.MAX_VALUE);
        super.setZoom(zoom);
        mapModel.setZoomLevel((byte) zoom);
    }

    @Override
    public void zoomOnPoint(final Point2D p, final double z) {
        final Point2D ep = getEnvPoint(p);
        setZoom(z);
        final Point2D nvp = getViewPoint(ep);
        setDeltaViewPosition(NSEAlg2DHelper.subtract(p, nvp));
    }

}
