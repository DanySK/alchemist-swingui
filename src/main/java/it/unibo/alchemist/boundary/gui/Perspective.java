/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import it.unibo.alchemist.boundary.gui.ReactivityPanel.Status;
import it.unibo.alchemist.boundary.gui.UpperBar.Commands;
import it.unibo.alchemist.boundary.gui.effects.JEffectsTab;
import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.boundary.interfaces.SwingOutputMonitor;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.boundary.monitors.Abstract2DDisplay;
import it.unibo.alchemist.boundary.monitors.Generic2DDisplay;
import it.unibo.alchemist.boundary.monitors.TimeStepMonitor;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;
import it.unibo.alchemist.language.EnvironmentBuilder;
import it.unibo.alchemist.language.EnvironmentBuilder.Result;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.utils.L;

/**
 * @author Danilo Pianini
 * 
 * @param <T>
 */
public class Perspective<T> extends JPanel implements ChangeListener, ActionListener {

	private static final long serialVersionUID = -6074331788924400019L;
	private static final FileFilter XML_FILTER = new FileNameExtensionFilter(r(Res.ALCHEMIST_XML), "xml");

	private final UpperBar bar;

	private File currentDirectory = new File(System.getProperty("user.home"));
	private SwingOutputMonitor<T> main;
	private RandomEngine rand;
	private final SimControlPanel scp = SimControlPanel.createControlPanel(null);
	private final JEffectsTab<T> effectsTab;
	private transient ISimulation<T> sim;
	private final StatusBar status;
	private File xml;

	@SuppressWarnings("rawtypes")
	private static final Class<? extends GraphicalOutputMonitor> DEFAULT_MONITOR_CLASS = Generic2DDisplay.class;
	private static final String DEFAULT_MONITOR_PACKAGE = "it.unibo.alchemist.boundary.monitors.";

	private static String r(final Res res) {
		return Res.get(res);
	}

	/**
	 * Builds a new SAPERE perspective.
	 */
	public Perspective() {
		super();
		setLayout(new BorderLayout());
		
		bar = new UpperBar(scp);
		add(bar, BorderLayout.NORTH);
		bar.addActionListener(this);
		bar.addChangeListener(this);
		
		status = new StatusBar();
		status.setText(r(Res.SAPERE_PERSPECTIVE));
		add(status, BorderLayout.SOUTH);

		effectsTab = new JEffectsTab<>();
		effectsTab.addLinksToggleActionListener(this);
		effectsTab.setEnabled(false);

		bar.registerTab(effectsTab);

		setMainDisplay(new Generic2DDisplay<T>());
	}
	
	private void dispose() {
		if (main != null) {
			main.dispose();
			if (sim != null) {
				sim.removeOutputMonitor(main);
			}
			remove((Component) main);
		}
		main = null;
		sim = null;
		effectsTab.setMonitor(null);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (Commands.OPEN.equalsToString(e.getActionCommand())) {
			openXML();
		} else if (Commands.PARALLEL.equalsToString(e.getActionCommand())) {
			process(true);
		} else if (Commands.PROCESS.equalsToString(e.getActionCommand())) {
			process(false);
		} else if (Commands.DICE.equalsToString(e.getActionCommand())) {
			setRandom();
		} else if (SimControlCommand.PLAY.equalsToString(e.getActionCommand())) {
			sim.play();
			bar.setPlay(true);
		} else if (SimControlCommand.PAUSE.equalsToString(e.getActionCommand())) {
			sim.pause();
			bar.setPlay(false);
		} else if (SimControlCommand.STEP.equalsToString(e.getActionCommand())) {
			sim.play();
			sim.pause();
		} else if (SimControlCommand.STOP.equalsToString(e.getActionCommand())) {
			sim.stop();
			bar.setFileOK(true);
		} else if (Commands.PAINT_LINKS.equalsToString(e.getActionCommand())) {
			main.setDrawLinks(effectsTab.isDrawingLinks()); // side.isDrawingLinks());
		} else if (Commands.REACTIVITY.equalsToString(e.getActionCommand())) {
			switch (bar.getReactivityStatus()) {
			case MAX_REACTIVITY:
				main.setStep(1);
				main.setRealTime(false);
				break;
			case REAL_TIME:
				main.setRealTime(true);
				main.setStep(1);
				break;
			case USER_SELECTED:
				main.setStep(bar.getReactivity());
				main.setRealTime(false);
				break;
			default:
				break;
			}
		} else {
			dispose();
		}
	}

