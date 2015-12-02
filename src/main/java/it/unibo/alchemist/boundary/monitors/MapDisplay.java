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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.swing.MapViewer;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

/**
 * 
 * @param <T>
 */
public class MapDisplay<T> extends Abstract2DDisplay<T> {
    private static final long serialVersionUID = 8593507198560560646L;
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final AtomicInteger IDGEN = new AtomicInteger();
    private MapView mapView;

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
//            mapView.dispose();
            remove(mapView);
        }
        mapView = null;
    }

    @Override
    protected void drawBackground(final Graphics2D g) {
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        if (mapView != null) {
            mapView.paint(g);
        }
          drawEnvOnView((Graphics2D) g);
    };

    @Override
    public void initialized(final IEnvironment<T> env) {
        super.initialized(env);
        final IMapEnvironment<T> e = (IMapEnvironment<T>) env;
        mapView = new MapView();
        Arrays.stream(getMouseListeners()).forEach(mapView::addMouseListener);
        Arrays.stream(getMouseMotionListeners()).forEach(mapView::addMouseMotionListener);
        Arrays.stream(getMouseWheelListeners()).forEach(mapView::addMouseWheelListener);
        TileDownloadLayer tdl = createTileDownloadLayer(createTileCache(), mapView.getModel());
        mapView.addLayer(tdl);
        tdl.start();
        setWormhole(new MapWormhole(getSize(), env, mapView.getModel().mapViewPosition));
        setZoomManager(new LinZoomManager(1, 1, 2, 18));
        getWormhole().setEnvPosition(new Point2D.Double(env.getOffset()[0] + env.getSize()[0] / 2, env.getOffset()[1] + env.getSize()[1] / 2));
        getWormhole().optimalZoom();
        getZoomManager().setZoom(getWormhole().getZoom());
//        add(MapViewer.createMapView(mapView, mapModel, getMapFile(e.getMapFile())), BorderLayout.CENTER);
        mapView.getMapScaleBar().setVisible(true);
        add(mapView);
        revalidate();
        super.initialized(env);
    }

    private static TileCache createTileCache() {
        TileCache firstLevelTileCache = new InMemoryTileCache(128);
        File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge" + IDGEN.getAndIncrement());
        TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private static TileDownloadLayer createTileDownloadLayer(final TileCache tileCache, final Model model) {
        TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
        TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache, model.mapViewPosition, tileSource, GRAPHIC_FACTORY);
        tileDownloadLayer.setDisplayModel(model.displayModel);
        return tileDownloadLayer;
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
    public void finished(final IEnvironment<T> env, final ITime time, final long step) {
        /*
         * Shut down the download threads, preventing memory leaks
         */
        mapView.getLayerManager().interrupt();
        super.finished(env, time, step);
    }

}
