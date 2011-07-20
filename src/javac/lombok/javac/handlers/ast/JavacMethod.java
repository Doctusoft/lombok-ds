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
package lombok.javac.handlers.ast;

import static com.sun.tools.javac.code.Flags.*;
import static lombok.ast.AST.*;
import static lombok.core.util.Lists.list;
import static lombok.core.util.Names.capitalize;
import static lombok.javac.handlers.Javac.addSuppressWarningsAll;

import java.util.List;

import lombok.ast.IMethod;
import lombok.ast.TypeRef;
import lombok.javac.JavacNode;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public class JavacMethod implements IMethod<JavacType, JavacNode, JCTree, JCMethodDecl> {
	private final JavacNode methodNode;
	private final JCTree source;
	private final JavacASTMaker builder;

	private JavacMethod(final JavacNode methodNode, final JCTree source) {
		if (!(methodNode.get() instanceof JCMethodDecl)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
		this.source = source;
		builder = new JavacASTMaker(methodNode, source);
	}

	public <T extends JCTree> T build(lombok.ast.Node node) {
		return builder.<T>build(node);
	}

	public <T extends JCTree> T build(lombok.ast.Node node, Class<T> extectedType) {
		return builder.build(node,extectedType);
	}

	public <T extends JCTree> List<T> build(List<? extends lombok.ast.Node> nodes) {
		return builder.build(nodes);
	}

	public <T extends JCTree> List<T> build(List<? extends lombok.ast.Node> nodes, Class<T> extectedType) {
		return builder.build(nodes, extectedType);
	}

	public TypeRef returns() {
		if (isConstructor()) return null;
		return Type(returnType());
	}

	public TypeRef boxedReturns() {
		if (isConstructor()) return null;
		JCExpression type = returnType();
		lombok.ast.TypeRef objectReturnType = Type(type);
		if (type instanceof JCPrimitiveTypeTree) {
			final String name = type.toString();
			if ("int".equals(name)) {
				objectReturnType = Type("java.lang.Integer");
			} else if ("char".equals(name)) {
				objectReturnType = Type("java.lang.Character");
			} else {
				objectReturnType = Type("java.lang." + capitalize(name));
			}
		}
		return objectReturnType;
	}

	public boolean returns(Class<?> clazz) {
		return returns(clazz.getName());
	}

	public boolean returns(final String typeName) {
		final JCExpression returnType = returnType();
		if (returnType == null) return false;
		final String type;
		if (returnType instanceof JCTypeApply) {
			type = ((JCTypeApply)returnType).clazz.type.toString();
		} else {
			type = returnType.type.toString();
		}
		return type.equals(typeName);
	}

	private JCExpression returnType() {
		if (isConstructor()) return null;
		return get().restype;
	}

	public boolean isSynchronized() {
		return (get().mods != null) && ((get().mods.flags & SYNCHRONIZED) != 0);
	}

	public boolean isStatic() {
		return (get().mods.flags & STATIC) != 0;
	}

	public boolean isConstructor() {
		return "<init>".equals(methodNode.getName());
	}

	public boolean isAbstract() {
		return (get().mods.flags & ABSTRACT) != 0;
	}

	public boolean isEmpty() {
		return get().body == null;
	}

	public JCMethodDecl get() {
		return (JCMethodDecl)methodNode.get();
	}

	public JavacNode node() {
		return methodNode;
	}

	public boolean hasNonFinalArgument() {
		for(JCVariableDecl param: get().params) {
			if ((param.mods == null) || (param.mods.flags & FINAL) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasArguments() {
		return !get().params.isEmpty();
	}

	public String name() {
		return node().getName();
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

	public void body(lombok.ast.Statement... statements) {
		body(list(statements));
	}

	public void body(java.util.List<lombok.ast.Statement> statements) {
		body(Block().withStatements(statements));
	}

	public void body(final lombok.ast.Block body) {
		get().body = builder.build(body);
		addSuppressWarningsAll(get().mods, node(), get().pos);
	}

	public void rebuild() {
		node().rebuild();
	}

	public JavacType surroundingType() {
		return JavacType.typeOf(node(), source);
	}

	public java.util.List<lombok.ast.Statement> statements() {
		final java.util.List<lombok.ast.Statement> methodStatements = new java.util.ArrayList<lombok.ast.Statement>();
		for (Object statement : get().body.stats) {
			methodStatements.add(Stat(statement));
		}
		return methodStatements;
	}

	public java.util.List<lombok.ast.Annotation> annotations() {
		final java.util.List<lombok.ast.Annotation> annotations = new java.util.ArrayList<lombok.ast.Annotation>();
		for (JCAnnotation annotation : get().mods.annotations) {
			lombok.ast.Annotation ann = Annotation(Type(annotation.annotationType));
			for (JCExpression arg : annotation.args) {
				if (arg instanceof JCAssign) {
					JCAssign assign = (JCAssign) arg;
					ann.withValue(assign.lhs.toString(), Expr(assign.rhs));
				} else {
					ann.withValue(Expr(arg));
				}
			}
			annotations.add(ann);
		}
		return annotations;
	}

	public java.util.List<lombok.ast.Argument> arguments() {
		final java.util.List<lombok.ast.Argument> methodArguments = new java.util.ArrayList<lombok.ast.Argument>();
		for (JCVariableDecl param : get().params) {
			methodArguments.add(Arg(Type(param.vartype), param.name.toString()));
		}
		return methodArguments;
	}

	public java.util.List<lombok.ast.TypeRef> thrownExceptions() {
		final java.util.List<lombok.ast.TypeRef> thrownExceptions = new java.util.ArrayList<lombok.ast.TypeRef>();
		for (Object thrownException : get().thrown) {
			thrownExceptions.add(Type(thrownException));
		}
		return thrownExceptions;
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static JavacMethod methodOf(final JavacNode node, final JCTree source) {
		JavacNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof JCMethodDecl)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new JavacMethod(methodNode, source);
	}
}
