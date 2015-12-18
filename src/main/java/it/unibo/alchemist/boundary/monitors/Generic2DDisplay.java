/*
 * Copyright (C) 2010-2015, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.alchemist.boundary.gui.effects.Effect;
import it.unibo.alchemist.boundary.interfaces.Graphical2DOutputMonitor;
import it.unibo.alchemist.boundary.l10n.R;
import it.unibo.alchemist.boundary.wormhole.implementation.AngleManagerImpl;
import it.unibo.alchemist.boundary.wormhole.implementation.ExponentialZoomManager;
import it.unibo.alchemist.boundary.wormhole.implementation.PointAdapter;
import it.unibo.alchemist.boundary.wormhole.implementation.PointerSpeedImpl;
import it.unibo.alchemist.boundary.wormhole.implementation.Wormhole2D;
import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D.Mode;
import it.unibo.alchemist.boundary.wormhole.interfaces.PointerSpeed;
import it.unibo.alchemist.boundary.wormhole.interfaces.ZoomManager;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.core.interfaces.Status;
import it.unibo.alchemist.model.implementations.positions.Continuous2DEuclidean;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IEnvironment2DWithObstacles;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IObstacle2D;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;

/**
 * Abstract base-class for each display able a graphically represent a 2D space
 * and simulation.
 * 
 * @param <T>
 */
public class Generic2DDisplay<T> extends JPanel implements Graphical2DOutputMonitor<T> {

    static {
        System.setProperty("sun.java2d.opengl", "true");
    }

    /**
     * The default frame rate.
     */
    public static final byte DEFAULT_FRAME_RATE = 25;

    private static final double TIME_STEP = 1d / DEFAULT_FRAME_RATE;
    private static final double FREEDOM_RADIUS = 1d;
    private static final Logger L = LoggerFactory.getLogger(Generic2DDisplay.class);
    private static final int MS_PER_SECOND = 1000;

    /**
     * 
     */
    public static final long PAUSE_DETECTION_THRESHOLD = 200;
    /**
     * How big (in pixels) the selected node should appear.
     */
    private static final byte SELECTED_NODE_DRAWING_SIZE = 16, SELECTED_NODE_INTERNAL_SIZE = 10;
    private static final long serialVersionUID = 511631766719686842L;

    private transient AngleManagerImpl angleManager;
    private IEnvironment<T> currentEnv;
    private List<Effect> effectStack;
    private boolean firstTime = true, paintLinks;
    private transient Optional<INode<T>> hooked = Optional.empty();
    private boolean inited;
    private double lasttime;
    private final Semaphore mapConsistencyMutex = new Semaphore(1);
    private boolean markCloser = true;
    private final transient PointerSpeed mouseVelocity = new PointerSpeedImpl();
    private int mousex, mousey;
    private INode<T> nearest;
    private final ConcurrentMap<INode<T>, INeighborhood<T>> neighbors = new ConcurrentHashMap<>();
    private List<? extends IObstacle2D> obstacles;
    private final ConcurrentMap<INode<T>, IPosition> positions = new ConcurrentHashMap<>();
    private boolean realTime;
    private int st;

    private long timeInit = System.currentTimeMillis();

    private transient IWormhole2D wormhole;

    private transient ZoomManager zoomManager;

    /**
     * Initializes a new display with out redrawing the first step.
     */
    public Generic2DDisplay() {
        this(1);
    }

    /**
     * Initializes a new display.
     * 
     * @param step
     *            number of steps to let pass without re-drawing
     */
    public Generic2DDisplay(final int step) {
        super();
        if (!"true".equals(System.getProperty("sun.java2d.opengl"))) {
            L.warn("OpenGL acceleration appears to be disabled on this system. This may impact performance negatively. Please enable it with -Dsun.java2d.opengl=true");
        }
        setStep(step);
        setBackground(Color.WHITE);
        inited = false;
        final MouseManager mgr = new MouseManager();
        addMouseListener(mgr);
        addMouseMotionListener(mgr);
        addMouseWheelListener(mgr);
        bindKeys();
    }

