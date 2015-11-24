/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import it.unibo.alchemist.boundary.gui.SimControlPanel;
import it.unibo.alchemist.boundary.gui.tape.JTape;
import it.unibo.alchemist.boundary.gui.tape.JTapeGroup;
import it.unibo.alchemist.boundary.gui.tape.JTapeMainFeature;
import it.unibo.alchemist.boundary.gui.tape.JTapeSection;
import it.unibo.alchemist.boundary.gui.tape.JTapeTab;
import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * @param <T>
 */
public class NodeTracker<T> extends JPanel implements OutputMonitor<T>, Runnable, ActionListener {
    private static final byte MARGIN = 100;
    private static final JLabel NOCONTROL = new JLabel(" = Simulation Control Disabled =");
    private static final String PROGRAM = " = Program =", CONTENT = " = Content =", POSITION = " = POSITION = ";
    private static final long serialVersionUID = -676002989218532788L;
    private final JTextArea txt = new JTextArea();
    private final JScrollPane areaScrollPane = new JScrollPane(txt);
    private final JPanel contentPanel = new JPanel();
    private final INode<T> n;
    private final String name;
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
    private final SimControlPanel scp;
    private final TimeStepMonitor<T> times = new TimeStepMonitor<>();
    private JTape tape;
    private JTapeTab tab;
    private JTapeGroup timesGroup;
    private JTapeSection timesSection;
    private int stringLength = Byte.MAX_VALUE;

    /**
     * @param node
     *            the node to track
     * @param env
     *            the environment to use.
     * 
     */
    public NodeTracker(final INode<T> node, final IEnvironment<T> env) {
        super();

        setLayout(new BorderLayout());
        n = node;
        name = "Node " + n.getId() + " tracker";

        add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        txt.setEditable(false);
        contentPanel.add(areaScrollPane, BorderLayout.CENTER);
        areaScrollPane.setPreferredSize(getMaximumSize());
        areaScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        areaScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        final ISimulation<T> simulation = Simulation.fromEnvironment(env);
        scp = SimControlPanel.createControlPanel(simulation);
        if (simulation != null) {
            tape = new JTape();
            tab = new JTapeTab(name);
            timesGroup = new JTapeGroup(Res.get(Res.TIME));
            timesSection = new JTapeMainFeature();

            tab.registerGroup(scp);

            simulation.addOutputMonitor(times);
            timesSection.registerFeature(times);
            timesGroup.registerSection(timesSection);
            tab.registerGroup(timesGroup);

            tape.registerTab(tab);
            contentPanel.add(tape, BorderLayout.NORTH);
        } else {
            stepDone(env, null, null, Long.MIN_VALUE);
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        /*
         * Close operation.
         */
        final ISimulation<T> sim = scp.getSimulation();
        if (sim != null) {
            sim.removeOutputMonitor(this);
        }
    }

    @Override
    public void finished(final IEnvironment<T> env, final ITime time, final long step) {
        if (scp != null) {
            scp.shutdown();
        }
        SwingUtilities.invokeLater(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initialized(final IEnvironment<T> env) {
        SwingUtilities.invokeLater(this);
    }

    @Override
    public void run() {
        final String newtext = queue.poll();
        if (newtext != null && !txt.getText().equals(newtext)) {
            txt.setText(newtext);
        }
    }

    @Override
    public void stepDone(final IEnvironment<T> env, final IReaction<T> exec, final ITime time, final long step) {
        final StringBuilder sb = new StringBuilder(stringLength);
        sb.append(POSITION);
        sb.append('\n');
        sb.append(env.getPosition(n));
        sb.append("\n\n\n");
        sb.append(CONTENT);
        sb.append('\n');
        for (final Entry<IMolecule, T> e : n.getContents().entrySet()) {
            sb.append(e.getKey());
            sb.append(" > ");
            sb.append(e.getValue());
            sb.append('\n');
        }
        sb.append("\n\n\n");
        sb.append(PROGRAM);
        sb.append("\n\n");
        for (final IReaction<T> r : n.getReactions()) {
            sb.append(r.toString());
            sb.append("\n\n");
        }
        if (scp.isDown()) {
            contentPanel.remove(scp);
            add(NOCONTROL);
            revalidate();
        }
        queue.clear();
        queue.add(sb.toString());
        stringLength = sb.length() + MARGIN;
        SwingUtilities.invokeLater(this);
    }
}
