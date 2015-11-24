/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui;

import it.unibo.alchemist.boundary.l10n.Res;

/**
 */
public enum SimControlCommand {

    /**
     * 
     */
    PLAY("/oxygen/actions/media-playback-start.png", r(Res.PLAY)), STEP("/oxygen/actions/media-skip-forward.png", r(Res.STEP)), PAUSE("/oxygen/actions/media-playback-pause.png", r(Res.PAUSE)), STOP("/oxygen/actions/media-playback-stop.png", r(Res.STOP));

    private final String icon, tt;

    private static String r(final Res res) {
        return Res.get(res);
    }

    SimControlCommand(final String iconPath, final String tooltip) {
        icon = iconPath;
        tt = tooltip;
    }

    /**
     * @return a new {@link SimControlButton} for this enum
     */
    public SimControlButton createButton() {
        return new SimControlButton(icon, this, tt);
    }

    /**
     * Compares this enum to a String.
     * 
     * @param s
     *            the String
     * @return true if the String representation of this enum is equal to the
     *         String
     */
    public boolean equalsToString(final String s) {
        return toString().equals(s);
    }

}
