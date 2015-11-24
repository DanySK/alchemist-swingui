/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.gui;

import it.unibo.alchemist.boundary.gui.logging.LoggerPerspective;
import it.unibo.alchemist.boundary.l10n.Res;
import it.unibo.alchemist.utils.L;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.danilopianini.view.GUIUtilities;

/**
 */
public class FileMenu extends AbstractMenu {

    private static final long serialVersionUID = 5209455686362711386L;
    private static final JMenuItem[] ITEMS = { new JMenuItem(Res.get(Res.QUIT)), new JMenuItem(Res.get(Res.LOAD_JAR)), new JMenuItem("Logging Perspective") };
    private static int n = 1;

    /**
     * Builds the File menu.
     */
    public FileMenu() {
        super(Res.get(Res.FILE), ITEMS);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(ITEMS[0])) {
            System.exit(0);
        } else if (e.getSource().equals(ITEMS[1])) {
            final JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(final File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                }

                @Override
                public String getDescription() {
                    return Res.get(Res.JAR_FILE);
                }
            });
            final int response = fc.showOpenDialog(null);
            if (response == JFileChooser.APPROVE_OPTION) {
                final File chosen = fc.getSelectedFile();
                Method method;
                try {
                    /*
                     * This horrible hack won't work if a SecurityManager is
                     * attached.
                     */
                    method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
                    method.setAccessible(true);
                    method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { chosen.toURI().toURL() });
                    GUIUtilities.alertMessage(Res.get(Res.LOAD_JAR), chosen + " " + Res.get(Res.JAR_LOAD_SUCCESSFULL));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e1) {
                    GUIUtilities.errorMessage(e1);
                    L.error(e1);
                }
            }
        } else if (e.getSource().equals(ITEMS[2])) {
            final StringBuilder sb = new StringBuilder("Logging Perspective");
            if (n > 1) {
                sb.append(' ');
                sb.append(n);
            }
            n++;
            final String name = sb.toString();
            AlchemistSwingUI.addTab(new LoggerPerspective(), name, name);
        }
    }

}
