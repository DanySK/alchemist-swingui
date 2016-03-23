package it.unibo.alchemist.test;
import org.junit.Test;

import it.unibo.alchemist.boundary.monitors.Generic2DDisplay;
import it.unibo.alchemist.boundary.wormhole.implementation.Wormhole2D;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.interfaces.Environment;

/**
 * Test for bugs in {@link Wormhole2D}.
 */
public class TestWormhole2D {

    /**
     * Ensure that no exception is thrown when a zero-sized environment is
     * requested.
     */
    @Test
    public void testZeroSizeEnvironment() {
        final Environment<Object> env = new Continuous2DEnvironment<>();
        final Wormhole2D worm = new Wormhole2D(env, new Generic2DDisplay<>());
        worm.center();
    }

}
