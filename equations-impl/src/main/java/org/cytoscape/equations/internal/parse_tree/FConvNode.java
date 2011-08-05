/*
  File: FConvNode.java

  Copyright (c) 2010, The Cytoscape Consortium (www.cytoscape.org)

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.equations.internal.parse_tree;


import java.util.Stack;

import org.cytoscape.equations.CodeAndSourceLocation;
import org.cytoscape.equations.Node;
import org.cytoscape.equations.internal.interpreter.Instruction;


/**
 *  A node in the parse tree representing a conversion to a floating point number
 */
public class FConvNode extends Node {
	private final Node convertee;

	public FConvNode(final Node convertee) {
		super(-1); // Type conversions are generated by the compiler and do not correspond to actual source locations!

		if (convertee == null)
			throw new IllegalArgumentException("convertee must not be null!");

		final Class type = convertee.getType();
		if (type != Long.class && type != Boolean.class && type != String.class)
			throw new IllegalArgumentException("convertee must be of type Long, Boolean or String!");

		this.convertee = convertee;
	}

	public String toString() {
		return "FConvNode: convertee = " + convertee;
	}

	public Class getType() { return Double.class; }

	/**
	 *  @return the only child of this node
	 */
	public Node getLeftChild() { return convertee; }

	/**
	 *  @return null, This type of node never has any right children!
	 */
	public Node getRightChild() { return null; }

	public void genCode(final Stack<CodeAndSourceLocation> codeStack) {
		convertee.genCode(codeStack);

		final Class type = convertee.getType();
		if (type == Long.class)
			codeStack.push(new CodeAndSourceLocation(Instruction.FCONVI, getSourceLocation()));
		else if (type == Boolean.class)
			codeStack.push(new CodeAndSourceLocation(Instruction.FCONVB, getSourceLocation()));
		else if (type == String.class)
			codeStack.push(new CodeAndSourceLocation(Instruction.FCONVS, getSourceLocation()));
		else
			throw new IllegalStateException("unknown type: " + type + "!");
	}
}
