/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import java.awt.geom.Point2D;

/**
 * Utility class for 2D-vector operation.<br>
 * NSE = No Side Effects.
 * 

 */
public final class NSEAlg2DHelper {

    /**
     * 2*PI = {@value #PI2} i.e. a complete round.
     */
    public static final double PI2 = Math.PI * 2.0;

    /**
     * Negates a vector.
     * 
     * @param p
     *            is the vector
     * @return a new {@link Point2D} object: (-p) == (-p.x; -p.y)
     */
    public static Point2D negation(final Point2D p) {
        return new Point2D.Double(-p.getX(), -p.getY());
    }

    /**
     * Calculates the product of a vector and a scalar value.
     * 
     * @param a
     *            is the scalar value
     * @param p
     *            is the vector
     * @return a new {@link Point2D} object: a*p == (a*p.x; a*p.y)
     */
    public static Point2D product(final double a, final Point2D p) {
        return new Point2D.Double(a * p.getX(), a * p.getY());
    }

    /**
     * Calculates the scalar product of two vectors.
     * 
     * @param p1
     *            is the first vector
     * @param p2
     *            is the second vector
     * @return a new {@link Point2D} object: p1 * p2 == p1.x*p2.x + p1.y*p2.y
     */
    public static double product(final Point2D p1, final Point2D p2) {
        return p1.getX() * p2.getX() + p2.getY() * p2.getY();
    }

    /**
     * Subtracts a vector to another.
     * 
     * @param p1
     *            is the first vector
     * @param p2
     *            is the second vector
     * @return a new {@link Point2D} object: p1 + (-p2)
     */
    public static Point2D subtract(final Point2D p1, final Point2D p2) {
        return new Point2D.Double(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    /**
     * Sums two vectors.
     * 
     * @param p1
     *            is the first vector
     * @param p2
     *            is the second vector
     * @return a new {@link Point2D} object: p1 + p2
     */
    public static Point2D sum(final Point2D p1, final Point2D p2) {
        return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    /**
     * Gets the vector between two points.
     * 
     * @param end
     *            is the end point
     * @param start
     *            is the start point
     * @return a new {@link Point2D} representing the vector (end - start)
     */
    public static Point2D variation(final Point2D end, final Point2D start) {
        return new Point2D.Double(end.getX() - start.getX(), end.getY() - start.getY());
    }

    private NSEAlg2DHelper() {

    }
}
