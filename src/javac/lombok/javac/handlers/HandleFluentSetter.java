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

import static lombok.core.util.ErrorMessages.*;
import static com.sun.tools.javac.code.Flags.*;
import static lombok.javac.handlers.Javac.typeDeclFiltering;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.FluentSetter;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.handlers.TransformationsUtil;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil.FieldAccess;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

/**
 * Handles the {@code lombok.FluentSetter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFluentSetter extends NonResolutionBased implements JavacAnnotationHandler<FluentSetter> {

	@Override public void handle(AnnotationValues<FluentSetter> annotation, JCAnnotation ast, JavacNode annotationNode) {
		Collection<JavacNode> fields = annotationNode.upFromAnnotationToFields();
		deleteAnnotationIfNeccessary(annotationNode, FluentSetter.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
		FluentSetter annotationInstance = annotation.getInstance();
		AccessLevel level = annotationInstance.value();
		handle(ast, annotationNode, level, fields);
	}

	public void generateSetterForType(JavacNode typeNode, JavacNode errorNode, AccessLevel level, boolean checkForTypeLevelSetter) {
		if (checkForTypeLevelSetter) {
			if (typeNode != null) for (JavacNode child : typeNode.down()) {
				if (child.getKind() == Kind.ANNOTATION) {
					if (Javac.annotationTypeMatches(FluentSetter.class, child)) {
						return;
					}
				}
			}
		}

		JCClassDecl typeDecl = typeDeclFiltering(typeNode, INTERFACE | ANNOTATION | ENUM);
		if (typeDecl == null) {
			errorNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}

		for (JavacNode field : typeNode.down()) {
			if (field.getKind() != Kind.FIELD) continue;
			JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
			if (fieldDecl.name.toString().startsWith("$")) continue;
			if ((fieldDecl.mods.flags & STATIC) != 0) continue;
			if ((fieldDecl.mods.flags & FINAL) != 0) continue;

			generateSetterForField(field, errorNode.get(), level, List.<JCExpression>nil(), List.<JCExpression>nil());
		}
	}

	public void generateSetterForField(JavacNode fieldNode, DiagnosticPosition pos, AccessLevel level, List<JCExpression> onMethod, List<JCExpression> onParam) {
		for (JavacNode child : fieldNode.down()) {
			if (child.getKind() == Kind.ANNOTATION) {
				if (Javac.annotationTypeMatches(FluentSetter.class, child)) {
					return;
				}
			}
		}
		createSetterForField(level, fieldNode, fieldNode, false, onMethod, onParam);
	}

	public void handle(JCAnnotation ast, JavacNode annotationNode, AccessLevel level, Collection<JavacNode> fields) {
		if (level == AccessLevel.NONE) return;

		JavacNode node = annotationNode.up();
		if (node == null) return;
		List<JCExpression> onMethod = getAndRemoveAnnotationParameter(ast, "onMethod");
		List<JCExpression> onParam = getAndRemoveAnnotationParameter(ast, "onParam");
		if (node.getKind() == Kind.FIELD) {
			createSetterForFields(level, fields, annotationNode, true, onMethod, onParam);
		}
		if (node.getKind() == Kind.TYPE) {
			if (!onMethod.isEmpty()) annotationNode.addError("'onMethod' is not supported for @Setter on a type.");
			if (!onParam.isEmpty()) annotationNode.addError("'onParam' is not supported for @Setter on a type.");
			generateSetterForType(node, annotationNode, level, false);
		}
	}

	private void createSetterForFields(AccessLevel level, Collection<JavacNode> fieldNodes, JavacNode errorNode, boolean whineIfExists,
			List<JCExpression> onMethod, List<JCExpression> onParam) {
		for (JavacNode fieldNode : fieldNodes) {
			createSetterForField(level, fieldNode, errorNode, whineIfExists, onMethod, onParam);
		}
	}

	private void createSetterForField(AccessLevel level, JavacNode fieldNode, JavacNode source, boolean whineIfExists,
			List<JCExpression> onMethod, List<JCExpression> onParam) {
		if (fieldNode.getKind() != Kind.FIELD) {
			fieldNode.addError(canBeUsedOnClassAndFieldOnly(FluentSetter.class));
			return;
		}

		JCVariableDecl fieldDecl = (JCVariableDecl)fieldNode.get();

		String methodName = fieldDecl.name.toString();

		switch (methodExists(methodName, fieldNode, false)) {
		case EXISTS_BY_LOMBOK:
			return;
		case EXISTS_BY_USER:
			if (whineIfExists) source.addWarning(
					String.format("Not generating %s(%s %s): A method with that name already exists",
					methodName, fieldDecl.vartype, fieldDecl.name));
			return;
		default:
		case NOT_EXISTS:
		}

		long access = toJavacModifier(level) | (fieldDecl.mods.flags & STATIC);

		injectMethod(fieldNode.up(), createSetter(access, fieldNode, fieldNode.getTreeMaker(), onMethod, onParam, source.get()));
	}

	private JCMethodDecl createSetter(long access, JavacNode field, TreeMaker treeMaker, List<JCExpression> onMethod, List<JCExpression> onParam, JCTree source) {
		JCVariableDecl fieldDecl = (JCVariableDecl) field.get();

		JCExpression fieldRef = createFieldAccessor(treeMaker, field, FieldAccess.ALWAYS_FIELD);
		JCAssign assign = treeMaker.Assign(fieldRef, treeMaker.Ident(fieldDecl.name));

		List<JCStatement> statements;
		List<JCAnnotation> nonNulls = findAnnotations(field, TransformationsUtil.NON_NULL_PATTERN);
		List<JCAnnotation> nullables = findAnnotations(field, TransformationsUtil.NULLABLE_PATTERN);

		if (nonNulls.isEmpty()) {
			statements = List.<JCStatement>of(treeMaker.Exec(assign));
		} else {
			JCStatement nullCheck = generateNullCheck(treeMaker, field);
			if (nullCheck != null) statements = List.<JCStatement>of(nullCheck, treeMaker.Exec(assign));
			else statements = List.<JCStatement>of(treeMaker.Exec(assign));
		}

		JCStatement returnStatement = treeMaker.Return(treeMaker.Ident(field.toName("this")));
		statements = statements.append(returnStatement);

		JCBlock methodBody = treeMaker.Block(0, statements);
		List<JCAnnotation> annsOnParam = copyAnnotations(onParam);
		annsOnParam = annsOnParam.appendList(nonNulls).appendList(nullables);
		JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(FINAL, annsOnParam), fieldDecl.name, fieldDecl.vartype, null);
		JCClassDecl classNode = (JCClassDecl) field.up().get();
		JCExpression methodType = treeMaker.Ident(classNode.name);

		if (!classNode.typarams.isEmpty()) {
			List<JCExpression> typeArgs = List.nil();
			for (JCTypeParameter typeparam : classNode.typarams) {
				typeArgs = typeArgs.append(treeMaker.Ident(typeparam.name));
			}
			methodType = treeMaker.TypeApply(methodType, typeArgs);
		}

		List<JCTypeParameter> methodGenericParams = List.nil();
		List<JCVariableDecl> parameters = List.of(param);
		List<JCExpression> throwsClauses = List.nil();
		JCExpression annotationMethodDefaultValue = null;

		return Javac.recursiveSetGeneratedBy(treeMaker.MethodDef(treeMaker.Modifiers(access, copyAnnotations(onMethod)), fieldDecl.name, methodType,
				methodGenericParams, parameters, throwsClauses, methodBody, annotationMethodDefaultValue), source);
	}
}
