/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.monitors;


import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.boundary.interfaces.SwingOutputMonitor;
import it.unibo.alchemist.core.implementations.Simulation;
import it.unibo.alchemist.core.interfaces.ISimulation;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.danilopianini.view.ExportForGUI;



/**
 * @author Gianluca Turin
 * 
 * @param <T>
 */
@ExportInspector
public class RecordingMonitor<T> extends JPanel implements OutputMonitor<T>, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8482529678274109355L;
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
	private SwingOutputMonitor<T> source;
	private final Semaphore mutex = new Semaphore(1);
	private PrintStream writer;
	private String fpCache;
	private boolean screenshotRequested;

	
	@ExportForGUI(nameToExport = "File path")
	private String filePath = System.getProperty("user.home") + System.getProperty("file.separator") + sdf.format(new Date()) + "-alchemist_report.svg";
	
	/**
	 * RecordingMonitor<T> empty constructor.
	 */
	public RecordingMonitor() {
		super();
		
		setLayout(new BorderLayout());
		
		JButton cap = new JButton("Capture!");
		cap.addActionListener(this);
		
		add(cap, BorderLayout.CENTER);
	}

	@Override
	public void initialized(IEnvironment<T> env) {
		/*
		try {
			source = (SwingOutputMonitor) Class.forName("it.unibo.alchemist.monitors."+env.getPreferredMonitor()).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			
		}*/
		
		new JFrame(env.getPreferredMonitor()).setVisible(true);
		
		stepDone(env, null, null, 0);		
	}

	@Override
	public void finished(IEnvironment<T> env, ITime time, long step) {
		stepDone(env, null, null, 0);
	}

	/**
	 * @return file path
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * @param fp file path
	 */
	public void setFilePath(final String fp) {
		this.filePath = fp;
	}
	
	/**
	 * Set the source which generate the svg screenshot.
	 * @param source
	 */
	public void setSource(SwingOutputMonitor<T> source) {
		this.source = source;
	}

	@Override
	public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		if (screenshotRequested && (source != null)) {
			mutex.acquireUninterruptibly();
			if (System.identityHashCode(fpCache) != System.identityHashCode(filePath)) {
				fpCache = filePath;
				if (writer != null) {
					writer.close();
				}
				try {
					writer = new PrintStream(new File(fpCache), StandardCharsets.UTF_8.name());
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					L.error(e);
				}
			}
			
			writer.print(source.getSVGScreenShot());
			writer.close();
			screenshotRequested=false;
			mutex.release();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		screenshotRequested=true;
		
	}
	
}