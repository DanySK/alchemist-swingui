/*
 * Copyright (C) 2010-2015, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import it.unibo.alchemist.boundary.gui.AlchemistSwingUI;
import it.unibo.alchemist.boundary.gui.effects.Effect;
import it.unibo.alchemist.boundary.interfaces.SwingOutputMonitor;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.boundary.wormhole.implementation.AngleManager;
import it.unibo.alchemist.boundary.wormhole.implementation.DoubleDimension;
import it.unibo.alchemist.boundary.wormhole.implementation.ExpZoomManager;
import it.unibo.alchemist.boundary.wormhole.implementation.NSEAlg2DHelper;
import it.unibo.alchemist.boundary.wormhole.implementation.NSEPointerVelocityHandler;
import it.unibo.alchemist.boundary.wormhole.implementation.NSEWormhole;
import it.unibo.alchemist.boundary.wormhole.interfaces.IAngleManager;
import it.unibo.alchemist.boundary.wormhole.interfaces.IPointerVelocityManager;
import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D.Mode;
import it.unibo.alchemist.boundary.wormhole.interfaces.IZoomManager;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IEnvironment2DWithObstacles;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IObstacle2D;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.jfree.graphics2d.svg.SVGGraphics2D;

/**
 * Abstract base-class for each display able a graphically represent a 2D space
 * and simulation.
 * 
 * @author Giovanni Ciatto
 * @author Danilo Pianini
 * 
 * @param <T>
 */
public abstract class Abstract2DDisplay<T> extends JPanel implements SwingOutputMonitor<T> {
	/**
	 * The default frame rate.
	 */
	public static final byte DEFAULT_FRAME_RATE = 25;
	/**
	 * 
	 */
	public static final long PAUSE_DETECTION_THRESHOLD = 200;

	/**
	 * How big (in pixels) the selected node should appear.
	 */
	private static final byte SELECTED_NODE_DRAWING_SIZE = 16, SELECTED_NODE_INTERNAL_SIZE = 10;
	private static final int MS_PER_SECOND = 1000;
	private static final long serialVersionUID = 511631766719686842L;

	private static final double FREEDOM_RADIUS = 1d;
	private static final double TIME_STEP = 1d / DEFAULT_FRAME_RATE;
	
	private boolean realTime;
	private boolean initialized;
	private int st;
	private List<Effect> effectStack;
	private IEnvironment<T> env;
	private List<? extends IObstacle2D> obstacles;
	private final Map<INode<T>, INeighborhood<T>> neighbors = new ConcurrentHashMap<>();
	private final Map<INode<T>, IPosition> positions = new ConcurrentHashMap<>();
	private Optional<INode<T>> hooked = Optional.empty();
	private IWormhole2D wormhole;
	private IAngleManager angleManager;
	private IZoomManager zoomManager;
	private IPointerVelocityManager mouseVelocity;
	private double dist = java.lang.Double.POSITIVE_INFINITY, lasttime;
	private boolean firstTime = true, paintLinks;
	private long timeInit = System.currentTimeMillis();
	private int mousex, mousey, nearestx, nearesty;
	private final Semaphore mutex = new Semaphore(1);
	private INode<T> nearest;
	private final MouseManager mouseManager = new MouseManager();
	private final ComponentManager componentManager = new ComponentManager();

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

	/**
	 * Initializes a new display with out redrawing the first step.
	 */
	public Abstract2DDisplay() {
		this(1);
	}

	/**
	 * Initializes a new display.
	 * 
	 * @param step
	 *            number of steps to let pass without re-drawing
	 */
	public Abstract2DDisplay(final int step) {
		super();
		st = step;
		setBackground(Color.WHITE);
		initialized = false;
	}

	/**
	 * Updates nodes positions and neighborhoods.
	 * 
	 */
	protected void computeNodes() {
		positions.clear();
		neighbors.clear();
		try {
			for (final INode<T> n : env) {
				positions.put(n, env.getPosition(n));
				neighbors.put(n, env.getNeighborhood(n).clone());
			}
		} catch (final CloneNotSupportedException e) {
			L.error(e);
		}
	}

