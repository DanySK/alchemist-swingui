/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.monitors;

import it.unibo.alchemist.boundary.gui.tape.JTapeFeatureStack;
import it.unibo.alchemist.boundary.gui.tape.JTapeFeatureStack.Type;
import it.unibo.alchemist.boundary.gui.tape.JTapeGroup;
import it.unibo.alchemist.boundary.gui.tape.JTapeSection;
import it.unibo.alchemist.boundary.gui.tape.JTapeTab;
import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.boundary.monitors.RecordingMonitor;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.utils.L;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

public class JMonitorsTab<T> extends JTapeTab implements ItemListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -821717582498461584L;
	private final JButton btnAddMonitor = new JButton(r(Res.ATTACH_MONITOR));
	private final JButton btnRemMonitor = new JButton(r(Res.DETACH_MONITOR));
	private final OutputMonitorSelector<T> monitorCombo = new OutputMonitorSelector<>();
	private final JTapeSection monitorsFS = new JTapeFeatureStack(Type.HORIZONTAL_STACK);
	private final List<JOutputMonitorRepresentation<T>> monitors = new LinkedList<>();
	private JOutputMonitorRepresentation<T> selected;
	private ISimulation<T> simulation;

	private static String r(final Res res) {
		return Res.get(res);
	}

	/**
	 * 
	 */
	public JMonitorsTab() {
		super(r(Res.MONITORS));
		simulation = null;
		final JTapeGroup monitorsGroup1 = new JTapeGroup(r(Res.OUTPUT_MONITORS));
		final JTapeGroup monitorsGroup2 = new JTapeGroup(r(Res.MONITORS));
		final JTapeSection monFS = new JTapeFeatureStack();

		monFS.registerFeature(monitorCombo);
		final JPanel p = new JPanel();
		p.setLayout(new GridLayout(0, 2, 0, 0));
		p.add(btnAddMonitor, BorderLayout.WEST);
		p.add(btnRemMonitor, BorderLayout.EAST);
		monFS.registerFeature(p);

		monitorsGroup1.registerSection(monFS);
		monitorsGroup2.registerSection(monitorsFS);

		registerGroup(monitorsGroup1);
		registerGroup(monitorsGroup2);

		btnAddMonitor.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				addOutputMonitor((Class<OutputMonitor<T>>) monitorCombo.getSelectedItem());
			}
		});
		btnRemMonitor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				remOutputMonitor(selected);
				selected = null;
			}
		});
	}

	protected void addOutputMonitor(final Class<OutputMonitor<T>> monClass) {
		if (OutputMonitor.class.isAssignableFrom(monClass)) {
			final OutputMonitor<T> mon;
			final JOutputMonitorRepresentation<T> repr;
			try {
				final Constructor<OutputMonitor<T>> c = monClass.getConstructor();
				mon = c.newInstance();
				
				if(simulation != null) {
					simulation.addOutputMonitor(mon);
				}
				repr = new JOutputMonitorRepresentation<>(mon);
				monitors.add(repr);
				monitorsFS.add(repr);
				repr.addItemListener(this);
				revalidate();
			} catch (final Exception e) {
				L.error(e);
				return;
			}
		}
	}

	public ISimulation<?> getSimulation() {
		return simulation;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void itemStateChanged(final ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			selected = (JOutputMonitorRepresentation<T>) e.getItem();
			for (final JOutputMonitorRepresentation<?> m : monitors) {
				if (!m.equals(selected) && m.isSelected()) {
					m.setSelected(false);
				}
			}
		}
	}

	protected void remOutputMonitor(final JOutputMonitorRepresentation<T> mon) {
		if (mon != null) {
			if(simulation != null) {
				simulation.removeOutputMonitor(mon.getMonitor());
			}
			monitors.remove(mon);
			monitorsFS.remove(mon);
			revalidate();
		}
	}

	/**
	 * @param sim the simulation
	 */
	@SuppressWarnings("unchecked")
	public void setSimulation(final ISimulation<?> sim) {
		if (simulation != null) {
			for (final JOutputMonitorRepresentation<T> jor : monitors) {
				final OutputMonitor<T> mon = jor.getMonitor();
				simulation.removeOutputMonitor(mon);
			}
			simulation.stopAndWait();
		}
		simulation = (ISimulation<T>) sim;
		for (final JOutputMonitorRepresentation<T> jor : monitors) {
			simulation.addOutputMonitor(jor.getMonitor());
		}
	}

}
