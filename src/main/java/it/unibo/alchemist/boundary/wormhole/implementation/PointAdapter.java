package it.unibo.alchemist.boundary.wormhole.implementation;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.Serializable;

import org.danilopianini.lang.HashUtils;

import it.unibo.alchemist.model.implementations.positions.Continuous2DEuclidean;
import it.unibo.alchemist.model.interfaces.IPosition;

public final class PointAdapter implements Serializable {
    
    private static final long serialVersionUID = 4144646922749713533L;
    private final double x, y;
    private IPosition pos;
    private int hash;
    
    private PointAdapter(final double x, final double y) {
        this.x = x;
        this.y = y;
    }
    
    private PointAdapter(IPosition pos) {
        assert pos.getDimensions() == 2;
        this.pos = pos;
        x = pos.getCoordinate(0);
        y = pos.getCoordinate(1);
    }
    
    private static int approx(final double d) {
        return (int) Math.round(d);
    }
    
    public static PointAdapter from(final double x, final double y) {
        return new PointAdapter(x, y);
    }
    
    public static PointAdapter from(final IPosition p) {
        return new PointAdapter(p);
    }
    
    public static PointAdapter from(final Point2D p) {
        return new PointAdapter(p.getX(), p.getY());
    }
    
    public Point toPoint() {
        return new Point(approx(x), approx(y));
    }
    
    public Point2D toPoint2D() {
        return new Point2D.Double(x, y);
    }

    public IPosition toPosition() {
        if (pos == null) {
            pos = new Continuous2DEuclidean(x, y);
        }
        return pos;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof PointAdapter
                && ((PointAdapter) obj).x == x
                && ((PointAdapter) obj).y == y;
    }
    
    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = HashUtils.hash32(x, y);
        }
        return hash;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public PointAdapter diff(PointAdapter op) {
        return new PointAdapter(x - op.x, y - op.y);
    }
    
    public PointAdapter sum(PointAdapter op) {
        return new PointAdapter(x + op.x, y + op.y);
    }
    
    public static PointAdapter diff(final PointAdapter p1, final PointAdapter p2) {
        return from(p1.x - p2.x, p1.y - p2.y);
    }

    public static PointAdapter sum(PointAdapter p1, PointAdapter p2) {
        return from(p1.x + p2.x, p1.y + p2.y);
    }
    
    @Override
    public String toString() {
        return "[" + x + ", " + y + "]";
    }

}
