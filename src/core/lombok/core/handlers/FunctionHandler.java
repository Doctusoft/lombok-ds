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
import static lombok.ast.IMethod.ArgumentStyle.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;

public final class FunctionHandler<TYPE_TYPE extends IType<METHOD_TYPE, ?, ?, ?, ?>, METHOD_TYPE extends IMethod<TYPE_TYPE, ?, ?, ?>> {

	public void rebuildFunctionMethod(final METHOD_TYPE method, final TemplateData template, final IParameterValidator<METHOD_TYPE> validation,
			final IParameterSanitizer<METHOD_TYPE> sanitizer) {
		final TYPE_TYPE type = method.surroundingType();
		final TypeRef boxedReturnType = method.boxedReturns();
		final List<TypeRef> boxedArgumentTypes = new ArrayList<TypeRef>();
		final List<Argument> arguments = withUnderscoreName(method.arguments(INCLUDE_ANNOTATIONS));
		final List<Argument> boxedArguments = method.arguments(BOXED_TYPES, INCLUDE_ANNOTATIONS);
		boxedArguments.removeAll(withUnderscoreName(boxedArguments));
		for (Argument argument : boxedArguments) {
			boxedArgumentTypes.add(argument.getType());
		}
		final TypeRef interfaceType = Type(template.typeName).withTypeArguments(boxedArgumentTypes).withTypeArgument(boxedReturnType);
		if (method.returns("void")) {
			method.replaceReturns(Return(Null()));
		}
		final MethodDecl innerMethod = MethodDecl(boxedReturnType, template.methodName).withArguments(boxedArguments).makePublic().implementing() //
				.withStatements(validation.validateParameterOf(method)) //
				.withStatements(sanitizer.sanitizeParameterOf(method)) //
				.withStatements(method.statements());
		if (method.returns("void")) {
			innerMethod.withStatement(Return(Null()));
		}
		final MethodDecl functionMethod = MethodDecl(interfaceType, method.name()).withArguments(arguments).withTypeParameters(method.typeParameters())
				.withAnnotations(method.annotations()) //
				.withStatement(Return(New(interfaceType).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(innerMethod))));
		if (method.isStatic())
			functionMethod.makeStatic();
		functionMethod.withAccessLevel(method.accessLevel());
		type.injectMethod(functionMethod);
		type.removeMethod(method);
		type.rebuild();
	}

	private List<Argument> withUnderscoreName(final List<Argument> arguments) {
		final List<Argument> filtedList = new ArrayList<Argument>();
		for (Argument argument : arguments) {
			if (argument.getName().startsWith("_"))
				filtedList.add(argument);
		}
		return filtedList;
	}

	@RequiredArgsConstructor
	@Getter
	@ToString
	public static class TemplateData {
		private final String typeName;
		private final String methodName;
	}
}
