/*
 * Copyright © 2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers;

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.PRIVATE;
import static com.sun.tools.javac.code.Flags.PROTECTED;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.SYNCHRONIZED;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

public class JavacMethod {
	private final JavacNode methodNode;
	private final TreeMaker maker;

	private JavacMethod(final JavacNode methodNode) {
		if (!(methodNode.get() instanceof JCMethodDecl)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
		maker = methodNode.getTreeMaker();
	}

	public boolean returns(Class<?> clazz) {
		final String type = typeStringOf(get().restype);
		return type.equals(clazz.getName());
	}

	public boolean isSynchronized() {
		return (get().mods != null) && ((get().mods.flags & SYNCHRONIZED) != 0);
	}

	public boolean isConstructor() {
		return "<init>".equals(methodNode.getName());
	}

	public JCMethodDecl get() {
		return (JCMethodDecl)methodNode.get();
	}

	public JavacNode node() {
		return methodNode;
	}

	public boolean hasNonFinalParameter() {
		for(JCVariableDecl param: get().params) {
			if ((param.mods == null) || (param.mods.flags & FINAL) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isAbstract() {
		return (get().mods.flags & ABSTRACT) != 0;
	}

	public boolean isEmpty() {
		return get().body == null;
	}

	public String name() {
		return node().getName();
	}

	public void body(JCStatement... statements) {
		if (statements != null) {
			body(List.from(statements));
		}
	}

	public void body(List<JCStatement> statements) {
		get().body = maker.Block(0, statements);
		addSuppressWarningsAll(get().mods, node(), get().pos);
	}

	private void addSuppressWarningsAll(JCModifiers mods, JavacNode node, int pos) {
		TreeMaker maker = node.getTreeMaker();
		JCExpression suppressWarningsType = chainDotsString(maker, node, "java.lang.SuppressWarnings");
		JCLiteral allLiteral = maker.Literal("all");
		suppressWarningsType.pos = pos;
		allLiteral.pos = pos;
		JCAnnotation annotation = maker.Annotation(suppressWarningsType, List.<JCExpression>of(allLiteral));
		annotation.pos = pos;
		mods.annotations = mods.annotations.append(annotation);
	}

	public void rebuild() {
		node().rebuild();
	}

	private String typeStringOf(JCExpression type) {
		if (type instanceof JCTypeApply) {
			return ((JCTypeApply)type).clazz.type.toString();
		} else {
			return type.type.toString();
		}
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static JavacMethod methodOf(final JavacNode node) {
		JavacNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof JCMethodDecl)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new JavacMethod(methodNode);
	}

	public void makePrivate() {
		makePackagePrivate();
		get().mods.flags |= PRIVATE;
	}
	
	public void makePackagePrivate() {
		get().mods.flags &= ~(PRIVATE |PROTECTED | PUBLIC);
	}
	
	public void makeProtected() {
		makePackagePrivate();
		get().mods.flags |= PROTECTED;
	}
	
	public void makePublic() {
		makePackagePrivate();
		get().mods.flags |= PUBLIC;
	}
}