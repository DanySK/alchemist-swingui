/**
 * 
 */
package it.unibo.alchemist.boundary.interfaces;

import java.awt.Component;

/**
 * @author Gianluca Turin
 *
 * @param T
 * 
 * Add to the GraphicalOutputMonitor<T>'s methods a paint method
 * to make implementing classes able to take a screenshot of them
 * 
 */
public interface SwingOutputMonitor<T> extends GraphicalOutputMonitor<T> {
	
	/**
	 * Make an svg representation of a UI element.
	 * 
	 * @param component you want to represent
	 * @return a String containing the screenshot in svg format
	 */
	String getSVGScreenShot();
}
