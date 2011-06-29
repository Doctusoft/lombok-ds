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

import static org.eclipse.jdt.core.dom.Modifier.FINAL;
import static lombok.core.util.Arrays.isNotEmpty;
import static lombok.core.util.ErrorMessages.canBeUsedOnConcreteMethodOnly;
import static lombok.core.util.ErrorMessages.canBeUsedOnMethodOnly;
import static lombok.core.util.Names.capitalize;
import static lombok.eclipse.handlers.Eclipse.getAnnotation;
import static lombok.eclipse.handlers.Eclipse.typeNodeOf;
import static lombok.ast.AST.*;

import java.util.ArrayList;
import java.util.List;

import lombok.DoPrivileged;
import lombok.RequiredArgsConstructor;
import lombok.DoPrivileged.SanitizeWith;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.ast.EclipseASTMaker;
import lombok.eclipse.handlers.ast.EclipseMethod;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@code lombok.DoPrivileged} annotation for eclipse.
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleDoPrivileged extends EclipseAnnotationHandler<DoPrivileged> {
	public boolean deferUntilPostDiet() {
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

		TypeReference returnType = method.returnType();
		TypeRef innerReturnType = boxedReturnType(returnType);

		if (method.returns("void")) {
			replaceReturns(method.get(), source);
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
	}

	private lombok.ast.TypeRef boxedReturnType(TypeReference type) {
		lombok.ast.TypeRef objectReturnType = Type(type);
		if (type instanceof SingleTypeReference) {
			final String name = new String(type.getLastToken());
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

	private List<lombok.ast.Statement> sanitizeParameter(final EclipseMethod method) {
		final List<lombok.ast.Statement> sanitizeStatements = new ArrayList<lombok.ast.Statement>();
		if (isNotEmpty(method.get().arguments)) for (Argument argument : method.get().arguments) {
			final Annotation ann = getAnnotation(SanitizeWith.class, argument);
			if (ann != null) {
				final EclipseNode annotationNode = method.node().getNodeFor(ann);
				String sanatizeMethodName = Eclipse.createAnnotation(SanitizeWith.class, annotationNode).getInstance().value();
				final String argumentName = new String(argument.name);
				final String newArgumentName = "$" + argumentName;
				sanitizeStatements.add(LocalDecl(Type(argument.type), argumentName).withInitialization(Call(sanatizeMethodName).withArgument(Name(newArgumentName))));
				argument.name = newArgumentName.toCharArray();
				argument.modifiers |= FINAL;
			}
		}
		return sanitizeStatements;
	}

	private List<lombok.ast.Statement> rethrowStatements(final EclipseMethod method) {
		final List<lombok.ast.Statement> rethrowStatements = new ArrayList<lombok.ast.Statement>();
		if (isNotEmpty(method.get().thrownExceptions)) for (TypeReference thrownException : method.get().thrownExceptions) {
			rethrowStatements.add(If(InstanceOf(Name("$cause"), Type(thrownException))) //
				.Then(Throw(Cast(Type(thrownException), Name("$cause")))));
		}
		return rethrowStatements;
	}

	private void replaceReturns(AbstractMethodDeclaration method, final ASTNode source) {
		final IReplacementProvider<Statement> replacement = new ReturnNullReplacementProvider(source);
		new ReturnStatementReplaceVisitor(replacement).visit(method);
	}

	private void replaceWithQualifiedThisReference(final EclipseMethod method, final ASTNode source) {
		final EclipseNode parent = typeNodeOf(method.node());
		final TypeDeclaration typeDec = (TypeDeclaration)parent.get();
		final IReplacementProvider<Expression> replacement = new QualifiedThisReplacementProvider(new String(typeDec.name), source);
		new ThisReferenceReplaceVisitor(replacement).visit(method.get());
	}

	@RequiredArgsConstructor
	private static class ReturnNullReplacementProvider implements IReplacementProvider<Statement> {
		private final ASTNode source;

		@Override public Statement getReplacement() {
			return new EclipseASTMaker(null, source).build(Return(Null()));
		}
	}
}
