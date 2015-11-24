/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import it.unibo.alchemist.boundary.wormhole.implementation.LinZoomManager;
import it.unibo.alchemist.boundary.wormhole.implementation.MapWormhole;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMapEnvironment;
import it.unibo.alchemist.model.interfaces.ITime;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.File;

import org.mapsforge.map.model.Model;
import org.mapsforge.map.swing.MapViewer;
import org.mapsforge.map.swing.view.MapView;

/**
 * 
 * @param <T>
 */
public class MapDisplay<T> extends Abstract2DDisplay<T> {
    private static final String MAPS_FORGE_EXTENSION = ".map";
    private static final long serialVersionUID = 8593507198560560646L;
    private MapView mapView;

    private class MapDisplayView extends MapView {
        private static final long serialVersionUID = -5055412016973925447L;

        public MapDisplayView(final Model model) {
            super(model);
        }
    
        @Override
        public void drawOnMap(final Graphics2D g) {
            drawEnvOnView(g);
            drawZoomLevel(g);
        }
    }

    /**
     * 
     */
    public MapDisplay() {
        super();
        setLayout(new BorderLayout());
    }

    @Override
    public void dispose() {
        super.dispose();
        this.removeAll();
        if (mapView != null) {
            mapView.dispose();
            remove(mapView);
        }
        mapView = null;
    }

    @Override
    protected void drawBackground(final Graphics2D g) {

    }

    private void drawZoomLevel(final Graphics2D g) {
        g.setColor(Color.BLACK);
        g.drawString("LOD: " + getWormhole().getZoom(), 0f, g.getFont().getSize2D());
    }

    private File getMapFile(final File f) {
        final StringBuilder sb = new StringBuilder(f.getPath());
        final String dot = ".";
        sb.delete(sb.lastIndexOf(dot), sb.length());
        sb.append(MAPS_FORGE_EXTENSION);
        return new File(sb.toString());
    }

    @Override
    public void initialized(final IEnvironment<T> env) {
        super.initialized(env);
        final IMapEnvironment<T> e = (IMapEnvironment<T>) env;
        final Model mapModel = new Model();
        mapView = new MapDisplayView(mapModel);
        setWormhole(new MapWormhole(getWormhole(), mapModel));
        setZoomManager(new LinZoomManager(1, 1));
        getWormhole().setEnvPosition(new Point2D.Double(getWormhole().getEnvOffset().getX() + getWormhole().getEnvSize().getWidth() / 2, getWormhole().getEnvOffset().getY() + getWormhole().getEnvSize().getHeight() / 2));
        getWormhole().setOptimalZoomRate();
        getZoomManager().setZoom(getWormhole().getZoom());
//        add(MapViewer.createMapView(mapView, mapModel, getMapFile(e.getMapFile())), BorderLayout.CENTER);
        add(MapViewer.createMapView(mapView, mapModel, null), BorderLayout.CENTER);
        revalidate();
        super.initialized(env);
    }



    @Override
    protected void onFirstResizing() {

    }

    @Override
    protected void setDist(final int x, final int y) {
        try {
            super.setDist(x, y);
        } catch (final IllegalArgumentException e) {
            return;
        }
    }

    @Override
    protected void updateView() {
        try {
            mapView.repaint();
        } catch (final NullPointerException e) {
            return;
        }
    }

    @Override
    public void finished(final IEnvironment<T> env, final ITime time, final long step) {
        /*
         * Shut down the download threads, preventing memory leaks
         */
        mapView.getLayerManager().interrupt();
        super.finished(env, time, step);
    }

}
