/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.frcsimulator.gui;

import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import net.sourceforge.frcsimulator.internals.FrcBotSimComponent;

/**
 *
 * @author wolf
 */
class ComponentTreeLeaf implements TreeNode {
	protected ComponentTreeRoot parent;
	public ComponentTreeLeaf(ComponentTreeRoot parent, FrcBotSimComponent get) {
		this.parent = parent;
	}

	@Override
	public TreeNode getChildAt(int i) {
		return null; // This is a leaf
	}

	@Override
	public int getChildCount() {
		return 0; // This is a leaf
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode tn) {
		return -1; // This is a leaf
	}

	@Override
	public boolean getAllowsChildren() {
		return false;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public Enumeration children() {
		return null; // This is a leaf
	}

}
