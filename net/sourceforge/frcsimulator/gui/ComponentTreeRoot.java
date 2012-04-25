/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui;

import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import net.sourceforge.frcsimulator.internals.FrcBotSimComponent;
import net.sourceforge.frcsimulator.internals.SimulatedBot;

/**
 *
 * @author wolf
 */
public class ComponentTreeRoot implements TreeNode {
	@Override
	public TreeNode getChildAt(int i) {
		return new ComponentTreeLeaf(this,SimulatedBot.getSimComponents().get(i));
	}
	@Override
	public int getChildCount() {
		return SimulatedBot.getSimComponents().size();
	}
	@Override
	public TreeNode getParent() {
		return null; // This is the root
	}
	@Override
	public int getIndex(TreeNode tn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	@Override
	public boolean getAllowsChildren() {
		return true;
	}
	@Override
	public boolean isLeaf() {
		return false;
	}
	@Override
	public Enumeration children() {
		return new PropertyEnumeration(this);
	}
	protected class PropertyEnumeration implements Enumeration {
		protected int index;
		protected ComponentTreeRoot parent;
		protected FrcBotSimComponent nextComponent;
		private PropertyEnumeration(ComponentTreeRoot parent) {
			this.parent = parent;
			nextComponent = SimulatedBot.getSimComponents().get(index);
		}
		@Override
		public boolean hasMoreElements() {
			return nextComponent == null;
		}

		@Override
		public ComponentTreeLeaf nextElement() {
			FrcBotSimComponent ret = nextComponent;
			index++;
			nextComponent = SimulatedBot.getSimComponents().get(index);
			return new ComponentTreeLeaf(parent,ret);
		}

	}
}
