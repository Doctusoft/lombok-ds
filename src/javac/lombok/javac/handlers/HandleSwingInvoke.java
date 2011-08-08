/*
 * Copyright © 2010-2011 Philipp Eichhorn
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

import static lombok.javac.handlers.JavacHandlerUtil.*;

import lombok.*;
import lombok.core.AnnotationValues;
import lombok.core.handlers.SwingInvokeHandler;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacMethod;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.SwingInvokeLater} and {@code lombok.SwingInvokeAndWait} annotation for javac.
 */
public class HandleSwingInvoke {

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeLater extends JavacAnnotationHandler<SwingInvokeLater> {
		@Override public void handle(final AnnotationValues<SwingInvokeLater> annotation, final JCAnnotation source, final JavacNode annotationNode) {
			final Class<? extends java.lang.annotation.Annotation> annotationType = SwingInvokeLater.class;
			deleteAnnotationIfNeccessary(annotationNode, annotationType);
			new SwingInvokeHandler<JavacMethod>(JavacMethod.methodOf(annotationNode, source), annotationNode) //
				.handle("invokeLater", annotationType, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}

	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSwingInvokeAndWait extends JavacAnnotationHandler<SwingInvokeAndWait> {
		@Override public void handle(final AnnotationValues<SwingInvokeAndWait> annotation, final JCAnnotation source, final JavacNode annotationNode) {
			final Class<? extends java.lang.annotation.Annotation> annotationType = SwingInvokeAndWait.class;
			deleteAnnotationIfNeccessary(annotationNode, annotationType);
			new SwingInvokeHandler<JavacMethod>(JavacMethod.methodOf(annotationNode, source), annotationNode) //
				.handle("invokeAndWait", annotationType, new JavacParameterValidator(), new JavacParameterSanitizer());
		}
	}
}