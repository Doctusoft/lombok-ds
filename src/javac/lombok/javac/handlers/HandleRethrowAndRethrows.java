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

import static lombok.core.util.Arrays.isEmpty;
import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.javac.handlers.JavacHandlerUtil.deleteAnnotationIfNeccessary;
import static lombok.javac.handlers.JavacTreeBuilder.statements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.Rethrow;
import lombok.Rethrows;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.core.util.Lists;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

public class HandleRethrowAndRethrows {
	private final static String TRY_BLOCK = "try %s ";
	private final static String CATCH_BLOCK_RETHROW = "catch ( java.lang.RuntimeException %s ) { throw %s; } ";
	private final static String CATCH_BLOCK_1ARG = "catch ( %s %s ) { throw new %s(%s); } ";
	private final static String CATCH_BLOCK_2ARGS = "catch ( %s %s ) { throw new %s(\"%s\", %s); } ";

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleRethrow extends JavacAnnotationHandler<Rethrow> {
		@Override
		public void handle(AnnotationValues<Rethrow> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Rethrow ann = annotation.getInstance();
			new HandleRethrowAndRethrows() //
				.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message())) //
				.handle(Rethrow.class, ast, annotationNode);
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleRethrows extends JavacAnnotationHandler<Rethrows> {
		@Override
		public void handle(AnnotationValues<Rethrows> annotation, JCAnnotation ast, JavacNode annotationNode) {
			HandleRethrowAndRethrows handle = new HandleRethrowAndRethrows();
			for (Object rethrow: annotation.getActualExpressions("value")) {
				JavacNode rethrowNode = new JavacNode(annotationNode.getAst(), (JCTree)rethrow, new ArrayList<JavacNode>(), Kind.ANNOTATION);
				Rethrow ann = Javac.createAnnotation(Rethrow.class, rethrowNode).getInstance();
				handle.withRethrow(new RethrowData(classNames(ann.value()), ann.as(), ann.message()));
			}
			handle.handle(Rethrow.class, ast, annotationNode);
		}
	}

	private List<RethrowData> rethrows = new ArrayList<RethrowData>();

	public HandleRethrowAndRethrows withRethrow(final RethrowData rethrowData) {
		rethrows.add(rethrowData);
		return this;
	}

	public void handle(Class<? extends Annotation> annotationType, JCAnnotation source, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, annotationType);

		if (rethrows.isEmpty()) {
			return;
		}

		JavacMethod method = JavacMethod.methodOf(annotationNode);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		StringBuilder methodBody = new StringBuilder();
		methodBody.append(String.format(TRY_BLOCK, method.get().body));
		int counter = 1;
		for (RethrowData rethrow : rethrows) {
			for (Class<?> thrown : rethrow.thrown) {
				String varname = "$e" + counter++;
				if (RethrowData.class == thrown) {
					methodBody.append(String.format(CATCH_BLOCK_RETHROW, varname, varname));
				} else if (rethrow.message.isEmpty()) {
					methodBody.append(String.format(CATCH_BLOCK_1ARG, thrown.getName(), varname, rethrow.as.getName(), varname));
				} else {
					methodBody.append(String.format(CATCH_BLOCK_2ARGS, thrown.getName(), varname, rethrow.as.getName(), rethrow.message, varname));
				}
			}
		}

		method.body(statements(method.node(), methodBody.toString()));

		method.rebuild(source);
	}

	private static List<Class<?>> classNames(final Class<?>[] classes) {
		if (isEmpty(classes)) {
			return Lists.<Class<?>>list(RethrowData.class, Exception.class);
		}
		return Lists.list(classes);
	}

	@RequiredArgsConstructor
	private static class RethrowData {
		public final List<Class<?>> thrown;
		public final Class<?> as;
		public final String message;
	}
}
