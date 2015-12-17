package it.unibo.alchemist.boundary.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.alchemist.boundary.gui.effects.JEffectsTab;
import it.unibo.alchemist.boundary.gui.monitors.JMonitorsTab;
import it.unibo.alchemist.boundary.gui.util.GraphicalMonitorFactory;
import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.boundary.monitors.TimeStepMonitor;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.language.EnvironmentBuilder;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;

/**
 * Utility class for quickly creating non-reusable graphical interfaces.
 */
public final class SingleRunGUI {

    private static final Logger L = LoggerFactory.getLogger(SingleRunGUI.class);

    private SingleRunGUI() {
    }

    /**
     * Builds a single-use graphical interface.
     * 
     * @param sim
     *            the simulation for this GUI
     * @param <T>
     *            concentration type
     */
    public static <T> void make(final ISimulation<T> sim) {
        final GraphicalOutputMonitor<T> main = GraphicalMonitorFactory.createMonitor(sim,
                e -> L.error("Cannot init the UI.", e));
        if (main instanceof Component) {
            sim.addOutputMonitor(main);
            final JFrame frame = new JFrame("Alchemist Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            final JPanel canvas = new JPanel();
            frame.getContentPane().add(canvas);
            canvas.setLayout(new BorderLayout());
            canvas.add((Component) main, BorderLayout.CENTER);
            /*
             * Upper area
             */
            final JPanel upper = new JPanel();
            upper.setLayout(new BoxLayout(upper, BoxLayout.X_AXIS));
            canvas.add(upper, BorderLayout.NORTH);
            final JEffectsTab<T> effects = new JEffectsTab<>(main);
            upper.add(effects);
            final TimeStepMonitor<T> time = new TimeStepMonitor<>();
            sim.addOutputMonitor(time);
            upper.add(time);
            final JPanel lower = new JPanel();
            lower.setLayout(new BoxLayout(lower, BoxLayout.Y_AXIS));
            canvas.add(lower, BorderLayout.SOUTH);
            final JMonitorsTab<T> monitors = new JMonitorsTab<>();
            monitors.setSimulation(sim);
            lower.add(monitors);
            /*
             * Go on screen
             */
//            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        } else {
            L.error("The default monitor of {} is not compatible with Java Swing.", sim);
        }
    }

    public static void main(String... args) throws InterruptedException, ExecutionException, FileNotFoundException {
        final IEnvironment<?> env = EnvironmentBuilder
                .build(new FileInputStream("/home/danysk/2015-SASO-DEMO/src-gen/test.xml")).get().getEnvironment();
        final ISimulation<?> sim = new Simulation<>(env, DoubleTime.INFINITE_TIME);
        make(sim);
        sim.run();
    }

}
