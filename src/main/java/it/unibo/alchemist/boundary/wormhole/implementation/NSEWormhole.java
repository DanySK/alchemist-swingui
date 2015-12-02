/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.wormhole.implementation;

import it.unibo.alchemist.boundary.wormhole.interfaces.IWormhole2D;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.utils.L;

import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * <code>NSEWormhole2D</code> = No Side Effects Wormhole2D.<br>
 * Complete implementation for the {@link Wormhole2D} class.
 * 

 */
public class NSEWormhole extends Wormhole2D {

    /**
     * Initializes a new <code>NSEWormhole2D</code> instance directly setting
     * the size of both view and environment, and the offset too.
     * 
     * @param viewSize
     *            is the size of the view
     * @param envSize
     *            is the size of the environment
     * @param offset
     *            is the offset
     * 
     * @see IWormhole2D
     */
    public NSEWormhole(final IEnvironment<?> env, final Component c) {
        super(env, c);
    }


}
