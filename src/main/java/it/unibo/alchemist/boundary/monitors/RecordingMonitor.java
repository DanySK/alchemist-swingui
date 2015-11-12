/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.monitors;


import it.unibo.alchemist.boundary.gui.effects.DrawShape;
import it.unibo.alchemist.boundary.gui.effects.Effect;
import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.awt.Point;
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
import java.util.concurrent.Semaphore;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.apache.commons.math3.util.FastMath;
import org.danilopianini.io.FileUtilities;
import org.danilopianini.lang.RangedInteger;
import org.danilopianini.view.ExportForGUI;



/**
 * @author Gianluca Turin
 * 
 * @param <T>
 */
@ExportInspector
public class RecordingMonitor<T> extends EnvironmentInspector<T> {
	
	
	/**
	 * The source reactivity.
	 * 
	 * @author Gianluca Turin
	 *
	 */
	protected enum ReactivityMode {
		REALTIME, MAX
	}
	
	private static final long serialVersionUID = 1L;
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
	private Generic2DDisplay<T> source;
	private final Semaphore mutex;
	private String fpCache;
	private String efCache;
	private final String defaultFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + sdf.format(new Date()) + "-alchemist_screenshots";
	private final String defaultEffectsFile = System.getProperty("user.home") + System.getProperty("file.separator") + "???";
	private PrintStream writer;
	private int screenCounter = 0;
	private final List<Effect> defEffects = new ArrayList<Effect>(Collections.singletonList(new DrawShape()));
	private SVGGraphics2D svgGraphicator;
	private long lastStep = Long.MIN_VALUE;
	private double lastUpdate = Long.MIN_VALUE;
	@SuppressWarnings("rawtypes")
	private static final Class< ? extends GraphicalOutputMonitor> DEFAULT_MONITOR_CLASS = Generic2DDisplay.class;
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
		String monitorClassName = env.getPreferredMonitor();
		Class< ? extends GraphicalOutputMonitor<T>> monitorClass;
		if (monitorClassName == null) {
			monitorClass = (Class< ? extends GraphicalOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
		} else {
			if (!monitorClassName.contains(".")) {
				monitorClassName = DEFAULT_MONITOR_PACKAGE + monitorClassName;
			}
			try {
				monitorClass = (Class<GraphicalOutputMonitor<T>>) Class.forName(monitorClassName);
			} catch (final ClassNotFoundException e) {
				L.warn(e);
				monitorClass = (Class< ? extends GraphicalOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
			}
		}
		try {
			source = (Generic2DDisplay<T>) monitorClass.getConstructor().newInstance();	
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			L.error(e);
		}
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
		createMonitor(env);
		source.setVisible(true);
		source.setEnabled(true);
		source.setRealTime(reactMode.equals(ReactivityMode.REALTIME));
		source.setSize(width.getVal(), height.getVal());
		source.initialized(env);
		for (MouseListener listener : source.getMouseListeners()) {
			source.removeMouseListener(listener);
		}
		//avoid nearest node circle
		source.setDist(0, 0);
		
		saveScreenshot(env, null, new DoubleTime(), 0);
	}
	
	/**
	 * Set the effects file path.
	 * @param ef new file path
	 */
	public void setEffectsFile(final String ef) {
		effectsFile = ef;
	}
	
	@Override
	public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		mutex.acquireUninterruptibly();
		final double sample = getInterval().getVal() * FastMath.pow(10, getIntervalOrderOfMagnitude().getVal());
		final boolean log = getMode().equals(Mode.TIME) ?  time.toDouble() - lastUpdate >= sample : step - lastStep >= sample;
		if (log) {
			lastUpdate = time.toDouble();
			lastStep = step;
			saveScreenshot(env, r, time, step);
		}
		mutex.release();
	}

	/**
	 * Save in a svg file a screenshot of the current source.
	 * @param env unused
	 * @param r unused
	 * @param time the current time of the simulation that could be added to the file name
	 * @param step the current step of the simulation that could be added to the file name
	 */
	@SuppressWarnings("unchecked")
	private void saveScreenshot(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
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
					source.revalidate();
				}
			}
			
			final int zoomVal = zoom.getVal();
			if (zoomVal == 0) {
				source.getWormhole().setOptimalZoomRate();
			} else {
				source.getWormhole().zoomOnPoint(new Point(0, height.getVal()), zoomVal);
			}
			source.revalidate();
			
			source.getWormhole().setViewPosition(new Point(width.getVal() * povX.getVal() / 100, (height.getVal() * povY.getVal() / 100) + height.getVal()));
			
			lastStep = step;
			lastUpdate = time.toDouble();
			String currentStep = isLoggingStep() ? getSeparator() + step : "";
			String currentTime = isLoggingTime() ? getSeparator() + time : "";			
				
			try {
				new File(fpCache).mkdirs();
				writer = new PrintStream(new File(fpCache + System.getProperty("file.separator") + screenCounter++ + currentStep + currentTime + ".svg"), StandardCharsets.UTF_8.name());
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				L.error(e);
			}
			
			svgGraphicator = new SVGGraphics2D(width.getVal(), height.getVal());
			source.setDrawLinks(drawLinks);
			source.paintComponent(svgGraphicator);
			writer.print(svgGraphicator.getSVGDocument());
			writer.close();
		}
		
	}
	
	@Override
	protected double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		/**
		 * Unused.
		 */
		return new double[0];
	}
}