    private void bindKeys() {
        bindKey(KeyEvent.VK_M, () -> setMarkCloserNode(!isCloserNodeMarked()));
        bindKey(KeyEvent.VK_L, () -> setDrawLinks(!paintLinks));
        bindKey(KeyEvent.VK_P, () -> Optional.ofNullable(Simulation.fromEnvironment(currentEnv))
                .ifPresent(sim -> {
                    if (sim.getStatus() == Status.RUNNING) {
                        sim.pause();
                    } else {
                        sim.play();
                    }
                }));
        bindKey(KeyEvent.VK_R, () -> setRealTime(!isRealTime()));
        bindKey(KeyEvent.VK_LEFT, () -> setStep(Math.max(1, st - Math.max(st / 10, 1))));
        bindKey(KeyEvent.VK_RIGHT, () -> setStep(Math.max(st, st + Math.max(st / 10, 1))));
    }

    private void accessData() {
        mapConsistencyMutex.acquireUninterruptibly();
    }

    private Shape convertObstacle(final IObstacle2D o) {
        final Rectangle2D r = o.getBounds2D();
        final IPosition[] points = new IPosition[] {
                new Continuous2DEuclidean(r.getX(), r.getY()),
                new Continuous2DEuclidean(r.getX() + r.getWidth(), r.getY()),
                new Continuous2DEuclidean(r.getX() + r.getWidth(), r.getY() + r.getHeight()),
                new Continuous2DEuclidean(r.getX(), r.getY() + r.getHeight()) };
        final Path2D path = new GeneralPath();
        for (int i = 0; i < points.length; i++) {
            final Point pt = wormhole.getViewPoint(points[i]);
            if (i == 0) {
                path.moveTo(pt.getX(), pt.getY());
            }
            path.lineTo(pt.getX(), pt.getY());
        }
        path.closePath();
        return path;
    }

    /**
     * This method is meant to be overridden by subclasses that want to display
     * a more sophisticated background than a simple color.
     * 
     * @param g
     *            the Graphics2D to use
     */
    protected void drawBackground(final Graphics2D g) {
    }

