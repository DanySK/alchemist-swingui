/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import static it.unibo.alchemist.boundary.gui.AlchemistSwingUI.DEFAULT_ICON_SIZE;
import static it.unibo.alchemist.boundary.gui.AlchemistSwingUI.loadScaledImage;

import java.awt.Color;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

/**
 * @param <T>
 *            Concentration type
 */
public class TimeStepMonitor<T> extends JPanel implements OutputMonitor<T> {

    private static final long serialVersionUID = 5818408644038869442L;
    private static final String BLANK = "", FINISHED = " (finished)";
    private static final int BORDER = 10, WIDTH = 200, HEIGHT = DEFAULT_ICON_SIZE + BORDER;
    private static final byte ICON_SIZE = DEFAULT_ICON_SIZE / 2;
    private final Semaphore mutex = new Semaphore(0);
    private boolean isFinished;
    private volatile boolean needsUpdate = true;
    private final JLabel s;
    private long step;
    private final JLabel t;
    private ITime time = new DoubleTime();
    private ExecutorService ex = Executors.newCachedThreadPool();

    /**
     * Constructor.
     */
    public TimeStepMonitor() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final Icon tIcon = loadScaledImage("/oxygen/apps/clock.png", ICON_SIZE);
        final Icon sIcon = loadScaledImage("/oxygen/mimetypes/application-x-executable.png", ICON_SIZE);
        t = new JLabel(BLANK, tIcon, SwingConstants.LEADING);
        s = new JLabel(BLANK, sIcon, SwingConstants.LEADING);
        add(Box.createVerticalGlue());
        add(t);
        add(Box.createVerticalGlue());
        add(s);
        add(Box.createVerticalGlue());
        setBorder(new LineBorder(Color.GRAY));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
    }

    @Override
    public void finished(final IEnvironment<T> env, final ITime tt, final long cs) {
        isFinished = true;
        mutex.release();
        ex.shutdownNow();
        try {
            ex.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            L.warn(e);
        }
    }

    @Override
    public void initialized(final IEnvironment<T> env) {
        isFinished = false;
        ex = Executors.newCachedThreadPool();
        ex.submit(() -> update());
        stepDone(env, null, new DoubleTime(), 0);
    }

    private void update() {
        while (!isFinished) {
            mutex.acquireUninterruptibly();
            mutex.drainPermits();
            if (needsUpdate) {
                needsUpdate = false;
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        t.setText(time.toString() + (isFinished ? FINISHED : BLANK));
                        s.setText(Long.toString(step) + (isFinished ? FINISHED : BLANK));
                    });
                } catch (InvocationTargetException | InterruptedException e) {
                    L.warn(e);
                }
            }
        }
    }

    @Override
    public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime curTime, final long curStep) {
        time = curTime;
        step = curStep;
        needsUpdate = true;
        mutex.release();
    }

}
