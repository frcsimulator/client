/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui;

import net.sourceforge.frcsimulator.gui.propertyeditor.PropertyEditor;
import frcbotsimtest.FrcBotSimTest;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.sourceforge.frcsimulator.gui.propertyeditor.BooleanPropertyEditor;
import net.sourceforge.frcsimulator.internals.CRIO;
import net.sourceforge.frcsimulator.internals.FrcBotSimComponent;
import net.sourceforge.frcsimulator.internals.FrcBotSimProperty;
import net.sourceforge.frcsimulator.internals.SimulatedBot;
import net.sourceforge.frcsimulator.mistware.Simulator;

/**
 *
 * @author wolf
 */
public class SimulatorControlFrame extends JFrame {
	protected Simulator simulator;
	protected String midletName;
	protected static final Logger logger = Logger.getLogger(SimulatorControlFrame.class.getName());
	protected static JButton startButton = new JButton("Start simulator");
	protected JMenuBar menuBar;
	protected JMenu fileMenu, fileExamplesMenuItem, helpMenu;
	protected JMenuItem fileQuitMenuItem, helpAboutMenuItem;
	protected JCheckBoxMenuItem fileDebugCheckboxMenuItem;
	protected JTree componentTree;
	protected JTextArea console;
	protected PrintStream consoleStream;
	protected PropertyEditor editor;
	protected JSplitPane propertyPane;
	protected String[][] examples = {{"MIDlet","net.sourceforge.frcsimulator.test.FRCBotMIDlet"},
			{"RobotBase","net.sourceforge.frcsimulator.test.FRCBotRobotBase"},
			{"SimpleRobot","edu.wpi.first.wpilibj.SimpleRobot"},
			{"IterativeRobot","edu.wpi.first.wpilibj.IterativeRobot"}};
	public SimulatorControlFrame(String testCase) {
		super("Frc Simulator - "+testCase);
		midletName = testCase;
		Logger.getLogger(SimulatorControlFrame.class.getName()).addHandler(new GuiHandler());
		//// Property Editors ////
		PropertyEditor.register(Boolean.class, BooleanPropertyEditor.class);
		//// Initialize the window ////
		setLayout(new BorderLayout());
		setSize(new Dimension(500,500));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		add(startButton,BorderLayout.NORTH);
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		fileExamplesMenuItem = new JMenu("Examples");
		for (String[] example:examples) {
			fileExamplesMenuItem.add(new JMenuItem(new ExampleSelectAction(example[0],example[1])));
		}
		fileMenu.add(fileExamplesMenuItem);
		fileDebugCheckboxMenuItem = new JCheckBoxMenuItem("Enable debug messages");
		fileDebugCheckboxMenuItem.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				CRIO.getInstance().setDebugging(fileDebugCheckboxMenuItem.isSelected());
			}
		});
		fileDebugCheckboxMenuItem.setSelected(true);
		fileMenu.add(fileDebugCheckboxMenuItem);
		JMenuItem fileRefreshMenuItem = new JMenuItem("Refresh Components",'R');
		fileRefreshMenuItem.setAccelerator(KeyStroke.getKeyStroke('R', InputEvent.CTRL_DOWN_MASK));
		fileRefreshMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				refreshProperties();
			}
		});
		fileMenu.add(fileRefreshMenuItem);
		fileQuitMenuItem = new JMenuItem("Quit",'Q');
		fileQuitMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
		fileQuitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				System.exit(FrcBotSimTest.E_NONE);
			}
		});
		fileMenu.add(fileQuitMenuItem);
		menuBar.add(fileMenu);
		helpMenu=new JMenu("Help");
		helpAboutMenuItem=new JMenuItem("About",'A');
		helpAboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				JDialog aboutBox = new JDialog();
				aboutBox.setTitle("About FRC Simulator");
				aboutBox.add(new JLabel("FRC Simulator"));
				aboutBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				aboutBox.setVisible(true);
			}
		});
		helpMenu.add(helpAboutMenuItem);
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
		console = new JTextArea();
		console.setEditable(false);
		console.setMargin(new Insets(3,3,3,3));
		JScrollPane outScroll = new JScrollPane(console);
		outScroll.setMinimumSize(new Dimension(getMinimumSize().width,(int)(getHeight()*.4)));
		componentTree = new JTree(new JTree.DynamicUtilTreeNode("Components",SimulatedBot.getSimComponents()));
		componentTree.setRootVisible(false);
		componentTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent tse) {
				if (tse.isAddedPath()) {
					Object[] path = componentTree.getSelectionPath().getPath();
					if (path.length == 3) {
						try {
							String key = path[2].toString();
							FrcBotSimComponent component = FrcBotSimComponent.class.cast(DefaultMutableTreeNode.class.cast(path[1]).getUserObject());
							editor = PropertyEditor.getEditor(key, component.getSimProperties().get(key));
						} catch (Exception ex) {
							logger.log(Level.WARNING, "Could not get an editor for the component", ex);
						}
					}
				} else {
					editor = PropertyEditor.nullPropertyEditor;
				}
				propertyPane.setBottomComponent(editor);
			}
		});
		editor = PropertyEditor.nullPropertyEditor;
		editor.initialize(null, null);
		propertyPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(componentTree),editor);
		add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,outScroll,
					propertyPane));
		consoleStream = new PrintStream(new TextAreaStream(console));
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				initSimulator(midletName);
				simulator.start();
				System.setOut(consoleStream);
				System.setErr(consoleStream);
				fileExamplesMenuItem.setEnabled(false);
				startButton.setEnabled(false);
			}
		});
		setVisible(true);
	}

	private void refreshProperties() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Simulator");
		for (FrcBotSimComponent component:SimulatedBot.getSimComponents()) {
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode(component);
			for (String key:component.getSimProperties().keySet()) {
				DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(key);
				branch.add(leaf);
			}
			root.add(branch);
		}
		componentTree.setModel(new DefaultTreeModel(root));
	}

	public void initSimulator (String action) {
		//// Initialize the simulator ////
		try {
			simulator = new Simulator(action);
		} catch (ClassNotFoundException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ClassCastException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		simulator.getLogger().addHandler(new GuiHandler());
		try {
			simulator.onStatusChange(SimulatorControlFrame.class.getMethod("simStateChange", Simulator.Status.class, Simulator.Status.class));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Couldn't add a state change handler to the simulator", e);
		}
	}

	public static void simStateChange(Simulator.Status status, Simulator.Status oldStatus) {
		startButton.setText("Simulator status: "+status.toString());
		if (status.equals(Simulator.Status.RUNNING)) {
			startButton.setBackground(Color.GREEN);
		} else if (status.equals(Simulator.Status.INITIALIZING)) {
			startButton.setBackground(Color.YELLOW);
		} else if (status.equals(Simulator.Status.ERROR)) {
			startButton.setBackground(Color.RED);
		} else {
			startButton.setBackground(null);
		}
	}
	public class TextAreaStream extends OutputStream {
		protected JTextArea area;
		public TextAreaStream(JTextArea textArea) {
			area = textArea;
		}
		@Override
		public void write(int i) throws IOException {
			area.append(new String(Character.toChars(i)));
		}
	}
	private class ExampleSelectAction extends AbstractAction {
		protected String midlet;
		public ExampleSelectAction(String title, String midletClass) {
			super(title);
			midlet = midletClass;
		}
		@Override
		public void actionPerformed(ActionEvent ae) {
			if (simulator == null) {
				midletName = midlet;
				setTitle("Frc Simulator - "+midletName);
			} else {
				logger.warning("Simulator already running; cannot set class.");
			}
		}
	}
}
