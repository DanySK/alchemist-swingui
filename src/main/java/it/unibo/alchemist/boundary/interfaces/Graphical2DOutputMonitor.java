package it.unibo.alchemist.boundary.interfaces;

import it.unibo.alchemist.model.interfaces.IPosition;

public interface Graphical2DOutputMonitor<T> extends GraphicalOutputMonitor<T> {

    void zoomTo(IPosition center, double zoomLevel);

}
