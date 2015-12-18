/**
 * 
 */
package it.unibo.alchemist.boundary.l10n;

import java.util.ResourceBundle;

/**
 * Shorthand for getting resources.
 */
public final class R {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("it.unibo.alchemist.l10n.UIStrings");

    private R() {
    }

    /**
     * Looks up on the property files and returns the correct String.
     * 
     * @param key
     *            the key
     * @return the String
     */
    public static String getString(final String key) {
        return RESOURCE_BUNDLE.getString(key);
    }

}
