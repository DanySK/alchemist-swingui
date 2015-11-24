/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.logging;

import it.unibo.alchemist.boundary.gui.tape.JTape;
import it.unibo.alchemist.boundary.gui.tape.JTapeGroup;
import it.unibo.alchemist.boundary.gui.tape.JTapeMainFeature;
import it.unibo.alchemist.boundary.gui.tape.JTapeSection;
import it.unibo.alchemist.boundary.gui.tape.JTapeTab;
import it.unibo.alchemist.boundary.logging.JPrintStream;
import it.unibo.alchemist.utils.L;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class LoggerPerspective extends JPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final JHandler handler;

    private static class JLevelSelector extends JComboBox<Level> {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private static final Level[] LEVELS = { Level.ALL, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.OFF };

        public JLevelSelector() {
            super();
            for (final Level l : LEVELS) {
                addItem(l);
            }
            setSelectedItem(L.getLevel());
        }
    }

    public LoggerPerspective() {
        super();
        setLayout(new BorderLayout(0, 0));

        final JTape tape = new JTape();
        final JTapeTab tab = new JTapeTab("Logging");
        final JTapeGroup group = new JTapeGroup("Log options");
        final JTapeSection section = new JTapeMainFeature();
        final JLevelSelector jls = new JLevelSelector();
        jls.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                L.setLoggingLevel((Level) e.getItem());
            }
        });
        // section.registerFeature(new Label("Log level:"));
        section.registerFeature(jls);
        group.registerSection(section);
        tab.registerGroup(group);
        tape.registerTab(tab);
        add(tape, BorderLayout.NORTH);

        final JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        final JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        final JPrintStream jps = new JPrintStream(textArea);
        handler = new JHandler(jps);
        L.addHandler(handler);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("CloseTab")) {
            L.removeHandler(handler);
        }
    }

}
