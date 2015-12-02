/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.monitors;

import java.awt.Component;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import javax.swing.JComponent;

import org.apache.commons.math3.util.FastMath;
import org.danilopianini.io.FileUtilities;
import org.danilopianini.lang.RangedInteger;
import org.danilopianini.view.ExportForGUI;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.alchemist.boundary.gui.effects.DrawShape;
import it.unibo.alchemist.boundary.gui.effects.Effect;
import it.unibo.alchemist.boundary.interfaces.Graphical2DOutputMonitor;
import it.unibo.alchemist.model.implementations.positions.Continuous2DEuclidean;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;

/**
 * @param <T>
 */
@ExportInspector
public class RecordingMonitor<T> extends EnvironmentInspector<T> {

    /**
     * The source reactivity.
     * 
     */
    protected enum ReactivityMode {
        REALTIME, MAX
    }

    private static final long serialVersionUID = 1L;
    private static final Logger L = LoggerFactory.getLogger(RecordingMonitor.class);
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
    private Graphical2DOutputMonitor<T> source;
    private JComponent sourceComponent;
    private final Semaphore mutex;
    private String fpCache;
    private String efCache;
    private final String defaultFilePath = System.getProperty("user.home") + System.getProperty("file.separator")
            + sdf.format(new Date()) + "-alchemist_screenshots";
    private final String defaultEffectsFile = System.getProperty("user.home") + System.getProperty("file.separator")
            + "???";
    private transient PrintStream writer;
    private int screenCounter;
    private final List<Effect> defEffects = new ArrayList<Effect>(Collections.singletonList(new DrawShape()));
    private transient SVGGraphics2D svgGraphicator;
    private long lastStep = Long.MIN_VALUE;
    private double lastUpdate = Long.MIN_VALUE;
    private static final String DEFAULT_MONITOR_CLASS = Generic2DDisplay.class.getName();
    private static final String DEFAULT_MONITOR_PACKAGE = "it.unibo.alchemist.boundary.monitors.";
    private static final int MIN_WIDTH = 800;
    private static final int DEF_WIDTH = 1000;
    private static final int MAX_WIDTH = 2000;
    private static final int MIN_HEIGHT = 800;
    private static final int DEF_HEIGHT = 1000;
    private static final int MAX_HEIGHT = 2000;
    private static final int MAX_ZOOM = 255;

    @ExportForGUI(nameToExport = "Zoom rate (leave 0 for optimal)")
    private RangedInteger zoom = new RangedInteger(0, MAX_ZOOM, 0);
    @ExportForGUI(nameToExport = "Reactivity")
    private ReactivityMode reactMode = ReactivityMode.MAX;
    @ExportForGUI(nameToExport = "Width")
    private RangedInteger width = new RangedInteger(MIN_WIDTH, MAX_WIDTH, DEF_WIDTH);
    @ExportForGUI(nameToExport = "Height")
    private RangedInteger height = new RangedInteger(MIN_HEIGHT, MAX_HEIGHT, DEF_HEIGHT);
    @ExportForGUI(nameToExport = "Draw links")
    private boolean drawLinks = false;
    @ExportForGUI(nameToExport = "Effects file")
    private String effectsFile = defaultEffectsFile;
    @ExportForGUI(nameToExport = "POV dx (%)")
    private RangedInteger povX = new RangedInteger(-100, 100, 0);
    @ExportForGUI(nameToExport = "POV dy (%)")
    private RangedInteger povY = new RangedInteger(-100, 100, 0);

    /**
     * RecordingMonitor<T> empty constructor.
     */
    public RecordingMonitor() {
        super();
        setFilePath(defaultFilePath);
        mutex = new Semaphore(1);
    }

