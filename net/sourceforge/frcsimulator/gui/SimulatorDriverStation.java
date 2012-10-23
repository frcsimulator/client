package net.sourceforge.frcsimulator.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import net.sourceforge.frcsimulator.internals.CRIO;
import net.sourceforge.frcsimulator.internals.SimulatedBot;

/**
 * A Clone of the DriverStation used in the competition.
 * @author benjamin
 */
public class SimulatorDriverStation  extends JFrame{
    private JRadioButton autonomous   = new JRadioButton("Autonomous");
    private JRadioButton teleoperated = new JRadioButton("Teleoperated");
    private JRadioButton disabled     = new JRadioButton("Disabled");
    private ButtonGroup status = new ButtonGroup();
    public SimulatorDriverStation(String testcase){
	super("FRC Simulator DriverStation - " + testcase);
	status.add(autonomous);
	status.add(teleoperated);
	status.add(disabled);
	disabled.setSelected(true);
	autonomous.addActionListener(new ActionListener(){

	    @Override
	    public void actionPerformed(ActionEvent e) {
	    }
	    
	});
    }
}
