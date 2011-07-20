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

import static lombok.core.handlers.RethrowAndRethrowsHandler.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.handlers.RethrowAndRethrowsHandler;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

public class HandleRethrowAndRethrows {

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleRethrow extends JavacAnnotationHandler<Rethrow> {
		@Override
		public void handle(AnnotationValues<Rethrow> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Rethrow ann = annotation.getInstance();
			prepareRethrowAndRethrowsHandler(annotationNode, ast, ann.getClass()) //
				.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message())) //
				.handle(Rethrow.class);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleRethrows extends JavacAnnotationHandler<Rethrows> {
		@Override
		public void handle(AnnotationValues<Rethrows> annotation, JCAnnotation ast, JavacNode annotationNode) {
			RethrowAndRethrowsHandler handler = prepareRethrowAndRethrowsHandler(annotationNode, ast, Rethrow.class);
			for (Object rethrow: annotation.getActualExpressions("value")) {
				JavacNode rethrowNode = new JavacNode(annotationNode.getAst(), (JCTree)rethrow, new ArrayList<JavacNode>(), Kind.ANNOTATION);
				Rethrow ann = Javac.createAnnotation(Rethrow.class, rethrowNode).getInstance();
				handler.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message()));
			}
			handler.handle(Rethrow.class);
		}
	}
	
	private static RethrowAndRethrowsHandler prepareRethrowAndRethrowsHandler(JavacNode node, JCAnnotation source, Class<? extends java.lang.annotation.Annotation> annotationType) {
		deleteAnnotationIfNeccessary(node, annotationType);
		deleteImportFromCompilationUnit(node, Rethrow.class.getName());
		return new RethrowAndRethrowsHandler(JavacMethod.methodOf(node, source), node);
	}
}