	private Shape convertObstacle(final IObstacle2D o) {
		final Rectangle2D r = o.getBounds2D();
		final Point2D[] points = new Point2D.Double[] { new Point2D.Double(r.getX(), r.getY()), new Point2D.Double(r.getX() + r.getWidth(), r.getY()), new Point2D.Double(r.getX() + r.getWidth(), r.getY() + r.getHeight()), new Point2D.Double(r.getX(), r.getY() + r.getHeight()) };
		final Path2D path = new GeneralPath();
		for (int i = 0; i < points.length; i++) {
			points[i] = wormhole.getViewPoint(points[i]);
			if (i == 0) {
				path.moveTo(points[i].getX(), points[i].getY());
			}
			path.lineTo(points[i].getX(), points[i].getY());
		}
		path.closePath();
		return path;
	}

	@Override
	public void dispose() {
		removeMouseListener(mouseManager);
		removeMouseMotionListener(mouseManager);
		removeMouseWheelListener(mouseManager);
		removeComponentListener(componentManager);
		removeAll();
		this.setVisible(false);
	}

	/**
	 * This method is meant to be overridden by subclasses that want to display
	 * a more sophisticated background than a simple color.
	 * 
	 * @param g
	 *            the Graphics2D to use
	 */
	protected abstract void drawBackground(final Graphics2D g);

	/**
	 * Actually draws the environment on the view.
	 * 
	 * @param g
	 *            {@link Graphics2D} object responsible for drawing
	 */
	protected void drawEnvOnView(final Graphics2D g) {
		if (wormhole == null || !isVisible() || !isEnabled()) {
			return;
		}

		mutex.acquireUninterruptibly();

		if (hooked.isPresent()) {
			final IPosition hcoor = positions.get(hooked.get());
			final Point2D hp = wormhole.getViewPoint(new Point2D.Double(hcoor.getCoordinate(0), hcoor.getCoordinate(1)));
			if (hp.distance(getCenter()) > FREEDOM_RADIUS) {
				wormhole.setDeltaViewPosition(NSEAlg2DHelper.variation(getCenter(), hp));
			}
		}

		g.setColor(Color.BLACK);
		if (obstacles != null) {
			for (final IObstacle2D o : obstacles) {
//				final Rectangle2D b = o.getBounds2D();
//				final Point2D.Double pt1 = new Point2D.Double(b.getMinX(), b.getMinY());
//				final Point2D.Double pt2 = new Point2D.Double(b.getMinX(), b.getMaxY());
//				final Point2D.Double pt3 = new Point2D.Double(b.getMaxX(), b.getMinY());
//				final Point2D.Double pt4 = new Point2D.Double(b.getMaxX(), b.getMaxY());
//				if (wormhole.isInsideView(pt1) || wormhole.isInsideView(pt2) || wormhole.isInsideView(pt3) || wormhole.isInsideView(pt4)) {
					g.fill(convertObstacle(o));
//				}
			}
		}
		if (paintLinks) {
			g.setColor(Color.GRAY);
			for (final Entry<INode<T>, INeighborhood<T>> entry : neighbors.entrySet()) {
				final IPosition coord = positions.get(entry.getKey());
				final Point2D s = wormhole.getViewPoint(new Point2D.Double(coord.getCoordinate(0), coord.getCoordinate(1)));
				for (final INode<?> n : entry.getValue()) {
					final IPosition coorddest = positions.get(n);
					final Point2D d = wormhole.getViewPoint(new Point2D.Double(coorddest.getCoordinate(0), coorddest.getCoordinate(1)));
					g.drawLine((int) s.getX(), (int) s.getY(), (int) d.getX(), (int) d.getY());
				}
			}
		}
		g.setColor(Color.GREEN);
		if (effectStack != null) {
			for (final Effect effect : effectStack) {
				for (final Entry<INode<T>, IPosition> entry : positions.entrySet()) {
					final IPosition coords = entry.getValue();
					final INode<T> node = entry.getKey();
					final Point2D s = wormhole.getViewPoint(new Point2D.Double(coords.getCoordinate(0), coords.getCoordinate(1)));
					if (wormhole.isInsideView(s)) {
						final double cd = Math.hypot(s.getX() - mousex, s.getY() - mousey);
						if (cd <= dist) {
							nearest = node;
							nearestx = (int) s.getX();
							nearesty = (int) s.getY();
							dist = cd;
						}
						effect.apply(g, node, (int) s.getX(), (int) s.getY());
					}
				}
			}
		}
		if (nearest != null) {
			g.setColor(Color.RED);
			g.fillOval(nearestx - SELECTED_NODE_DRAWING_SIZE / 2, nearesty - SELECTED_NODE_DRAWING_SIZE / 2, SELECTED_NODE_DRAWING_SIZE, SELECTED_NODE_DRAWING_SIZE);
			g.setColor(Color.YELLOW);
			g.fillOval(nearestx - SELECTED_NODE_INTERNAL_SIZE / 2, nearesty - SELECTED_NODE_INTERNAL_SIZE / 2, SELECTED_NODE_INTERNAL_SIZE, SELECTED_NODE_INTERNAL_SIZE);
		}
		mutex.release();
	}

