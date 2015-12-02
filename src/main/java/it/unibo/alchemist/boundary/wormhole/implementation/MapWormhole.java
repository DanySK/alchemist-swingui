/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.function.BiFunction;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;

import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.model.interfaces.IEnvironment;

/**
 * Wormhole used for maps rendering.
 * 

 */
public final class MapWormhole extends Wormhole2D {
    private final MapViewPosition mapModel;
    /**
     * Maximum zoom.
     */
    public static final byte MAX_ZOOM = 18;
    private static final long MAPSFORGE_TILE_SIZE = 256;

    /**
     * Initializes a new {@link MapWormhole} copying the state of the one in
     * input.
     * 
     * @param w
     *            is the previous {@link IWormhole2D}
     * @param m
     *            is the {@link Model} object used to handle the map
     */
    public MapWormhole(final IEnvironment<?> env, final Component comp, final MapViewPosition m) {
        super(env, comp);
        mapModel = m;
        super.setMode(Mode.MAP);
        super.setViewPosition(new Point2D.Double(getViewSize().getWidth() / 2, getViewSize().getHeight() / 2));
    }

    @Override
    public Point2D getEnvPoint(final Point2D viewPoint) {
        final LatLong l = mapModel.getCenter();
        final Point2D c = new Point2D.Double(lonToPxX(l.longitude), latToPxY(l.latitude));
        final Point2D vc = getViewPosition();
        final Point2D d = NSEAlg2DHelper.subtract(viewPoint, vc);
        final Point2D p = NSEAlg2DHelper.sum(d, c);
        if (p.getX() < 0 || p.getY() < 0 || p.getX() > mapSize() || p.getY() > mapSize()) {
            /*
             * The point is OUTSIDE the map.
             */
            return new Point2D.Double(l.longitude, l.latitude);
        }
        return new Point2D.Double(pxXToLon(p.getX()), pxYToLat(p.getY()));
    }

    private double lonToPxX(final double lon) {
        return mercatorApplier(MercatorProjection::longitudeToPixelX, lon);
    }

    private double pxXToLon(final double pxx) {
        return mercatorApplier(MercatorProjection::pixelXToLongitude, pxx);
    }

    private double latToPxY(final double lat) {
        return mercatorApplier(MercatorProjection::latitudeToPixelY, lat);
    }

    private double pxYToLat(final double pxy) {
        return mercatorApplier(MercatorProjection::pixelYToLatitude, pxy);
    }

    private double mercatorApplier(final BiFunction<Double, Long, Double> fun, final double arg) {
        return fun.apply(arg, mapSize());
    }

    private long mapSize() {
        return MAPSFORGE_TILE_SIZE << mapModel.getZoomLevel();
    }

    @Override
    public Point2D getViewPoint(final Point2D envPoint) {
        final LatLong l = mapModel.getCenter();
        final Point2D p = new Point2D.Double(lonToPxX(envPoint.getX()), latToPxY(envPoint.getY()));
        final Point2D c = new Point2D.Double(lonToPxX(l.longitude), latToPxY(l.latitude));
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
    public void optimalZoom() {
        final double startLong = getEnvironmentOffset()[0];
        final double startLat = getEnvironmentOffset()[1];
        final double endLong = startLong + getEnvironmentSize()[0];
        final double endLat = startLat + getEnvironmentSize()[1];
        center();
        Point2D startScreen;
        Point2D endScreen;
        byte zoom = MAX_ZOOM;
        do {
            setZoom(zoom);
            final Point2D start = new Point2D.Double(startLong, startLat);
            final Point2D end = new Point2D.Double(endLong, endLat);
            startScreen = getViewPoint(start);
            endScreen = getViewPoint(end);
            zoom--;
        } while(zoom > 1 && !isInsideView(startScreen) && !isInsideView(endScreen));
        setZoom(zoom);
    }

    @Override
    public void setViewPosition(final Point2D p) {
        setDeltaViewPosition(NSEAlg2DHelper.subtract(p, getViewPosition()));
    }

    @Override
    public void setZoom(final double z) {
        super.setZoom(z);
        mapModel.setZoomLevel((byte) getZoom());
    }

    @Override
    public void zoomOnPoint(final Point2D p, final double z) {
        final Point2D ep = getEnvPoint(p);
        setZoom(z);
        final Point2D nvp = getViewPoint(ep);
        setDeltaViewPosition(NSEAlg2DHelper.subtract(p, nvp));
    }

}
