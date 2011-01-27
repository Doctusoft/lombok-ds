/*
 * Copyright ©  2011 Philipp Eichhorn
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

import static lombok.eclipse.handlers.Eclipse.*;
import static lombok.javac.handlers.Javac.isConstructor;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.camelCase;

import java.util.Iterator;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

import lombok.Yield;
import lombok.eclipse.EclipseASTAdapter;
import lombok.eclipse.EclipseASTVisitor;
import lombok.eclipse.EclipseNode;

@ProviderFor(EclipseASTVisitor.class)
public class HandleYield extends EclipseASTAdapter {
	private boolean handled;
	private String methodName;
	
	@Override public void visitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		handled = false;
	}
	
	@Override public void visitStatement(EclipseNode statementNode, Statement statement) {
		if (statement instanceof MessageSend) {
			MessageSend methodCall = (MessageSend) statement;
			methodName = new String(methodCall.selector);
			if (isMethodCallValid(statementNode, methodName, Yield.class, "yield")) {
				try {
					EclipseNode methodNode = methodNodeOf(statementNode);
					if (isConstructor(methodNode)) {
						methodNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
					} else {
						handled = handle(methodNode);
					}
				} catch (IllegalArgumentException e) {
					statementNode.addError(canBeUsedInBodyOfMethodsOnly("yield"));
				}
			}
		}
	}
	
	@Override public void endVisitCompilationUnit(EclipseNode top, CompilationUnitDeclaration unit) {
		if (handled) {
			deleteMethodCallImports(top, methodName, Yield.class, "yield");
		}
	}
	
	public boolean handle(EclipseNode methodNode) {
		final boolean returnsIterable = returns(methodNode, Iterable.class);
		final boolean returnsIterator = returns(methodNode, Iterator.class);
		if (!(returnsIterable || returnsIterator)) {
			methodNode.addError("Method that contain yield() can only return java.util.Iterator or java.lang.Iterable");
			return true;
		}
		final String yielderName = yielderName(methodNode);
		
		return true;
	}
	
	private String yielderName(EclipseNode methodNode) {
		String[] parts = methodNode.getName().split("_");
		String[] newParts = new String[parts.length + 1];
		newParts[0] = "yielder";
		System.arraycopy(parts, 0, newParts, 1, parts.length);
		return camelCase("$", newParts);
	}
	
	private boolean returns(EclipseNode methodNode, Class<?> clazz) {
		MethodDeclaration methodDecl = (MethodDeclaration)methodNode.get();
		TypeReference returnType = methodDecl.returnType;
		// TODO
		return false;
	}
}