	@Override
	public void finished(final IEnvironment<T> environment, final ITime time, final long step) {
		reset();
	}

	/**
	 * Gets the view center point.
	 * 
	 * @return the center
	 */
	protected Point2D getCenter() {
		return new Point2D.Double(getWidth() / 2d, getHeight() / 2d);
	}

	/**
	 * @return the environment
	 */
	protected IEnvironment<T> getEnv() {
		return env;
	}

	@Override
	public int getStep() {
		return st;
	}
	
	/**
	 * Make an svg representation of a UI element.
	 * 
	 * @param component you want to represent
	 * @return a String containing the screenshot in svg format
	 */
	public String getSVGScreenShot() {		
		SVGGraphics2D svg = new SVGGraphics2D(this.getWidth(), this.getHeight());
		
		this.paint(svg);
		return svg.getSVGDocument();
	}

	/**
	 * Lets child-classes access the wormhole.
	 * 
	 * @return an {@link IWormhole2D}
	 */
	protected IWormhole2D getWormhole() {
		return wormhole;
	}

	/**
	 * Lets child-classes access the zoom manager.
	 * 
	 * @return an {@link IZoomManager}
	 */
	protected IZoomManager getZoomManager() {
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
	protected void initAll(final IReaction<T> r, final ITime time, final long step) {
		final double[] envSize = env.getSize();
		final double[] offset = env.getOffset();
		wormhole = new NSEWormhole(getSize(), new DoubleDimension(envSize), new Point2D.Double(offset[0], offset[1]));
		angleManager = new AngleManager(AngleManager.DEF_DEG_PER_PIXEL);
		zoomManager = new ExpZoomManager(wormhole.getZoom(), ExpZoomManager.DEF_BASE);
		mouseVelocity = new NSEPointerVelocityHandler();
		computeNodes();
		if (env instanceof IEnvironment2DWithObstacles) {
			loadObstacles();
		} else {
			obstacles = null;
		}
		addMouseListener(mouseManager);
		addMouseMotionListener(mouseManager);
		addMouseWheelListener(mouseManager);
		addComponentListener(componentManager);
	}

	@Override
	public void initialized(final IEnvironment<T> environment) {
		stepDone(environment, null, new DoubleTime(), 0);
	}

	/**
	 * Lets child-classes check if the display is initialized.
	 * 
	 * @return a <code>boolean</code> value
	 */
	protected boolean isInitilized() {
		return initialized;
	}

	/**
	 * @return true if this monitor is trying to draw in realtime
	 */
	@Override
	public boolean isRealTime() {
		return realTime;
	}

	/**
	 * Updates the environment obstacles. The environment must be a subclass of
	 * {@link IEnvironment2DWithObstacles}.
	 * 
	 */
	protected void loadObstacles() {
		obstacles = ((IEnvironment2DWithObstacles<?, ?>) env).getObstacles();
	}

	/**
	 * 
	 */
	protected abstract void onFirstResizing();

	/**
	 * Call this method if you want this monitor to be bound to a new
	 * environment.
	 */
	public void reset() {
		firstTime = true;
	}

	/**
	 * 
	 * @param x x coord
	 * @param y y coord
	 */
	protected void setDist(final int x, final int y) {
		mousex = x;
		mousey = y;
		final Point2D envMouse = wormhole.getEnvPoint(new Point2D.Double(mousex, mousey));
		dist = Math.hypot(mousex - nearestx, mousey - nearesty);
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(envMouse.getX());
		sb.append(", ");
		sb.append(envMouse.getY());
		sb.append(']');
		if (nearest != null) {
			sb.append(" -- ");
			sb.append(Res.get(Res.NEAREST_NODE_IS));
			sb.append(": ");
			sb.append(nearest.getId());
		}
		setToolTipText(sb.toString());
	}

	@Override
	public void setDrawLinks(final boolean b) {
		paintLinks = b;
		updateView();
	}

	@Override
	public void setEffectStack(final List<Effect> l) {
		effectStack = l;
	}

	/**
	 * @param environment
	 *            the environment
	 */
	protected void setEnv(final IEnvironment<T> environment) {
		this.env = environment;
	}

	@Override
	public void setRealTime(final boolean rt) {
		realTime = rt;
	}

	@Override
	public void setStep(final int step) {
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
	 *            an {@link IZoomManager}
	 */
	protected void setZoomManager(final IZoomManager zm) {
		zoomManager = zm;
		wormhole.setZoom(zoomManager.getZoom());
	}

	@Override
	public void stepDone(final IEnvironment<T> environment, final IReaction<T> r, final ITime time, final long step) {
		if (firstTime) {
			env = environment;
			mutex.acquireUninterruptibly();
			/*
			 * Thread safety: need to double-check 
			 */
			if (firstTime) {
				initAll(r, time, step);
				lasttime = -TIME_STEP;
				firstTime = false;
				timeInit = System.currentTimeMillis();
				updateView();
			}
			mutex.release();
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
						L.error(e);
					}
				}
			}
			update(time);
		}
	}

	private void update(final ITime time) {
		mutex.acquireUninterruptibly();
		if (envHasMobileObstacles(env)) {
			loadObstacles();
		}
		lasttime = time.toDouble();
		computeNodes();
		mutex.release();
		updateView();
	}

	/**
	 * Actually repaints the view.
	 */
	protected void updateView() {
		repaint();
	}
	
	private class MouseManager implements MouseInputListener, MouseWheelListener, MouseMotionListener {
		
		@Override
		public void mouseClicked(final MouseEvent e) {
			setDist(e.getX(), e.getY());
			if (nearest != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
				final NodeTracker<T> monitor = new NodeTracker<>(nearest, env);
				AlchemistSwingUI.addTab(monitor);
				Simulation.addOutputMonitor(env, monitor);
			}
			if (nearest != null && SwingUtilities.isMiddleMouseButton(e)) {
				hooked = hooked.isPresent() ? Optional.empty() : Optional.of(nearest);
			}
			updateView();
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			setDist(e.getX(), e.getY());
			if (wormhole == null) {
				return;
			}
			if (SwingUtilities.isLeftMouseButton(e)) {
				if (mouseVelocity != null && !hooked.isPresent()) {
					wormhole.setDeltaViewPosition(mouseVelocity.getVariation());
				}
			} else if (SwingUtilities.isRightMouseButton(e) && mouseVelocity != null && angleManager != null && wormhole.getMode() != Mode.MAP) {
				angleManager.inc(mouseVelocity.getVariation().getX());
				wormhole.rotateAroundPoint(getCenter(), angleManager.getAngle());
			}
			mouseVelocity.setCurrentPosition(e.getPoint());
			updateView();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			setDist(e.getX(), e.getY());
			updateView();
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			setDist(e.getX(), e.getY());
			updateView();
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			setDist(e.getX(), e.getY());
			if (mouseVelocity != null) {
				mouseVelocity.setCurrentPosition(e.getPoint());
			}
			updateView();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			/*
			 * Unused
			 */
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			/*
			 * Unused
			 */
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			if (wormhole != null && zoomManager != null) {
				zoomManager.dec(e.getWheelRotation());
				wormhole.zoomOnPoint(e.getPoint(), zoomManager.getZoom());
				setDist(e.getX(), e.getY());
				updateView();
			}
		}

	}
	
	private class ComponentManager implements ComponentListener {
		@Override
		public void componentHidden(final ComponentEvent e) {
			/*
			 * Unused
			 */
		}

		@Override
		public void componentMoved(final ComponentEvent e) {
			/*
			 * Unused
			 */
		}

		@Override
		public void componentResized(final ComponentEvent e) {
			if (wormhole != null) {
				wormhole.setViewSize(getSize());
				if (!initialized) {
					onFirstResizing();
					initialized = true;
				}
			}
			updateView();
		}

		@Override
		public void componentShown(final ComponentEvent e) {
			/*
			 * Unused
			 */
		}
	}
	
}
