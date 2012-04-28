/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui.propertyeditor;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sourceforge.frcsimulator.internals.FrcBotSimProperty;

/**
 *
 * @author wolf
 */
public class IntegerPropertyEditor extends PropertyEditor<Integer> {
	protected JSpinner n_decSpinner;
        protected JLabel n_hexLabel,n_octLabel,n_binLabel;
	protected FrcBotSimProperty<Integer> property;
	@Override
	public void initialize(String key, FrcBotSimProperty<Integer> iProperty) {
		property = iProperty;
		n_decSpinner = new JSpinner();
                n_decSpinner.setValue(property.get());
                n_hexLabel=new JLabel("HEX: "+Integer.toHexString(property.get()).toUpperCase());
                n_octLabel=new JLabel("OCT: "+Integer.toOctalString(property.get()));
                n_binLabel=new JLabel("BIN: "+Integer.toBinaryString(property.get()));
		n_decSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				property.set((Integer)n_decSpinner.getValue());
                                n_hexLabel.setText("HEX: "+Integer.toHexString((Integer)n_decSpinner.getValue()).toUpperCase());
                                n_octLabel.setText("OCT: "+Integer.toOctalString((Integer)n_decSpinner.getValue()));
                                n_binLabel.setText("BIN: "+Integer.toBinaryString((Integer)n_decSpinner.getValue()));
			}
		});
		add(n_decSpinner);
                add(n_hexLabel);
                add(n_octLabel);
                add(n_binLabel);
	}
}