    @SuppressWarnings("unchecked")
    private void createMonitor(final IEnvironment<T> env) {
        String monitorClassName = Optional.ofNullable(env.getPreferredMonitor()).orElse(DEFAULT_MONITOR_CLASS);
        if (!monitorClassName.contains(".")) {
            monitorClassName = DEFAULT_MONITOR_PACKAGE + monitorClassName;
        }
        try {
            final Class<?> monitorClass = Class.forName(monitorClassName);
            if (Component.class.isAssignableFrom(monitorClass)) {
                initSource(monitorClass);
            } else {
                initSource(Class.forName(DEFAULT_MONITOR_CLASS));
            }
        } catch (final ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            L.error("Cannot create monitor", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void initSource(final Class<?> monitorClass) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        source = (Graphical2DOutputMonitor<T>) monitorClass.getConstructor().newInstance();
        sourceComponent = (JComponent) source;
    }

    @Override
    public void finished(final IEnvironment<T> env, final ITime time, final long step) {
        saveScreenshot(env, null, time, step);
        source.finished(env, time, step);
    }

    /**
     * @return current effects stack file.
     */
    public String getEffectsFile() {
        return effectsFile;
    }

    @Override
    public void initialized(final IEnvironment<T> env) {
        assert source == sourceComponent; // NOPMD
        createMonitor(env);
        sourceComponent.setVisible(true);
        sourceComponent.setEnabled(true);
        sourceComponent.setSize(width.getVal(), height.getVal());
        source.setRealTime(reactMode.equals(ReactivityMode.REALTIME));
        source.initialized(env);
        for (MouseListener listener : sourceComponent.getMouseListeners()) {
            sourceComponent.removeMouseListener(listener);
        }
        // avoid nearest node circle
        source.setMarkCloserNode(false);

        saveScreenshot(env, null, new DoubleTime(), 0);
    }

    /**
     * Set the effects file path.
     * 
     * @param ef
     *            new file path
     */
    public void setEffectsFile(final String ef) {
        effectsFile = ef;
    }

    @Override
    public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
        mutex.acquireUninterruptibly();
        final double sample = getInterval().getVal() * FastMath.pow(10, getIntervalOrderOfMagnitude().getVal());
        final boolean log = getMode().equals(Mode.TIME) ? time.toDouble() - lastUpdate >= sample
                : step - lastStep >= sample;
        if (log) {
            lastUpdate = time.toDouble();
            lastStep = step;
            saveScreenshot(env, r, time, step);
        }
        mutex.release();
    }

    /**
     * Save in a svg file a screenshot of the current source.
     * 
     * @param env
     *            unused
     * @param r
     *            unused
     * @param time
     *            the current time of the simulation that could be added to the
     *            file name
     * @param step
     *            the current step of the simulation that could be added to the
     *            file name
     */
    @SuppressWarnings("unchecked")
    private void saveScreenshot(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
        assert source == sourceComponent; // NOPMD
        if (source != null) {
            source.stepDone(env, r, time, step);
            if (System.identityHashCode(fpCache) != System.identityHashCode(getFilePath())) {
                fpCache = getFilePath();
            }

            if (System.identityHashCode(efCache) != System.identityHashCode(getEffectsFile())) {
                efCache = getEffectsFile();
                List<Effect> effects = null;
                try {
                    effects = (List<Effect>) FileUtilities.fileToObject(getEffectsFile());
                } catch (IOException | ClassNotFoundException e1) {
                    effects = defEffects;
                } finally {
                    source.setEffectStack(effects);
                    sourceComponent.revalidate();
                }
            }

            final int zoomVal = zoom.getVal();
            final double[] offset = env.getOffset();
            final double[] size = env.getSize();
            offset[0] += size[0] / 2;
            offset[1] += size[1] / 2;
            final IPosition center = new Continuous2DEuclidean(offset);
            source.zoomTo(center, zoomVal);
            sourceComponent.revalidate();

//            source.getWormhole().setViewPosition(new Point(width.getVal() * povX.getVal() / 100,
//                    (height.getVal() * povY.getVal() / 100) + height.getVal()));

            lastStep = step;
            lastUpdate = time.toDouble();
            String currentStep = isLoggingStep() ? getSeparator() + step : "";
            String currentTime = isLoggingTime() ? getSeparator() + time : "";

            try {
                new File(fpCache).mkdirs();
                writer = new PrintStream(new File(fpCache + System.getProperty("file.separator") + screenCounter++
                        + currentStep + currentTime + ".svg"), StandardCharsets.UTF_8.name());
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                L.error("Cannot create monitor", e);
            }

            svgGraphicator = new SVGGraphics2D(width.getVal(), height.getVal());
            source.setDrawLinks(drawLinks);
//            final Method paintComponent = sourceComponent.getClass().getMethod("paintComponent", Graphics.class);
//            paintComponent.setAccessible(true);
//            paintComponent.invoke(sourceComponent, svgGraphicator);
            sourceComponent.paint(svgGraphicator);
            writer.print(svgGraphicator.getSVGDocument());
            writer.close();
        }

    }

    @Override
    protected double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time,
            final long step) {
        /**
         * Unused.
         */
        return new double[0];
    }
}
