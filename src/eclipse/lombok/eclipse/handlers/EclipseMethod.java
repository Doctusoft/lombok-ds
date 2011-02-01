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
package lombok.eclipse.handlers;

import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.annotation;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

import lombok.core.util.Arrays;
import lombok.eclipse.EclipseNode;

public class EclipseMethod {
	private final EclipseNode methodNode;

	private EclipseMethod(final EclipseNode methodNode) {
		if (!(methodNode.get() instanceof AbstractMethodDeclaration)) {
			throw new IllegalArgumentException();
		}
		this.methodNode = methodNode;
	}

	public boolean returns(Class<?> clazz) {
		if (isConstructor()) return false;
		MethodDeclaration methodDecl = (MethodDeclaration)get();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (char[] elem : methodDecl.returnType.getTypeName()) {
			if (first) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		String type = sb.toString();
		return type.endsWith(clazz.getSimpleName());
	}

	public boolean isSynchronized() {
		return !isConstructor() && (get().bits & ASTNode.IsSynchronized) != 0;
	}

	public boolean isConstructor() {
		return get() instanceof ConstructorDeclaration;
	}

	public AbstractMethodDeclaration get() {
		return (AbstractMethodDeclaration)methodNode.get();
	}

	public EclipseNode node() {
		return methodNode;
	}

	public String name() {
		return new String(get().selector);
	}

	public boolean hasNonFinalParameter() {
		if (get().arguments != null) for (Argument arg : get().arguments) {
			if ((arg.modifiers & ClassFileConstants.AccFinal) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isAbstract() {
		return get().isAbstract();
	}

	public boolean isEmpty() {
		return Arrays.isEmpty(get().statements);
	}

	public void body(Statement... statements) {
		get().statements = statements;
		get().annotations = createSuppressWarningsAll(get(), get().annotations);
	}

	private Annotation[] createSuppressWarningsAll(ASTNode source, Annotation[] originalAnnotationArray) {
		Annotation ann = annotation(source, "java.lang.SuppressWarnings", "all");
		if (originalAnnotationArray == null) return array(ann);
		Annotation[] newAnnotationArray = resize(originalAnnotationArray, 1);
		newAnnotationArray[originalAnnotationArray.length] = ann;
		return newAnnotationArray;
	}

	public void rebuild() {
		node().rebuild();
	}

	@Override
	public String toString() {
		return get().toString();
	}

	public static EclipseMethod methodOf(final EclipseNode node) {
		EclipseNode methodNode = node;
		while ((methodNode != null) && !(methodNode.get() instanceof AbstractMethodDeclaration)) {
			methodNode = methodNode.up();
		}
		return methodNode == null ? null : new EclipseMethod(methodNode);
	}
}
