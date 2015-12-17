/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.effects;

import it.unibo.alchemist.boundary.gui.UpperBar.Commands;
import it.unibo.alchemist.boundary.gui.tape.JTapeFeatureStack;
import it.unibo.alchemist.boundary.gui.tape.JTapeFeatureStack.Type;
import it.unibo.alchemist.boundary.gui.tape.JTapeGroup;
import it.unibo.alchemist.boundary.gui.tape.JTapeMainFeature;
import it.unibo.alchemist.boundary.gui.tape.JTapeSection;
import it.unibo.alchemist.boundary.gui.tape.JTapeTab;
import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.boundary.l10n.Res;

import java.awt.Color;
import java.awt.Component;
import java.awt.ItemSelectable;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;

import org.danilopianini.io.FileUtilities;
import org.danilopianini.view.GUIUtilities;

/**
 * Graphic component to handle effects.
 * 
 * @param <T>
 *            is the type for the concentration
 */
public class JEffectsTab<T> extends JTapeTab implements ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = 5687806032498247246L;
    private static final String EXT = ".aes", DESC = "Alchemist Effect Stack";
    private GraphicalOutputMonitor<T> main;
    private final List<ActionListener> listeners = new LinkedList<>();
    private final JTapeFeatureStack stackSec;
    private final JButton addEffectButton, remEffectButton, saveButton, loadButton, moveLeftButton, moveRightButton;
    private final JToggleButton paintLinksButton;
    private File currentDirectory = new File(System.getProperty("user.home"));
    private JEffectRepresentation<T> selected;
    private class CreateEffectBuilder implements ActionListener {
        private final Component parent;

        CreateEffectBuilder(final Component p) {
            parent = p;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final EffectBuilder eb = new EffectBuilder();
            eb.pack();
            final Point location = parent.getLocation();
            SwingUtilities.convertPointToScreen(location, parent);
            eb.setLocation(location);
            eb.setVisible(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Effect effect = EffectFactory.buildEffect(eb.getResult());
                        addEffect(effect);
                        genEvents();
                    } catch (final Exception e1) {
                        GUIUtilities.errorMessage(e1);
                    }
                }
            }).start();
        }
    }

    private static String r(final Res r) {
        return Res.get(r);
    }

    /**
     * Initialize the component.
     */
    public JEffectsTab() {
        super(r(Res.EFFECT_TAB));
        stackSec = new JTapeFeatureStack(Type.HORIZONTAL_STACK);
        final JTapeGroup showGroup = new JTapeGroup(r(Res.SHOW_GROUP));
        final JTapeGroup effectsGroup = new JTapeGroup(r(Res.EFFECTS_GROUP));

        final JTapeSection showLinksSec = new JTapeMainFeature();
        paintLinksButton = new JToggleButton(r(Res.ENABLE_DRAW_LINKS));
        paintLinksButton.setActionCommand(Commands.PAINT_LINKS.toString());
        showLinksSec.registerFeature(paintLinksButton);

        showGroup.registerSection(showLinksSec);

        final JTapeSection saveLoadSec = new JTapeFeatureStack(Type.VERTICAL_STACK);
        saveButton = new JButton(r(Res.SAVE));
        saveButton.addActionListener((e) -> save(makeFileChooser()));
        loadButton = new JButton(r(Res.LOAD));
        loadButton.addActionListener((e) -> load(makeFileChooser()));
        saveLoadSec.registerFeature(saveButton);
        saveLoadSec.registerFeature(loadButton);

        effectsGroup.registerSection(saveLoadSec);

        final JTapeSection addRemSec = new JTapeFeatureStack(Type.VERTICAL_STACK);
        addEffectButton = new JButton(r(Res.ADD_EFFECT));
        addEffectButton.addActionListener(new CreateEffectBuilder(addEffectButton));
        remEffectButton = new JButton(r(Res.REMOVE_EFFECT));
        remEffectButton.addActionListener((event) -> {
            if (selected != null) {
                stackSec.unregisterFeature(selected);
                selected = null;
                genEvents();
            }
        });
        addRemSec.registerFeature(addEffectButton);
        addRemSec.registerFeature(remEffectButton);

        effectsGroup.registerSection(addRemSec);

        final JTapeSection moveSec = new JTapeFeatureStack(Type.VERTICAL_STACK);
        moveLeftButton = new JButton("<");
        moveLeftButton.addActionListener((e) -> moveSelectedLeft());
        moveRightButton = new JButton(">");
        moveRightButton.addActionListener((e) -> moveSelectedRight());
        moveSec.registerFeature(moveLeftButton);
        moveSec.registerFeature(moveRightButton);

        effectsGroup.registerSection(moveSec);

        stackSec.setBorder(new LineBorder(Color.BLACK, 1, false));

        effectsGroup.registerSection(stackSec);

        registerGroup(showGroup);
        registerGroup(effectsGroup);

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (main != null) {
                    main.setEffectStack(getEffects());
                    main.repaint();
                }
            }
        });

        final Effect defaultEffect = EffectFactory.buildDefaultEffect();

        addEffect(defaultEffect);
        genEvents();
    }

    /**
     * See {@link AbstractButton#addActionListener(ActionListener)}.
     * 
     * @param al
     *            the {@link ActionListener} to add
     */
    public void addActionListener(final ActionListener al) {
        listeners.add(al);
    }

    /**
     * Adds a new {@link Effect} to this stack.
     * 
     * @param e
     *            the {@link Effect} to add
     */
    public void addEffect(final Effect e) {
        final JEffectRepresentation<T> er = new JEffectRepresentation<>(e, main);
        registerItemSelectable(er);
        stackSec.registerFeature(er);
    }

    /**
     * Adds listener to the toggle button that enables/disables links.
     * 
     * @param l
     *            is the {@link ActionListener}
     */
    public void addLinksToggleActionListener(final ActionListener l) {
        paintLinksButton.addActionListener(l);
    }

    /**
     * Removes every effect.
     */
    public void clearEffects() {
        stackSec.removeAll();
    }

    private void genEvents() {
        revalidate();
        final ActionEvent event = new ActionEvent(this, 0, "");
        for (final ActionListener al : listeners) {
            al.actionPerformed(event);
        }
    }

    /**
     * @return The list of currently active {@link Effect}s.
     */
    public List<Effect> getEffects() {
        final List<Component> l = stackSec.getOrderedComponents();
        final List<Effect> l1 = new ArrayList<>(l.size());
        for (final Component c : l) {
            l1.add(((JEffectRepresentation<?>) c).getEffect());
        }
        return l1;
    }

    /**
     * @return true if the links are currently drawn
     */
    public boolean isDrawingLinks() {
        return paintLinksButton.isSelected();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            selected = (JEffectRepresentation<T>) e.getItem();
            for (final Component c : stackSec.getComponents()) {
                final JEffectRepresentation<T> er = (JEffectRepresentation<T>) c;
                if (!er.equals(selected) && er.isSelected()) {
                    er.setSelected(false);
                }
            }
        }
    }

    /**
     * Decreases the priority of the selected effect.
     */
    protected void moveSelectedLeft() {
        if (selected != null) {
            final List<Component> l = stackSec.getOrderedComponents();
            final int index = l.indexOf(selected);
            if (index > 0) {
                stackSec.setComponentOrder(selected, index - 1);
                genEvents();
            }
        }
    }

    /**
     * Increases the priority of the selected effect.
     */
    protected void moveSelectedRight() {
        if (selected != null) {
            final List<Component> l = stackSec.getOrderedComponents();
            final int index = l.indexOf(selected);
            final int last = l.size() - 1;
            if (index < last) {
                stackSec.setComponentOrder(selected, index + 1);
                genEvents();
            }
        }
    }

    private void registerItemSelectable(final ItemSelectable is) {
        is.addItemListener(this);
    }

    /**
     * Sets a new effect stack.
     * 
     * @param effects
     *            is a {@link List} of effects
     */
    public void setEffects(final List<Effect> effects) {
        for (final Effect e : effects) {
            addEffect(e);
        }
        genEvents();
    }

    @Override
    public void setEnabled(final boolean value) {
        super.setEnabled(value);
        addEffectButton.setEnabled(value);
        remEffectButton.setEnabled(value);
        saveButton.setEnabled(value);
        loadButton.setEnabled(value);
        moveLeftButton.setEnabled(value);
        moveRightButton.setEnabled(value);
        for (final Component c : stackSec.getComponents()) {
            c.setEnabled(value);
        }
    }

    /**
     * Sets the {@link GraphicalOutputMonitor} to use.
     * 
     * @param d
     *            the {@link GraphicalOutputMonitor} to use
     */
    @SuppressWarnings("unchecked")
    public void setMonitor(final GraphicalOutputMonitor<T> d) {
        main = d;
        if (main != null) {
            main.setEffectStack(getEffects());
        }
        for (final Component c : stackSec.getOrderedComponents()) {
            if (c instanceof JEffectRepresentation) {
                ((JEffectRepresentation<T>) c).setMonitor(main);
            }
        }
    }
    
    private JFileChooser makeFileChooser() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(final File f) {
                return f.getName().endsWith(EXT) || f.isDirectory();
            }
            @Override
            public String getDescription() {
                return DESC;
            }
        });
        fc.setCurrentDirectory(currentDirectory);
        return fc;
    }
    
    private void save(final JFileChooser fc) {
        final int result = fc.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fc.getSelectedFile().getParentFile();
            try {
                final File f = fc.getSelectedFile();
                final File fileToWrite = f.getName().endsWith(EXT) ? f : new File(f.getAbsolutePath() + EXT);
                FileUtilities.objectToFile(getEffects(), fileToWrite, false);
            } catch (final IOException e1) {
                GUIUtilities.errorMessage(e1);
            }
        }
    }
    
    private void load(final JFileChooser fc) {
        final int result = fc.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fc.getSelectedFile().getParentFile();
            try {
                clearEffects();
                @SuppressWarnings("unchecked")
                final List<Effect> effects = (List<Effect>) FileUtilities.fileToObject(fc.getSelectedFile());
                setEffects(effects);
                revalidate();
            } catch (IOException | ClassNotFoundException e1) {
                GUIUtilities.errorMessage(e1);
            }
        }
    }

}
