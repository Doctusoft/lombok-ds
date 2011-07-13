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

import static lombok.ast.AST.*;
import static lombok.core.util.Arrays.*;
import static lombok.core.util.ErrorMessages.*;
import static lombok.eclipse.handlers.Eclipse.*;

import java.util.*;

import lombok.*;
import lombok.ast.*;
import lombok.core.AnnotationValues;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleDoPrivileged extends EclipseAnnotationHandler<DoPrivileged> {
	@Override public boolean deferUntilPostDiet() {
		return true;
	}

	@Override public void handle(AnnotationValues<DoPrivileged> annotation, Annotation source, EclipseNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = DoPrivileged.class;

		final EclipseMethod method = EclipseMethod.methodOf(annotationNode, source);

		if (method == null) {
			annotationNode.addError(canBeUsedOnMethodOnly(annotationType));
			return;
		}
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		replaceWithQualifiedThisReference(method, source);

		final TypeRef innerReturnType = method.boxedReturns();
		if (method.returns("void")) {
			replaceReturns(method);
			method.body(Block() //
				.withStatements(sanitizeParameter(method)) //
				.withStatement(Try(Block() //
					.withStatement(Call(Name("java.security.AccessController"), "doPrivileged").withArgument( //
						New(Type("java.security.PrivilegedExceptionAction").withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()) //
							.withStatement(Return(Null()))))))) //
				.Catch(Arg(Type("java.security.PrivilegedActionException"), "$ex"), Block() //
					.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))))));
		} else {
			method.body(Block() //
				.withStatements(sanitizeParameter(method)) //
				.withStatement(Try(Block() //
					.withStatement(Return(Call(Name("java.security.AccessController"), "doPrivileged").withArgument( //
						New(Type("java.security.PrivilegedExceptionAction").withTypeArgument(innerReturnType)).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
						.withMethod(MethodDecl(innerReturnType, "run").makePublic().withThrownExceptions(method.thrownExceptions()) //
							.withStatements(method.statements()))))))) //
				.Catch(Arg(Type("java.security.PrivilegedActionException"), "$ex"), Block() //
					.withStatement(LocalDecl(Type("java.lang.Throwable"), "$cause").makeFinal().withInitialization(Call(Name("$ex"), "getCause"))) //
					.withStatements(rethrowStatements(method)) //
					.withStatement(Throw(New(Type("java.lang.RuntimeException")).withArgument(Name("$cause")))))));
		}

		method.rebuild();
	}

	private List<lombok.ast.Statement> sanitizeParameter(final EclipseMethod method) {
		final List<lombok.ast.Statement> sanitizeStatements = new ArrayList<lombok.ast.Statement>();
		if (isNotEmpty(method.get().arguments)) for (Argument argument : method.get().arguments) {
			final Annotation ann = getAnnotation(DoPrivileged.SanitizeWith.class, argument);
			if (ann != null) {
				final EclipseNode annotationNode = method.node().getNodeFor(ann);
				String sanatizeMethodName = Eclipse.createAnnotation(DoPrivileged.SanitizeWith.class, annotationNode).getInstance().value();
				final String argumentName = new String(argument.name);
				final String newArgumentName = "$" + argumentName;
				sanitizeStatements.add(LocalDecl(Type(argument.type), argumentName).withInitialization(Call(sanatizeMethodName).withArgument(Name(newArgumentName))));
				argument.name = newArgumentName.toCharArray();
				argument.modifiers |= Modifier.FINAL;
			}
		}
		return sanitizeStatements;
	}

	private List<lombok.ast.Statement> rethrowStatements(final EclipseMethod method) {
		final List<lombok.ast.Statement> rethrowStatements = new ArrayList<lombok.ast.Statement>();
		for (lombok.ast.TypeRef thrownException : method.thrownExceptions()) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), thrownException)) //
				.Then(Throw(Cast(thrownException, Name("$cause")))));
		}
		return rethrowStatements;
	}

	private void replaceReturns(final EclipseMethod method) {
		final IReplacementProvider<Statement> replacement = new ReturnNullReplacementProvider(method);
		new ReturnStatementReplaceVisitor(replacement).visit(method.get());
	}

	private void replaceWithQualifiedThisReference(final EclipseMethod method, final ASTNode source) {
		final IReplacementProvider<Expression> replacement = new QualifiedThisReplacementProvider(method.surroundingType().name(), source);
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}

	@RequiredArgsConstructor
	private static class ReturnNullReplacementProvider implements IReplacementProvider<Statement> {
		private final EclipseMethod method;

		@Override public Statement getReplacement() {
			return method.build(Return(Null()));
		}
	}
}