    /**
     * Actually draws the environment on the view.
     * 
     * @param g
     *            {@link Graphics2D} object responsible for drawing
     */
    protected final void drawEnvOnView(final Graphics2D g) {
        if (wormhole == null || !isVisible() || !isEnabled()) {
            return;
        }
        accessData();
        if (hooked.isPresent()) {
            final IPosition hcoor = positions.get(hooked.get());
            final Point hp = wormhole.getViewPoint(hcoor);
            if (hp.distance(getCenter()) > FREEDOM_RADIUS) {
                wormhole.setViewPosition(hp);
            }
        }
        /*
         * Compute nodes in sight and their screen position
         */
        final Map<INode<T>, Point> onView = positions.entrySet().parallelStream()
                .map(pair -> new Pair<>(pair.getKey(), wormhole.getViewPoint(pair.getValue())))
                .filter(p -> wormhole.isInsideView(p.getSecond()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        g.setColor(Color.BLACK);
        if (obstacles != null) {
            /*
             * TODO: only draw obstacles if on view
             */
            obstacles.parallelStream()
                .map(this::convertObstacle)
                .forEachOrdered(g::fill);
        }
        if (paintLinks) {
            g.setColor(Color.GRAY);
            onView.keySet().parallelStream()
                .map(neighbors::get)
                .flatMap(neigh -> neigh.getNeighbors().parallelStream()
                    .map(node -> node.compareTo(neigh.getCenter()) > 0
                                ? new Pair<>(neigh.getCenter(), node)
                                : new Pair<>(node, neigh.getCenter())))
                .distinct()
                .map(pair -> mapPair(pair, node ->
                        Optional.ofNullable(onView.get(node))
                        .orElse(wormhole.getViewPoint(positions.get(node)))))
                .forEachOrdered(line -> {
                    final Point p1 = line.getFirst();
                    final Point p2 = line.getSecond();
                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                });
        }
        releaseData();
        g.setColor(Color.GREEN);
        if (effectStack != null) {
            effectStack.forEach(effect -> {
                onView.entrySet().forEach(entry -> {
                    final Point p = entry.getValue();
                    effect.apply(g, entry.getKey(), p.x, p.y);
                });
            });
        }
        if (isCloserNodeMarked()) {
            final Optional<Map.Entry<INode<T>, Point>> closest = onView.entrySet().parallelStream()
                    .min((pair1, pair2) -> {
                        final Point p1 = pair1.getValue();
                        final Point p2 = pair2.getValue();
                        final double d1 = Math.hypot(p1.x - mousex, p1.y - mousey);
                        final double d2 = Math.hypot(p2.x - mousex, p2.y - mousey);
                        return Double.compare(d1, d2);
                    });
            if (closest.isPresent()) {
                nearest = closest.get().getKey();
                final int nearestx = closest.get().getValue().x;
                final int nearesty = closest.get().getValue().y;
                g.setColor(Color.RED);
                g.fillOval(nearestx - SELECTED_NODE_DRAWING_SIZE / 2, nearesty - SELECTED_NODE_DRAWING_SIZE / 2, SELECTED_NODE_DRAWING_SIZE, SELECTED_NODE_DRAWING_SIZE);
                g.setColor(Color.YELLOW);
                g.fillOval(nearestx - SELECTED_NODE_INTERNAL_SIZE / 2, nearesty - SELECTED_NODE_INTERNAL_SIZE / 2, SELECTED_NODE_INTERNAL_SIZE, SELECTED_NODE_INTERNAL_SIZE);
            }
        }
    }

    @Override
    public void finished(final IEnvironment<T> environment, final ITime time, final long step) {
        update(environment, time);
        firstTime = true;
    }

    /**
     * Gets the view center point.
     * 
     * @return the center
     */
    private Point getCenter() {
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    @Override
    public int getStep() {
        return st;
    }

    /**
     * Lets child-classes access the wormhole.
     * 
     * @return an {@link IWormhole2D}
     */
    protected final IWormhole2D getWormhole() {
        return wormhole;
    }

    /**
     * Lets child-classes access the zoom manager.
     * 
     * @return an {@link ZoomManager}
     */
    protected final ZoomManager getZoomManager() {
        return zoomManager;
    }

    /**
     * Initializes all the internal data.
     * 
     * @param r
     *            the reaction executed
     * @param time
     *            the simulation time
     * @param step
     *            the current simulation step
     */
    private void initAll(final IEnvironment<T> env) {
        wormhole = new Wormhole2D(env, this);
        wormhole.center();
        wormhole.optimalZoom();
        angleManager = new AngleManagerImpl(AngleManagerImpl.DEF_DEG_PER_PIXEL);
        zoomManager = new ExponentialZoomManager(wormhole.getZoom(), ExponentialZoomManager.DEF_BASE);
        if (env instanceof IEnvironment2DWithObstacles) {
            loadObstacles(env);
        } else {
            obstacles = null;
        }
    }

    @Override
    public void initialized(final IEnvironment<T> environment) {
        stepDone(environment, null, new DoubleTime(), 0);
    }

    /**
     * @return true if the closer node is marked
     */
    protected final boolean isCloserNodeMarked() {
        return markCloser;
    }

    /**
     * Lets child-classes check if the display is initialized.
     * 
     * @return a <code>boolean</code> value
     */
    protected boolean isInitilized() {
        return inited;
    }

    /**
     * @return true if this monitor is trying to draw in realtime
     */
    @Override
    public final boolean isRealTime() {
        return realTime;
    }

    private void loadObstacles(final IEnvironment<T> env) {
        obstacles = ((IEnvironment2DWithObstacles<?, ?>) env).getObstacles();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        drawBackground((Graphics2D) g);
        drawEnvOnView((Graphics2D) g);
    }

    private void releaseData() {
        mapConsistencyMutex.release();
    }

    /**
     * 
     * @param x x coord
     * @param y y coord
     */
    protected void setDist(final int x, final int y) {
        if (wormhole != null) {
            mousex = x;
            mousey = y;
            final IPosition envMouse = wormhole.getEnvPoint(new Point(mousex, mousey));
            final StringBuilder sb = new StringBuilder();
            sb.append(envMouse);
            if (nearest != null) {
                sb.append(" -- ");
                sb.append(R.getString("nearest_node_is"));
                sb.append(": ");
                sb.append(nearest.getId());
            }
            setToolTipText(sb.toString());
        }
    }

    @Override
    public void setDrawLinks(final boolean b) {
        if (paintLinks != b) {
            paintLinks = b;
            repaint();
        }
    }

    @Override
    public void setEffectStack(final List<Effect> l) {
        effectStack = l;
    }

    @Override
    public void setMarkCloserNode(final boolean mark) {
        if (mark != markCloser) {
            markCloser = mark;
            repaint();
        }
    }

    @Override
    public void setRealTime(final boolean rt) {
        realTime = rt;
    }

    @Override
    public final void setStep(final int step) {
        if (step <= 0) {
            throw new IllegalArgumentException("The parameter must be a positive integer");
        }
        st = step;
    }

    /**
     * Lets child-classes change the wormhole.
     * 
     * @param w
     *            an {@link IWormhole2D}
     */
    protected void setWormhole(final IWormhole2D w) {
        Objects.requireNonNull(w);
        wormhole = w;
    }

    /**
     * Lets child-classes change the zoom manager.
     * 
     * @param zm
     *            an {@link ZoomManager}
     */
    protected void setZoomManager(final ZoomManager zm) {
        zoomManager = zm;
        wormhole.setZoom(zoomManager.getZoom());
    }

    @Override
    public void stepDone(final IEnvironment<T> environment, final IReaction<T> r, final ITime time, final long step) {
        if (firstTime) {
            if (firstTime) {
                initAll(environment);
                lasttime = -TIME_STEP;
                firstTime = false;
                timeInit = System.currentTimeMillis();
                update(environment, time);
            }
        } else if (st < 1 || step % st == 0) {
            if (isRealTime()) {
                if (lasttime + TIME_STEP > time.toDouble()) {
                    return;
                }
                final long timeSimulated = (long) (time.toDouble() * MS_PER_SECOND);
                if (timeSimulated == 0) {
                    timeInit = System.currentTimeMillis();
                }
                final long timePassed = System.currentTimeMillis() - timeInit;
                if (timePassed - timeSimulated > PAUSE_DETECTION_THRESHOLD) {
                    timeInit = timeInit + timePassed - timeSimulated;
                }
                if (timeSimulated > timePassed) {
                    try {
                        Thread.sleep(Math.min(timeSimulated - timePassed, MS_PER_SECOND / DEFAULT_FRAME_RATE));
                    } catch (final InterruptedException e) {
                        L.warn("Damn spurious wakeups.");
                    }
                }
            }
            update(environment, time);
        }
    }

    private void update(final IEnvironment<T> env, final ITime time) {
        if (envHasMobileObstacles(env)) {
            loadObstacles(env);
        }
        lasttime = time.toDouble();
        currentEnv = env;
        accessData();
        positions.clear();
        neighbors.clear();
        env.getNodes().parallelStream().forEach(node -> {
            positions.put(node, env.getPosition(node));
            try {
                neighbors.put(node, env.getNeighborhood(node).clone());
            } catch (Exception e) {
                L.error("Unable to clone neighborhood for " + node, e);
            }
        });
        releaseData();
        repaint();
    }

    @Override
    public void zoomTo(final IPosition center, final double zoomLevel) {
        assert center.getDimensions() == 2;
        wormhole.zoomOnPoint(wormhole.getViewPoint(center), zoomLevel);
    }

    /**
     * @param env
     *            the current environment
     * @param <N>
     *            positions
     * @param <D>
     *            distances
     * @return true if env is subclass of {@link IEnvironment2DWithObstacles}
     *         and has mobile obstacles
     */
    protected static <N extends Number, D extends Number> boolean envHasMobileObstacles(final IEnvironment<?> env) {
        return env instanceof IEnvironment2DWithObstacles && ((IEnvironment2DWithObstacles<?, ?>) env).hasMobileObstacles();
    }

    private static <I, O> Pair<O, O> mapPair(
            final Pair<? extends I, ? extends I> pair,
            final Function<? super I, ? extends O> converter) {
        return new Pair<>(converter.apply(pair.getFirst()), converter.apply(pair.getSecond()));
    }

    private class MouseManager implements MouseInputListener, MouseWheelListener, MouseMotionListener {
        @Override
        public void mouseClicked(final MouseEvent e) {
            setDist(e.getX(), e.getY());
            if (isCloserNodeMarked() && nearest != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                final NodeTracker<T> monitor = new NodeTracker<>(nearest);
                monitor.stepDone(currentEnv, null, new DoubleTime(lasttime), st);
                final ISimulation<T> sim = Simulation.fromEnvironment(currentEnv);
                final JFrame frame = makeFrame("Tracker for node " + nearest.getId(), monitor);
                if (sim != null) {
                    sim.addOutputMonitor(monitor);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(final WindowEvent e) {
                            sim.removeOutputMonitor(monitor);
                        }
                    });
                }
            }
            if (nearest != null && SwingUtilities.isMiddleMouseButton(e)) {
                hooked = hooked.isPresent() ? Optional.empty() : Optional.of(nearest);
            }
            repaint();
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            setDist(e.getX(), e.getY());
            if (wormhole == null || mouseVelocity == null) {
                return;
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (mouseVelocity != null && !hooked.isPresent()) {
                    final Point previous = wormhole.getViewPosition();
                    wormhole.setViewPosition(
                            PointAdapter.from(previous)
                                .sum(PointAdapter.from(mouseVelocity.getVariation())).toPoint());
                }
            } else if (SwingUtilities.isRightMouseButton(e) && mouseVelocity != null && angleManager != null && wormhole.getMode() != Mode.MAP) {
                angleManager.inc(mouseVelocity.getVariation().getX());
                wormhole.rotateAroundPoint(getCenter(), angleManager.getAngle());
            }
            mouseVelocity.setCurrentPosition(e.getPoint());
            repaint();
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            updateMouse(e);
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            updateMouse(e);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if (mouseVelocity != null) {
                mouseVelocity.setCurrentPosition(e.getPoint());
            }
            updateMouse(e);
        }

        @Override
        public void mousePressed(final MouseEvent e) {
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(final MouseWheelEvent e) {
            if (wormhole != null && zoomManager != null) {
                zoomManager.dec(e.getWheelRotation());
                wormhole.zoomOnPoint(e.getPoint(), zoomManager.getZoom());
                updateMouse(e);
            }
        }

        private void updateMouse(final MouseEvent e) {
            setDist(e.getX(), e.getY());
            if (isCloserNodeMarked()) {
                repaint();
            }
        }

    }

    private static JFrame makeFrame(final String title, final JPanel content) {
        final JFrame frame = new JFrame(title);
        frame.getContentPane().add(content);
        frame.setLocationByPlatform(true);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    private void bindKey(final int key, final Runnable fun) {
        final Object binder = "Key: " + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), binder);
        getActionMap().put(binder, new AbstractAction() {
            private static final long serialVersionUID = 7927420406960259675L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                fun.run();
            }
        });
    }
}
