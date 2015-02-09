/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui.effects;

import it.unibo.alchemist.boundary.gui.AlchemistSwingUI;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.utils.L;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.reflections.Reflections;

/**
 * @author Danilo Pianini
 * 
 */
public class EffectBuilder extends JFrame implements ActionListener {

	private static final Reflections REFLECTIONS = new Reflections("it.unibo.alchemist");
	private static final long serialVersionUID = -5030318714404946998L;
	private static final Set<Class<? extends Effect>> EFFECTS = REFLECTIONS.getSubTypesOf(Effect.class);
	private final JButton button = new JButton(r(Res.DONE));
	private final CountDownLatch barrier = new CountDownLatch(1);
	private final JComboBox<Class<? extends Effect>> effectBox;
	
	private static String r(final Res r) {
		return Res.get(r);
	}

	/**
	 * Default constructor.
	 */
	public EffectBuilder() {
		super(r(Res.ALCHEMIST) + " " + r(Res.EFFECT_BUILDER));
		setUndecorated(true);
		final Container pane = getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		final JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final Icon effectIcon = AlchemistSwingUI.loadScaledImage("/oxygen/actions/tools-wizard.png");
		p1.add(new JLabel(r(Res.EFFECT), effectIcon, SwingConstants.LEADING));
		pane.add(p1);
		pane.add(Box.createVerticalGlue());
		effectBox = new JComboBox<>();
		for (final Class<? extends Effect> c : EFFECTS) {
			if (!Modifier.isAbstract(c.getModifiers())) {
				effectBox.addItem(c);
			}
		}
		pane.add(effectBox);
		final JPanel p4 = new JPanel();
		final Icon done = AlchemistSwingUI.loadScaledImage("/oxygen/categories/applications-graphics.png");
		button.setIcon(done);
		p4.add(button);
		pane.add(p4);
		button.addActionListener(this);

		pane.add(Box.createVerticalGlue());
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		barrier.countDown();
		setVisible(false);
	}

	/**
	 * @return a future with the built effect
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Effect> getResult() {
		try {
			barrier.await();
		} catch (final InterruptedException e) {
			L.warn(e);
		}
		return (Class<? extends Effect>) effectBox.getSelectedItem();
	}

}