	@SuppressWarnings("unchecked")
	private void createMonitor() {
		String monitorClassName = sim.getEnvironment().getPreferredMonitor();
		Class<? extends SwingOutputMonitor<T>> monitorClass;
		if (monitorClassName == null) {
			monitorClass = (Class<? extends SwingOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
		} else {
			if (!monitorClassName.contains(".")) {
				monitorClassName = DEFAULT_MONITOR_PACKAGE + monitorClassName;
			}
			try {
				monitorClass = (Class<SwingOutputMonitor<T>>) Class.forName(monitorClassName);
			} catch (final ClassNotFoundException e) {
				L.warn(e);
				monitorClass = (Class<? extends SwingOutputMonitor<T>>) DEFAULT_MONITOR_CLASS;
			}
		}
		try {
			final SwingOutputMonitor<T> display = monitorClass.getConstructor().newInstance();
			setMainDisplay(display);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			L.error(e);
		}
	}

	private void openXML() {
		final JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(false);
		fc.setFileFilter(XML_FILTER);
		fc.setCurrentDirectory(currentDirectory);
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			xml = fc.getSelectedFile();
			currentDirectory = fc.getSelectedFile().getParentFile();
			if (xml.exists() && xml.getName().endsWith("xml")) {
				status.setText(r(Res.READY_TO_PROCESS) + " " + xml.getAbsolutePath());
				status.setOK();
				if (sim != null) {
					sim.stop();
				}
				bar.setFileOK(true);
			} else {
				status.setText(r(Res.FILE_NOT_VALID) + " " + xml.getAbsolutePath());
				status.setNo();
				bar.setFileOK(false);
			}
		}
	}

	private void process(final boolean parallel) {
		if (sim != null) {
			sim.stopAndWait();
		}
		try {
			sim = null;
			final Future<Result<T>> fenv = EnvironmentBuilder.build(new FileInputStream(xml));
			final IEnvironment<T> env = fenv.get().getEnvironment();
			rand = fenv.get().getRandomEngine();
			sim = new Simulation<>(env, new DoubleTime(Double.POSITIVE_INFINITY), parallel);
			bar.setSimulation(sim);
			scp.setSimulation(sim);
			final Thread simThread = new Thread(sim);
			createMonitor();
			simThread.start();
			final TimeStepMonitor<T> tm = bar.getTimeMonitor();
			sim.addOutputMonitor(tm);
			bar.setRandom(rand.getSeed());
			bar.setFileOK(true);
			bar.setProcessOK(true);
			effectsTab.setEnabled(true);
			status.setOK();
			status.setText(r(Res.FILE_PROCESSED) + ": " + xml.getAbsolutePath());
		} catch (Exception e) {
			processError(e);
		}
	}
	
	private void processError(final Throwable e) {
		SwingUtilities.invokeLater(() -> {
			bar.setFileOK(false);
			bar.setProcessOK(false);
			status.setText(r(Res.FILE_NOT_VALID) + " " + xml.getAbsolutePath());
			status.setNo();
			L.error(e);
		});
	}

	private void setMainDisplay(final SwingOutputMonitor<T> gom) {
		if (main != null) {
			sim.removeOutputMonitor(main);
			gom.setStep(main.getStep());
			gom.setRealTime(main.isRealTime());
			remove((Component) main);
			main.dispose();
		}
		main = gom;
		if (sim != null) {
			new Thread(() -> sim.addOutputMonitor(main)).start();
		}
		add((Component) main, BorderLayout.CENTER);
		revalidate();
		effectsTab.setMonitor(gom);
		gom.setDrawLinks(effectsTab.isDrawingLinks());
	}

	private void setRandom() {
		if (rand != null) {
			try {
				rand.setSeed(bar.getRandomText());
				status.setOK();
				status.setText(r(Res.RANDOM_REINIT_SUCCESS) + ": " + rand.getSeed());
			} catch (final NumberFormatException e) {
				status.setNo();
				status.setText(r(Res.RANDOM_REINIT_FAIL) + ": " + r(Res.IS_NOT_AN_INTEGER));
			}
		} else {
			status.setNo();
			status.setText(r(Res.RANDOM_REINIT_FAIL) + ": RandomEngine " + r(Res.IS_NOT_INITIALIZED_YET));
		}
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		if (bar.getReactivityStatus().equals(Status.USER_SELECTED)) {
			main.setStep(bar.getReactivity());
		}
	}

}
