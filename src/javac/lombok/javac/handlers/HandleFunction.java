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

import static lombok.ast.AST.*;
import static lombok.ast.IMethod.ArgumentStyle.BOXED_TYPES;
import static lombok.ast.IMethod.ArgumentStyle.INCLUDE_ANNOTATIONS;
import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Names.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.*;

import lombok.*;
import lombok.ast.Argument;
import lombok.ast.MethodDecl;
import lombok.ast.TypeRef;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.ResolutionBased;
import lombok.javac.handlers.ast.JavacMethod;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import org.mangosdk.spi.ProviderFor;

/**
 * Handles the {@link Function} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
@ResolutionBased
public class HandleFunction extends JavacAnnotationHandler<Function> {

	@Override
	public void handle(final AnnotationValues<Function> annotation, final JCAnnotation source, final JavacNode annotationNode) {
		final Class<? extends java.lang.annotation.Annotation> annotationType = Function.class;
		deleteAnnotationIfNeccessary(annotationNode, annotationType);
		final JavacMethod method = JavacMethod.methodOf(annotationNode, source);
		if (method.isAbstract() || method.isEmpty()) {
			annotationNode.addError(canBeUsedOnConcreteMethodOnly(annotationType));
			return;
		}

		final Object templates = annotation.getActualExpression("template");
		final TypeSymbol resolvedTemplates = resolveTemplates(method.node(), source, templates);
		if (resolvedTemplates == null) {
			annotationNode.addError("@Function unable to resolve template type");
			return;
		}
		final List<TemplateData> matchingTemplates = findTemplatesFor(method.get(), resolvedTemplates);
		if (matchingTemplates.isEmpty()) {
			annotationNode.addError("@Function no template found that matches the given method signature");
			return;
		}
		if (matchingTemplates.size() > 1) {
			annotationNode.addError("@Function more than one template found that matches the given method signature");
			return;
		}
		final TemplateData matchingTemplate = matchingTemplates.get(0);
		rebuildFunctionMethod(method, matchingTemplate);
	}
	
	private void rebuildFunctionMethod(final JavacMethod method, final TemplateData template) {
		final JavacType type = method.surroundingType();
		final TypeRef boxedReturnType = method.boxedReturns();
		final List<TypeRef> boxedArgumentTypes = new ArrayList<TypeRef>();
		final List<Argument> boxedArguments = method.arguments(BOXED_TYPES, INCLUDE_ANNOTATIONS);
		for (Argument argument : boxedArguments) {
			boxedArgumentTypes.add(argument.getType());
		}
		final TypeRef interfaceType = Type(template.typeName).withTypeArguments(boxedArgumentTypes).withTypeArgument(boxedReturnType);
		if (method.returns("void")) {
			method.replaceReturns(Return(Null()));
		}
		final MethodDecl innerMethod = MethodDecl(boxedReturnType, template.methodName).withArguments(boxedArguments).makePublic().implementing() //
			.withStatements(method.statements());
		if (method.returns("void")) {
			innerMethod.withStatement(Return(Null()));
		}
		final MethodDecl functionMethod = MethodDecl(interfaceType, method.name()).withTypeParameters(method.typeParameters()).withAnnotations(method.annotations()) //
			.withStatement(Return(New(interfaceType).withTypeDeclaration(ClassDecl("").makeAnonymous().makeLocal() //
				.withMethod(innerMethod))));
		if (method.isStatic()) functionMethod.makeStatic();
		functionMethod.withAccessLevel(method.accessLevel());
		type.injectMethod(functionMethod);
		type.removeMethod(method);
		type.rebuild();
	}

	private TypeSymbol resolveTemplates(final JavacNode node, final JCAnnotation annotation, final Object templatesDef) {
		if (templatesDef instanceof JCFieldAccess) {
			final JCFieldAccess templates = (JCFieldAccess) templatesDef;
			if (!"class".equals(string(templates.name))) return null;
			final Type templatesType = resolveClassMember(node, templates.selected);
			return (templatesType == null) ? null : templatesType.asElement();
		} else {
			final Type annotationType = resolveClassMember(node, (JCExpression) annotation.annotationType);
			if (annotationType == null) return null;
			final List<MethodSymbol> enclosedMethods = enclosedMethodsOf(annotationType.asElement());
			if (enclosedMethods.size() != 1) return null;
			final Attribute.Class defaultValue = (Attribute.Class) enclosedMethods.get(0).getDefaultValue();
			return defaultValue.getValue().asElement();
		}
	}
	
	private static Type resolveClassMember(final JavacNode node, final JCExpression expr) {
		Type type = expr.type;
		if (type == null) {
			new JavacResolution(node.getContext()).resolveClassMember(node);
			type = expr.type;
		}
		return type;
	}

	private List<TemplateData> findTemplatesFor(final JCMethodDecl methodDecl, final TypeSymbol template) {
		final List<TemplateData> foundTemplates = new ArrayList<TemplateData>();
		final TemplateData templateData = templateDataFor(methodDecl, template);
		if (templateData != null) foundTemplates.add(templateData);
		for (Symbol enclosedElement : template.getEnclosedElements()) {
			if (!(enclosedElement instanceof TypeSymbol)) continue;
			final TypeSymbol enclosedType = (TypeSymbol) enclosedElement;
			if (!enclosedType.isStatic()) continue;
			foundTemplates.addAll(findTemplatesFor(methodDecl, enclosedType));
		}
		return foundTemplates;
	}
	
	private TemplateData templateDataFor(final JCMethodDecl methodDecl, final TypeSymbol template) {
		if ((template.flags_field & (Flags.PUBLIC)) == 0) return null;
		if (!template.isInterface()) return null;
		final List<Type> templateTypeArguments = new ArrayList<Type>(template.type.getTypeArguments());
		final List<MethodSymbol> enclosedMethods = enclosedMethodsOf(template);
		if (enclosedMethods.size() != 1) return null;
		final MethodSymbol enclosedMethod = enclosedMethods.get(0);
		final Type enclosedMethodType = enclosedMethod.type;
		final List<Type> methodTypeArguments = new ArrayList<Type>(enclosedMethodType.getParameterTypes());
		methodTypeArguments.add(enclosedMethodType.getReturnType());
		if (!templateTypeArguments.equals(methodTypeArguments)) return null;
		if ((methodDecl.getParameters().size() + 1) != templateTypeArguments.size()) return null;
		return new TemplateData(string(template.getQualifiedName()), string(enclosedMethod.name));
	}
	
	private List<MethodSymbol> enclosedMethodsOf(final TypeSymbol type) {
		final List<MethodSymbol> enclosedMethods = new ArrayList<MethodSymbol>();
		for (Symbol enclosedElement : type.getEnclosedElements()) {
			if (enclosedElement instanceof MethodSymbol) {
				enclosedMethods.add((MethodSymbol) enclosedElement);
			}
		}
		return enclosedMethods;
	}
	
	@RequiredArgsConstructor
	@Getter
	private static class TemplateData {
		private final String typeName;
		private final String methodName;
	}
}
