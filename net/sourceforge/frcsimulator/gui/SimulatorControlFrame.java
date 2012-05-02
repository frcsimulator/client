/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui;

import frcbotsimtest.FrcBotSimTest;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import net.sourceforge.frcsimulator.gui.propertyeditor.*;
import net.sourceforge.frcsimulator.internals.*;
import net.sourceforge.frcsimulator.mistware.Simulator;

/**
 *
 * @author wolf
 */
public class SimulatorControlFrame extends JFrame {

	protected Simulator simulator;
	protected String midletName;
	// Note: must be anonymous to use in applet.
	protected static final Logger logger = Logger.getAnonymousLogger();//Logger.getLogger(SimulatorControlFrame.class.getName());
	protected static JButton startButton = new JButton("Start simulator");
	protected JScrollBar scrollBar = null;
	protected JMenuBar menuBar;
	protected JMenu fileMenu, fileExamplesMenuItem, helpMenu;
	protected JMenuItem fileQuitMenuItem, helpAboutMenuItem;
	protected JCheckBoxMenuItem fileDebugCheckboxMenuItem;
	protected JTree componentTree;
	protected JTextArea console;
	protected PrintStream consoleStream;
	protected PropertyEditor editor;
	protected JSplitPane propertyPane;
	protected JScrollPane outScroll;
	protected String[][] examples = {{"MIDlet", "net.sourceforge.frcsimulator.test.FRCBotMIDlet"},
		{"RobotBase", "net.sourceforge.frcsimulator.test.FRCBotRobotBase"},
		{"SimpleRobot", "edu.wpi.first.wpilibj.SimpleRobot"},
		{"IterativeRobot", "edu.wpi.first.wpilibj.IterativeRobot"}};
	public SimulatorControlFrame() { // For applet
		this("edu.wpi.first.wpilibj.SimpleRobot");
	}
	public SimulatorControlFrame(String testCase) {
		super("Frc Simulator - " + testCase);
		midletName = testCase;
		logger.addHandler(new GuiHandler());
		//// Property Editors ////
		PropertyEditor.register(Boolean.class, BooleanPropertyEditor.class);
		PropertyEditor.register(Byte.class, BytePropertyEditor.class);
		PropertyEditor.register(Character.class, CharacterPropertyEditor.class);
		PropertyEditor.register(Short.class, ShortPropertyEditor.class);
		PropertyEditor.register(Integer.class, IntegerPropertyEditor.class);
		PropertyEditor.register(Long.class, LongPropertyEditor.class);
		//// Initialize the window ////
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(new Dimension(500, 500));
		setLayout(new BorderLayout());
		add(startButton, BorderLayout.NORTH);
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		fileExamplesMenuItem = new JMenu("Examples");
		for (String[] example : examples) {
			fileExamplesMenuItem.add(new JMenuItem(new ExampleSelectAction(example[0], example[1])));
		}
		fileMenu.add(fileExamplesMenuItem);
		fileDebugCheckboxMenuItem = new JCheckBoxMenuItem("Enable debug messages");
		fileDebugCheckboxMenuItem.setAccelerator(KeyStroke.getKeyStroke('D', InputEvent.CTRL_DOWN_MASK));
		fileDebugCheckboxMenuItem.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				CRIO.getInstance().setDebugging(fileDebugCheckboxMenuItem.isSelected());
			}
		});
		//fileDebugCheckboxMenuItem.setSelected(true);
		fileMenu.add(fileDebugCheckboxMenuItem);
		JMenuItem fileRefreshMenuItem = new JMenuItem("Refresh Components", 'R');
		fileRefreshMenuItem.setAccelerator(KeyStroke.getKeyStroke('R', InputEvent.CTRL_DOWN_MASK));
		fileRefreshMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				refreshProperties();
			}
		});
		fileMenu.add(fileRefreshMenuItem);
		fileQuitMenuItem = new JMenuItem("Quit", 'Q');
		fileQuitMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
		fileQuitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				System.exit(FrcBotSimTest.E_NONE);
			}
		});
		fileMenu.add(fileQuitMenuItem);
		menuBar.add(fileMenu);
		helpMenu = new JMenu("Help");
		helpAboutMenuItem = new JMenuItem("About", 'A');
		helpAboutMenuItem.setAccelerator(KeyStroke.getKeyStroke('I', InputEvent.CTRL_DOWN_MASK));
		final JFrame frame=this;
		helpAboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				//TODO finish the about dialog
				JDialog aboutBox = new JDialog(frame);
				aboutBox.setTitle("About FRC Simulator");
				aboutBox.setLayout(new GridLayout(0,1));
				JLabel titleLabel=new JLabel("FRC Simulator",JLabel.CENTER);
				titleLabel.setFont(new Font(Font.SERIF,Font.BOLD,24));
				JLabel versionLabel=new JLabel("Subversion trunk",JLabel.CENTER);
				versionLabel.setFont(new Font(Font.DIALOG,Font.ITALIC,12));
				aboutBox.add(titleLabel);
				aboutBox.add(versionLabel);
				aboutBox.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				aboutBox.setSize(500,500);
				aboutBox.setVisible(true);
			}
		});
		helpMenu.add(helpAboutMenuItem);
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
		console = new JTextArea();
		console.setEditable(false);
		console.setMargin(new Insets(3, 3, 3, 3));
		outScroll = new JScrollPane(console);
		scrollBar = outScroll.getVerticalScrollBar();
		componentTree = new JTree(new JTree.DynamicUtilTreeNode("Components", SimulatedBot.getSimComponents()));
		componentTree.setMinimumSize(new Dimension(componentTree.getMinimumSize().width, getHeight() / 2));
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent ce) {
				if (outScroll.getWidth() < getWidth() / 2) {
					outScroll.setSize(new Dimension(getWidth() / 2, outScroll.getHeight()));
				}
				outScroll.setMinimumSize(new Dimension(getWidth() / 2, outScroll.getMinimumSize().height));
				if (propertyPane.getRightComponent().getWidth() > getWidth() / 2) {
					propertyPane.getRightComponent().setSize(new Dimension(getWidth() / 2, propertyPane.getHeight()));
				}
				propertyPane.setMaximumSize(new Dimension(getWidth() / 2, propertyPane.getMaximumSize().height));
			}
		});
		componentTree.setRootVisible(false);
		componentTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent tse) {
				if (tse.isAddedPath()) {
					try {
						Object[] path = componentTree.getSelectionPath().getPath();
						int i = 0;
						Object edit = null;
						for (Object pathNode : path) {
							Object node;
							if (DefaultMutableTreeNode.class.isAssignableFrom(pathNode.getClass())) {
								DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)pathNode;
								node = treeNode.getUserObject();
							} else {
								node = pathNode;
							}
							i++;
							if (node == null || edit == null || "Simulator".equals(edit) || FrcBotSimProperty.class.isAssignableFrom(node.getClass())) {
								edit = node;
							} else if (FrcBotSimComponent.class.isAssignableFrom(edit.getClass()) && String.class.isAssignableFrom(node.getClass())) {
								edit = ((FrcBotSimComponent)edit).getSimProperties().get((String)node);
							} else if (FrcBotSimProperty.class.isAssignableFrom(edit.getClass()) && (FrcBotSimProperties.class.isAssignableFrom(((FrcBotSimProperty)edit).get().getClass())) && String.class.isAssignableFrom(node.getClass())) {
								edit = ((FrcBotSimProperties)((FrcBotSimProperty)edit).get()).get((String)node);
							} else if (Integer.class.isAssignableFrom(node.getClass())) {
								edit = new ArrayWrappingProperty<Object>(FrcBotSimProperty.class.cast(edit),Integer.decode(node.toString()));
							} else {
								edit = node;
							}
						}
						if (edit != null && FrcBotSimProperty.class.isAssignableFrom(edit.getClass())) {
							editor = PropertyEditor.getEditor(componentTree.getSelectionPath().getLastPathComponent().toString(),(FrcBotSimProperty)edit);
						} else {
							editor = PropertyEditor.nullPropertyEditor;
						}
					} catch (Exception ex) {
						logger.log(Level.WARNING, "Could not get an editor for the component", ex);
						editor = PropertyEditor.nullPropertyEditor;
					}
				} else {
					editor = PropertyEditor.nullPropertyEditor;
				}
				((JSplitPane) propertyPane.getRightComponent()).setBottomComponent(editor);
				editor.setSize(editor.getMinimumSize());
				//componentTree.setSize(propertyPane.getWidth()-editor.getWidth(),propertyPane.getHeight()-editor.getHeight());
				//propertyPane.getBottomComponent().setSize(propertyPane.getBottomComponent().getMinimumSize());
				//propertyPane.getTopComponent().setSize(propertyPane.getTopComponent().getMaximumSize());plitPane(JSplitPane.HORIZONTAL_SPLIT,
			}
		});
		editor = PropertyEditor.nullPropertyEditor;
		editor.initialize(null, null);
		outScroll.setSize(getWidth() / 2, outScroll.getHeight());
		outScroll.setPreferredSize(new Dimension(getWidth() / 2, outScroll.getHeight()));
		propertyPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outScroll, new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(componentTree), editor));
		/*
		 * propertyPane.add(editor); propertyPane.add(new JScrollPane(componentTree));
		 */
		//propertyPane.getTopComponent().setPreferredSize(propertyPane.getTopComponent().getMaximumSize());
		//propertyPane.setPreferredSize(propertyPane.getMaximumSize());
		add(propertyPane);
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
		for (FrcBotSimComponent component : SimulatedBot.getSimComponents()) {
			DefaultMutableTreeNode branch = new DefaultMutableTreeNode(component);
			recurseNodes(branch, component.getSimProperties());
			/*
			 * for (String key:component.getSimProperties().keySet()) {
			 * DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(key);
			 * branch.add(leaf); }
			 */
			root.add(branch);
		}
		componentTree.setModel(new DefaultTreeModel(root));
	}

	// TODO RecurseNodes is *extremely* repetitive--is there any way to simplify this?
	private void recurseNodes(DefaultMutableTreeNode branch, FrcBotSimComponent component) {
		for (String key : component.getSimProperties().keySet()) {
			DefaultMutableTreeNode branchBranch = new DefaultMutableTreeNode(key);
			branch.add(branchBranch);
			try {
				if (component.getSimProperties().get(key).get().getClass().getName().equals(boolean[].class.getName())) {
					for (int i = 0; i < ((boolean[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(byte[].class.getName())) {
					for (int i = 0; i < ((byte[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(char[].class.getName())) {
					for (int i = 0; i < ((char[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(short[].class.getName())) {
					for (int i = 0; i < ((short[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(int[].class.getName())) {
					for (int i = 0; i < ((int[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(long[].class.getName())) {
					for (int i = 0; i < ((long[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(float[].class.getName())) {
					for (int i = 0; i < ((float[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(double[].class.getName())) {
					for (int i = 0; i < ((double[]) component.getSimProperties().get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (component.getSimProperties().get(key).get().getClass().isArray()) {
					recurseNodes(branchBranch, (Object[]) component.getSimProperties().get(key).get());
				} else if (component.getSimProperties().get(key).get().getClass().getName().equals(FrcBotSimProperties.class.getName())) {
					recurseNodes(branchBranch, (FrcBotSimProperties) component.getSimProperties().get(key).get());
				} else if (FrcBotSimComponent.class.isAssignableFrom(component.getSimProperties().get(key).get().getClass())) {
					recurseNodes(branchBranch, (FrcBotSimComponent) component.getSimProperties().get(key).get());
				}
			} catch (NullPointerException npe) {
			}
		}
	}

	private void recurseNodes(DefaultMutableTreeNode branch, FrcBotSimProperties properties) {
		for (String key : properties.keySet()) {
			DefaultMutableTreeNode branchBranch = new DefaultMutableTreeNode(key);
			branch.add(branchBranch);
			try {
				if (properties.get(key).get().getClass().getName().equals(boolean[].class.getName())) {
					for (int i = 0; i < ((boolean[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(byte[].class.getName())) {
					for (int i = 0; i < ((byte[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(char[].class.getName())) {
					for (int i = 0; i < ((char[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(short[].class.getName())) {
					for (int i = 0; i < ((short[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(int[].class.getName())) {
					for (int i = 0; i < ((int[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(long[].class.getName())) {
					for (int i = 0; i < ((long[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(float[].class.getName())) {
					for (int i = 0; i < ((float[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().getName().equals(double[].class.getName())) {
					for (int i = 0; i < ((double[]) properties.get(key).get()).length; i++) {
						branchBranch.add(new DefaultMutableTreeNode(i));
					}
				} else if (properties.get(key).get().getClass().isArray()) {
					recurseNodes(branchBranch, (Object[]) properties.get(key).get());
				} else if (properties.get(key).get().getClass().getName().equals(FrcBotSimProperties.class.getName())) {
					recurseNodes(branchBranch, (FrcBotSimProperties) properties.get(key).get());
				} else if (FrcBotSimComponent.class.isAssignableFrom(properties.get(key).get().getClass())) {
					recurseNodes(branchBranch, (FrcBotSimComponent) properties.get(key).get());
				}
			} catch (NullPointerException npe) {
			}
		}
	}

	private void recurseNodes(DefaultMutableTreeNode branch, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			DefaultMutableTreeNode branchBranch = new DefaultMutableTreeNode(array[i]);
			branch.add(branchBranch);
			try {
				if (array[i].getClass().getName().equals(boolean[].class.getName())) {
					for (int j = 0; j < ((boolean[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(byte[].class.getName())) {
					for (int j = 0; j < ((byte[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(char[].class.getName())) {
					for (int j = 0; j < ((char[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(short[].class.getName())) {
					for (int j = 0; j < ((short[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(int[].class.getName())) {
					for (int j = 0; j < ((int[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(long[].class.getName())) {
					for (int j = 0; j < ((long[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(float[].class.getName())) {
					for (int j = 0; j < ((float[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().getName().equals(double[].class.getName())) {
					for (int j = 0; j < ((double[]) array[i]).length; j++) {
						branchBranch.add(new DefaultMutableTreeNode(j));
					}
				} else if (array[i].getClass().isArray()) {
					recurseNodes(branchBranch, (Object[]) array[i]);
				} else if (array[i].getClass().getName().equals(FrcBotSimProperties.class.getName())) {
					recurseNodes(branchBranch, (FrcBotSimProperties) array[i]);
				} else if (FrcBotSimComponent.class.isAssignableFrom(array[i].getClass())) {
					recurseNodes(branchBranch, (FrcBotSimComponent) array[i]);
				}
			} catch (NullPointerException npe) {
			}
		}
	}

	public void initSimulator(String action) {
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
		startButton.setText("Simulator status: " + status.toString());
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
			scrollBar.setValue(scrollBar.getMaximum() + scrollBar.getVisibleAmount());
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
				setTitle("Frc Simulator - " + midletName);
			} else {
				logger.warning("Simulator already running; cannot set class.");
			}
		}
	}
}
