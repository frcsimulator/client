/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui.propertyeditor;

import java.util.HashMap;
import javax.swing.JPanel;
import net.sourceforge.frcsimulator.internals.FrcBotSimProperty;

/**
 *
 * @author wolf
 */
public abstract class PropertyEditor extends JPanel {
	public static final PropertyEditor nullPropertyEditor = new NullPropertyEditor();
	public static final HashMap<Class<?>,Class<? extends PropertyEditor>> editors = new HashMap();
	public abstract void initialize(String key, FrcBotSimProperty property);
	public static final void register(Class<?> propertyClass, Class<? extends PropertyEditor> editor) {
		editors.put(propertyClass, editor);
	}
	public static final PropertyEditor getEditor(String key, FrcBotSimProperty property) throws InstantiationException, IllegalAccessException {
		Class<? extends PropertyEditor> editorClass = editors.get(property.get().getClass());
		PropertyEditor editor;
		if (editorClass == null) {
			editor = new UnknownPropertyEditor();
		} else {
			editor = editorClass.newInstance();
		}
		editor.initialize(key, property);
		return editor;
	}
}
