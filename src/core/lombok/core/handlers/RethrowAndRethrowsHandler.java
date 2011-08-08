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
package lombok.core.handlers;

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.ast.*;
import lombok.core.DiagnosticsReceiver;
import lombok.core.util.Lists;

@RequiredArgsConstructor
public final class RethrowAndRethrowsHandler<METHOD_TYPE extends IMethod<?, ?, ?, ?>> {
	private final List<RethrowData> rethrows = new ArrayList<RethrowData>();
	private final METHOD_TYPE method;
	private final DiagnosticsReceiver diagnosticsReceiver;

	public RethrowAndRethrowsHandler<METHOD_TYPE> withRethrow(final RethrowData rethrowData) {
		rethrows.add(rethrowData);
		return this;
	}

	public void handle(final Class<? extends java.lang.annotation.Annotation> annotationType, final IParameterValidator<METHOD_TYPE> validation,
			final IParameterSanitizer<METHOD_TYPE> sanitizer) {
		if (rethrows.isEmpty()) {
			return;
		}

		if (method == null) {
			diagnosticsReceiver.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}

		if (method.isAbstract() || method.isEmpty()) {
			diagnosticsReceiver.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		Try tryBuilder = Try(Block() //
			.withStatements(validation.validateParameterOf(method)) //
			.withStatements(sanitizer.sanitizeParameterOf(method)) //
			.withStatements(method.statements()));
		int counter = 1;
		for (RethrowData rethrow : rethrows) {
			for (Class<?> thrown : rethrow.thrown) {
				String varname = "$e" + counter++;
				if (RethrowData.class == thrown) {
					tryBuilder.Catch(Arg(Type("java.lang.RuntimeException"), varname), Block().withStatement(Throw(Name(varname))));
				} else if (rethrow.message.isEmpty()) {
					tryBuilder.Catch(Arg(Type(thrown.getName()), varname), Block().withStatement(Throw(New(Type(rethrow.as.getName())) //
						.withArgument(Name(varname)))));
				} else {
					tryBuilder.Catch(Arg(Type(thrown.getName()), varname), Block().withStatement(Throw(New(Type(rethrow.as.getName())) //
						.withArgument(String(rethrow.message)).withArgument(Name(varname)))));
				}
			}
		}
		method.body(Block().withStatement(tryBuilder));

		method.rebuild();
	}

	public static List<Class<?>> classNames(final Class<?>[] classes) {
		if (isEmpty(classes)) {
			return Lists.<Class<?>> list(RethrowData.class, Exception.class);
		}
		return Lists.list(classes);
	}

	@RequiredArgsConstructor
	public static class RethrowData {
		public final List<Class<?>> thrown;
		public final Class<?> as;
		public final String message;
	}
}
