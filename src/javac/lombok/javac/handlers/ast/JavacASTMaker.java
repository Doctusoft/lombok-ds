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
package lombok.javac.handlers.ast;

import static lombok.ast.AST.*;
import static lombok.javac.Javac.*;
import static lombok.javac.handlers.Javac.*;
import static com.sun.tools.javac.code.Flags.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import lombok.core.util.Cast;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;

public final class JavacASTMaker implements lombok.ast.ASTVisitor<JCTree, Void> {
	private final JavacNode sourceNode;
	private final JCTree source;
	private final TreeMaker M;

	public JavacASTMaker(final JavacNode sourceNode, final JCTree source) {
		this.sourceNode = sourceNode;
		this.source = source;
		M = sourceNode.getTreeMaker().at(source.pos);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node) {
		return this.<T>build(node, null);
	}

	public <T extends JCTree> T build(final lombok.ast.Node node, final Class<T> extectedType) {
		if (node == null) return null;
		JCTree tree = node.accept(this, null);
		if ((JCStatement.class == extectedType ) && (tree instanceof JCExpression)) {
			tree = M.Exec((JCExpression) tree);
		}
		return Cast.<T>uncheckedCast(tree);
	}

	public <T extends JCTree> List<T> build(final java.util.List<? extends lombok.ast.Node> nodes) {
		return this.<T>build(nodes, null);
	}

	public <T extends JCTree> List<T> build(final java.util.List<? extends lombok.ast.Node> nodes, final Class<T> extectedType) {
		if (nodes == null) return null;
		ListBuffer<T> list = ListBuffer.lb();
		for (lombok.ast.Node node : nodes) {
			list.append(build(node, extectedType));
		}
		return list.toList();
	}

	private Name name(final String name) {
		return sourceNode.toName(name);
	}

	private JCExpression chainDots(final String name) {
		return JavacHandlerUtil.chainDotsString(M, sourceNode, name);
	}

	private JCExpression fixLeadingDot(final JCExpression expr) {
		if (expr instanceof JCFieldAccess) {
			JCFieldAccess fieldAccess = (JCFieldAccess) expr;
			JCExpression selExpr = fieldAccess.selected;
			if (selExpr instanceof JCIdent) {
				if ("".equals(selExpr.toString())) {
					return M.Ident(fieldAccess.name);
				}
			} else if (selExpr instanceof JCFieldAccess) {
				fieldAccess.selected = fixLeadingDot(selExpr);
			}
		}
		return expr;
	}

	private long flagsFor(final Set<lombok.ast.Modifier> modifiers) {
		long flags = 0;
		flags |= modifiers.contains(lombok.ast.Modifier.FINAL) ? FINAL : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.PRIVATE) ? PRIVATE : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.PROTECTED) ? PROTECTED : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.PUBLIC) ? PUBLIC : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.STATIC) ? STATIC : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.TRANSIENT) ? TRANSIENT : 0;
		flags |= modifiers.contains(lombok.ast.Modifier.VOLATILE) ? VOLATILE : 0;
		return flags;
	}

	@Override
	public JCTree visitAnnotation(final lombok.ast.Annotation node, final Void p) {
		final ListBuffer<JCExpression> args = ListBuffer.lb();
		for (Entry<String, lombok.ast.Expression> entry : node.getValues().entrySet()) {
			args.append(build(Assign(Name(entry.getKey()), entry.getValue()), JCExpression.class));
		}
		final JCAnnotation annotation = setGeneratedBy(M.Annotation(build(node.getType()), args.toList()), source);
		return annotation;
	}

	@Override
	public JCTree visitArgument(final lombok.ast.Argument node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		final JCVariableDecl argument = setGeneratedBy(M.VarDef(mods, name(node.getName()), build(node.getType(), JCExpression.class), null), source);
		return argument;
	}

	@Override
	public JCTree visitArrayRef(final lombok.ast.ArrayRef node, final Void p) {
		final JCArrayAccess arrayAccess = setGeneratedBy(M.Indexed(build(node.getIndexed(), JCExpression.class), build(node.getIndex(), JCExpression.class)), source);
		return arrayAccess;
	}

	@Override
	public JCTree visitAssignment(final lombok.ast.Assignment node, final Void p) {
		final JCAssign assignment = setGeneratedBy(M.Assign(build(node.getLeft(), JCExpression.class), build(node.getRight(), JCExpression.class)), source);
		return assignment;
	}

	@Override
	public JCTree visitBinary(final lombok.ast.Binary node, final Void p) {
		final String operator = node.getOperator();
		final int opcode;
		if ("+".equals(operator)) {
			opcode = getCTCint(JCTree.class, "PLUS");
		} else if ("-".equals(operator)) {
			opcode = getCTCint(JCTree.class, "MINUS");
		} else if ("*".equals(operator)) {
			opcode = getCTCint(JCTree.class, "MUL");
		} else if ("/".equals(operator)) {
			opcode = getCTCint(JCTree.class, "DIV");
		} else if ("||".equals(operator)) {
			opcode = getCTCint(JCTree.class, "OR");
		} else if ("&&".equals(operator)) {
			opcode = getCTCint(JCTree.class, "AND");
		} else {
			throw new IllegalStateException(String.format("Unknown binary operator '%s'", operator));
		}
		JCBinary binary = setGeneratedBy(M.Binary(opcode, build(node.getLeft(), JCExpression.class), build(node.getRight(), JCExpression.class)), source);
		return binary;
	}

	@Override
	public JCTree visitBlock(final lombok.ast.Block node, final Void p) {
		final JCBlock block = setGeneratedBy(M.Block(0, build(node.getStatements(), JCStatement.class)), source);
		return block;
	}

	@Override
	public JCTree visitBooleanLiteral(final lombok.ast.BooleanLiteral node, final Void p) {
		final JCLiteral literal = setGeneratedBy(M.Literal(TypeTags.BOOLEAN, node.isTrue() ? 1 : 0), source);
		return literal;
	}

	@Override
	public JCTree visitCall(final lombok.ast.Call node, final Void p) {
		final JCExpression fn;
		if (node.getReceiver() == null) {
			fn = M.Ident(name(node.getName()));
		} else {
			fn = M.Select(build(node.getReceiver(), JCExpression.class), name(node.getName()));
		}
		final JCMethodInvocation methodInvocation = setGeneratedBy(M.Apply(build(node.getTypeArgs(), JCExpression.class), fn, build(node.getArgs(), JCExpression.class)), source);
		return methodInvocation;
	}

	@Override
	public JCTree visitCase(final lombok.ast.Case node, final Void p) {
		final JCCase caze = setGeneratedBy(M.Case(build(node.getPattern(), JCExpression.class), build(node.getStatements(), JCStatement.class)), source);
		return caze;
	}

	@Override
	public JCTree visitCast(final lombok.ast.Cast node, final Void p) {
		final JCTypeCast cast = setGeneratedBy(M.TypeCast(build(node.getType()), build(node.getExpression(), JCExpression.class)), source);
		return cast;
	}

	@Override
	public JCTree visitCharLiteral(final lombok.ast.CharLiteral node, final Void p) {
		final JCLiteral literal = setGeneratedBy(M.Literal(node.getCharacter().charAt(0)), source);
		return literal;
	}

	@Override
	public JCTree visitClassDecl(final lombok.ast.ClassDecl node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		if (node.isInterface()) mods.flags |= Flags.INTERFACE;
		final ListBuffer<JCTree> defs = ListBuffer.lb();
		defs.appendList(build(node.getFields()));
		defs.appendList(build(node.getMethods()));
		defs.appendList(build(node.getMemberTypes()));
		final JCClassDecl classDecl = setGeneratedBy(createClassDef(mods, name(node.getName()), build(node.getTypeParameters(), JCTypeParameter.class), build(node.getSuperclass(), JCExpression.class), build(node.getSuperInterfaces(), JCExpression.class), defs.toList()), source);
		return classDecl;
	}

	// to support both:
	//   javac 1.6 - M.ClassDef(JCModifiers, Name, List<JCTypeParameter>, JCTree, List<JCExpression>, List<JCTree>)
	//   and javac 1.7 - M.ClassDef(JCModifiers, Name, List<JCTypeParameter>, JCExpression, List<JCExpression>, List<JCTree>)
	private JCClassDecl createClassDef(final JCModifiers mods, final Name name, final List<JCTypeParameter> typarams, final JCExpression extending,
			final List<JCExpression> implementing, final List<JCTree> defs) {
		try {
			Method classDefMethod = null;
			for (Method method : TreeMaker.class.getMethods()) {
				if ("ClassDef".equals(method.getName())) {
					classDefMethod = method;
					break;
				}
			}
			if (classDefMethod == null) throw new IllegalStateException();
			return (JCClassDecl) classDefMethod.invoke(M, mods, name, typarams, extending, implementing, defs);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public JCTree visitConstructorDecl(final lombok.ast.ConstructorDecl node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		List<JCStatement> statements = build(node.getStatements(), JCStatement.class);
		if (node.implicitSuper()) {
			statements = statements.prepend(build(Call("super"), JCStatement.class));
		}
		final JCBlock body = setGeneratedBy(M.Block(0, statements), source);
		final JCMethodDecl constructor = setGeneratedBy(M.MethodDef(mods, name("<init>"), null, build(node.getTypeParameters(), JCTypeParameter.class), build(node.getArguments(), JCVariableDecl.class), build(node.getThrownExceptions(), JCExpression.class), body, null), source);
		return constructor;
	}

	@Override
	public JCTree visitContinue(final lombok.ast.Continue node, final Void p) {
		final JCContinue continueStatement = setGeneratedBy(M.Continue(node.getLabel() == null ? null : name(node.getLabel())), source);
		return continueStatement;
	}

	@Override
	public JCTree visitDoWhile(final lombok.ast.DoWhile node, final Void p) {
		final JCDoWhileLoop doStatement = setGeneratedBy(M.DoLoop(build(node.getAction(), JCStatement.class), build(node.getCondition(), JCExpression.class)), source);
		return doStatement;
	}

	@Override
	public JCTree visitEnumConstant(final lombok.ast.EnumConstant node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()) | ENUM, build(node.getAnnotations(), JCAnnotation.class)), source);
		lombok.ast.ClassDecl enumClassDecl = node.upTo(lombok.ast.ClassDecl.class);
		final JCExpression varType;
		if (enumClassDecl == null) {
			varType = build(Type(typeNodeOf(sourceNode).getName()));
		} else {
			varType = chainDots(enumClassDecl.getName());
		}
		final List<JCExpression> nilExp = List.nil();
		final JCNewClass init = setGeneratedBy(M.NewClass(null, nilExp, varType, nilExp, null), source);
		final JCVariableDecl enumContant = setGeneratedBy(M.VarDef(mods, name(node.getName()), varType, init), source);
		return enumContant;
	}

	@Override
	public JCTree visitEqual(final lombok.ast.Equal node, final Void p) {
		final JCBinary equal = setGeneratedBy(M.Binary(getCTCint(JCTree.class, node.isNotEqual() ? "NE": "EQ"), build(node.getLeft(), JCExpression.class), build(node.getRight(), JCExpression.class)), source);
		return equal;
	}

	@Override
	public JCTree visitFieldDecl(final lombok.ast.FieldDecl node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		final JCVariableDecl field = setGeneratedBy(M.VarDef(mods, name(node.getName()), build(node.getType(), JCExpression.class), build(node.getInitialization(), JCExpression.class)), source);
		return field;
	}

	@Override
	public JCTree visitFieldRef(final lombok.ast.FieldRef node, final Void p) {
		final Name fieldName = name(node.getName());
		if (node.getReceiver() == null) {
			return setGeneratedBy(M.Ident(fieldName), source);
		} else {
			return setGeneratedBy(M.Select(build(node.getReceiver(), JCExpression.class), fieldName), source);
		}
	}

	@Override
	public JCTree visitForeach(final lombok.ast.Foreach node, final Void p) {
		final JCEnhancedForLoop foreach = setGeneratedBy(M.ForeachLoop(build(node.getElementVariable(), JCVariableDecl.class), build(node.getCollection(), JCExpression.class), build(node.getAction(), JCStatement.class)), source);
		return foreach;
	}

	@Override
	public JCTree visitIf(final lombok.ast.If node, final Void p) {
		final JCIf ifStatement = setGeneratedBy(M.If(build(node.getCondition(), JCExpression.class), build(node.getThenStatement(), JCStatement.class), build(node.getElseStatement(), JCStatement.class)), source);
		return ifStatement;
	}

	@Override
	public JCTree visitInstanceOf(final lombok.ast.InstanceOf node, final Void p) {
		final JCInstanceOf instanceOf = setGeneratedBy(M.TypeTest(build(node.getExpression(), JCExpression.class), build(node.getType())), source);
		return instanceOf;
	}

	@Override
	public JCTree visitLocalDecl(final lombok.ast.LocalDecl node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		final JCVariableDecl local = setGeneratedBy(M.VarDef(mods, name(node.getName()), build(node.getType(), JCExpression.class), build(node.getInitialization(), JCExpression.class)), source);
		return local;
	}

	@Override
	public JCTree visitMethodDecl(final lombok.ast.MethodDecl node, final Void p) {
		final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(node.getModifiers()), build(node.getAnnotations(), JCAnnotation.class)), source);
		JCBlock body = null;
		if (!node.noBody() && ((mods.flags & Flags.ABSTRACT) == 0)) {
			body = setGeneratedBy(M.Block(0, build(node.getStatements(), JCStatement.class)), source);
		}
		final JCMethodDecl method = setGeneratedBy(M.MethodDef(mods, name(node.getName()), build(node.getReturnType(), JCExpression.class), build(node.getTypeParameters(), JCTypeParameter.class), build(node.getArguments(), JCVariableDecl.class), build(node.getThrownExceptions(), JCExpression.class), body, null), source);
		return method;
	}

	@Override
	public JCTree visitNameRef(final lombok.ast.NameRef node, final Void p) {
		return setGeneratedBy(chainDots(node.getName()), source);
	}

	@Override
	public JCTree visitNew(final lombok.ast.New node, final Void p) {
		final JCNewClass newClass = setGeneratedBy(M.NewClass(null, build(node.getTypeArgs(), JCExpression.class), build(node.getType(), JCExpression.class), build(node.getArgs(), JCExpression.class), build(node.getAnonymousType(), JCClassDecl.class)), source);
		return newClass;
	}

	@Override
	public JCTree visitNewArray(final lombok.ast.NewArray node, final Void p) {
		final ListBuffer<JCExpression> dims = ListBuffer.lb();
		dims.appendList(build(node.getDimensionExpressions(), JCExpression.class));
		final List<JCExpression> initializerExpressions = build(node.getInitializerExpressions(), JCExpression.class);
		JCNewArray newClass = setGeneratedBy(M.NewArray(build(node.getType(), JCExpression.class), dims.toList(), initializerExpressions.isEmpty() ? null : initializerExpressions), source);
		return newClass;
	}

	@Override
	public JCTree visitNullLiteral(final lombok.ast.NullLiteral node, final Void p) {
		final JCLiteral literal = setGeneratedBy(M.Literal(TypeTags.BOT, null), source);
		return literal;
	}

	@Override
	public JCTree visitNumberLiteral(final lombok.ast.NumberLiteral node, final Void p) {
		final JCLiteral literal = setGeneratedBy(M.Literal(node.getNumber()), source);
		return literal;
	}

	@Override
	public JCTree visitReturn(final lombok.ast.Return node, final Void p) {
		final JCReturn returnStatement = setGeneratedBy(M.Return(build(node.getExpression(), JCExpression.class)), source);
		return returnStatement;
	}

	@Override
	public JCTree visitReturnDefault(final lombok.ast.ReturnDefault node, final Void p) {
		lombok.ast.Return returnDefault = Return(Null());
		lombok.ast.TypeRef returnType = node.upTo(lombok.ast.MethodDecl.class).getReturnType();
		if (returnType == null) {
			returnType = Type(methodNodeOf(sourceNode).getName());
		}
		final JCExpression type = build(returnType);
		if (type instanceof JCPrimitiveTypeTree) {
			JCPrimitiveTypeTree primitiveType = (JCPrimitiveTypeTree) type;
			if (primitiveType.typetag == TypeTags.VOID) {
				returnDefault = Return();
			} else {
				returnDefault = Return(Expr(M.Literal(primitiveType.typetag, 0)));
			}
		}
		return build(returnDefault);
	}

	@Override
	public JCTree visitStringLiteral(final lombok.ast.StringLiteral node, final Void p) {
		final JCLiteral literal = setGeneratedBy(M.Literal(node.getString()), source);
		return literal;
	}

	@Override
	public JCTree visitSwitch(final lombok.ast.Switch node, final Void p) {
		final JCSwitch switchStatement = setGeneratedBy(M.Switch(build(node.getExpression(), JCExpression.class), build(node.getCases(), JCCase.class)), source);
		return switchStatement;
	}

	@Override
	public JCTree visitSynchronized(final lombok.ast.Synchronized node, final Void p) {
		final JCBlock block = setGeneratedBy(M.Block(0, build(node.getStatements(), JCStatement.class)), source);
		final JCSynchronized synchronizedStatemenet = setGeneratedBy(M.Synchronized(build(node.getLock(), JCExpression.class), block), source);
		return synchronizedStatemenet;
	}

	@Override
	public JCTree visitThis(final lombok.ast.This node, final Void p) {
		final Name thisName = name("this");
		if (node.getType() == null) {
			return setGeneratedBy(M.Ident(thisName), source);
		} else {
			return setGeneratedBy(M.Select(build(node.getType(), JCExpression.class), thisName), source);
		}
	}

	@Override
	public JCTree visitThrow(final lombok.ast.Throw node, final Void p) {
		final JCThrow throwStatement = setGeneratedBy(M.Throw(build(node.getExpression(), JCExpression.class)), source);
		return throwStatement;
	}

	@Override
	public JCTree visitTry(final lombok.ast.Try node, final Void p) {
		final ListBuffer<JCCatch> catchers = ListBuffer.lb();
		final Iterator<lombok.ast.Argument> iter = node.getCatchArguments().iterator();
		for (lombok.ast.Block catchBlock : node.getCatchBlocks()) {
			lombok.ast.Argument catchArgument = iter.next();
			final JCModifiers mods = setGeneratedBy(M.Modifiers(flagsFor(catchArgument.getModifiers()), build(catchArgument.getAnnotations(), JCAnnotation.class)), source);
			catchers.append(M.Catch(M.VarDef(mods, name(catchArgument.getName()), build(catchArgument.getType(), JCExpression.class), null), build(catchBlock, JCBlock.class)));
		}
		final JCTry tryStatement = setGeneratedBy(M.Try(build(node.getTryBlock(), JCBlock.class), catchers.toList(), build(node.getFinallyBlock(), JCBlock.class)), source);
		return tryStatement;
	}

	@Override
	public JCTree visitTypeParam(final lombok.ast.TypeParam node, final Void p) {
		JCTypeParameter typeParam = setGeneratedBy(M.TypeParameter(name(node.getName()), build(node.getBounds(), JCExpression.class)), source);
		return typeParam;
	}

	@Override
	public JCTree visitTypeRef(final lombok.ast.TypeRef node, final Void p) {
		JCExpression typeRef;
		final String typeName = node.getTypeName();
		if ("void".equals(typeName)) {
			return setGeneratedBy(M.TypeIdent(TypeTags.VOID), source);
		} else if ("int".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.INT);
		} else if ("long".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.LONG);
		} else if ("short".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.SHORT);
		} else if ("boolean".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.BOOLEAN);
		} else if ("byte".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.BYTE);
		} else if ("char".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.CHAR);
		} else if ("float".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.FLOAT);
		} else if ("double".equals(typeName)) {
			typeRef = M.TypeIdent(TypeTags.DOUBLE);
		} else {
			typeRef = chainDots(node.getTypeName());
			if (!node.getTypeArgs().isEmpty()) {
				typeRef = M.TypeApply(setGeneratedBy(typeRef, source), build(node.getTypeArgs(), JCExpression.class));
			}
		}
		typeRef = setGeneratedBy(typeRef, source);
		for (int i = 0; i < node.getDims(); i++) {
			typeRef = setGeneratedBy(M.TypeArray(typeRef), source);
		}
		return typeRef;
	}

	@Override
	public JCTree visitUnary(final lombok.ast.Unary node, final Void p) {
		final String operator = node.getOperator();
		final int opcode;
		if ("!".equals(operator)) {
			opcode = getCTCint(JCTree.class, "NOT");
		} else if ("+".equals(operator)) {
			opcode = getCTCint(JCTree.class, "PLUS");
		} else if ("-".equals(operator)) {
			opcode = getCTCint(JCTree.class, "MINUS");
		} else {
			throw new IllegalStateException(String.format("Unknown binary operator '%s'", operator));
		}
		JCUnary unary = setGeneratedBy(M.Unary(opcode, build(node.getExpression(), JCExpression.class)), source);
		return unary;
	}

	@Override
	public JCTree visitWhile(final lombok.ast.While node, final Void p) {
		final JCWhileLoop whileLoop = setGeneratedBy(M.WhileLoop(build(node.getCondition(), JCExpression.class), build(node.getAction(), JCStatement.class)), source);
		return whileLoop;
	}

	@Override
	public JCTree visitWildcard(final lombok.ast.Wildcard node, final Void p) {
		BoundKind boundKind = BoundKind.UNBOUND;
		if (node.getBound() != null) {
			switch(node.getBound()) {
			case SUPER:
				boundKind = BoundKind.SUPER;
				break;
			default:
			case EXTENDS:
				boundKind = BoundKind.EXTENDS;
			}
		}
		final TypeBoundKind kind = setGeneratedBy(M.TypeBoundKind(boundKind), source);
		final JCWildcard wildcard = setGeneratedBy(M.Wildcard(kind, build(node.getType(), JCExpression.class)), source);
		return wildcard;
	}

	@Override
	public JCTree visitWrappedExpression(final lombok.ast.WrappedExpression node, final Void p) {
		final JCExpression expression = (JCExpression) node.getWrappedObject();
		return expression;
	}

	@Override
	public JCTree visitWrappedMethodDecl(final lombok.ast.WrappedMethodDecl node, final Void p) {
		MethodSymbol methodSymbol = (MethodSymbol) node.getWrappedObject();
		Type mtype = methodSymbol.type;

		if (node.getReturnType() == null) {
			node.withReturnType(Type(fixLeadingDot(M.Type(mtype.getReturnType()))));
		}
		if (node.getThrownExceptions().isEmpty()) for (JCExpression expr : M.Types(mtype.getThrownTypes())) {
			node.withThrownException(Type(fixLeadingDot(expr)));
		}
		if (node.getArguments().isEmpty()) for (JCVariableDecl param : M.Params(mtype.getParameterTypes(), methodSymbol)) {
			node.withArgument(Arg(Type(fixLeadingDot(param.vartype)), param.name.toString()));
		}
		if (node.getTypeParameters().isEmpty()) for (JCTypeParameter typaram : M.TypeParams(mtype.getTypeArguments())) {
			final lombok.ast.TypeParam typeParam = TypeParam(typaram.name.toString());
			for (JCExpression expr : typaram.bounds) {
				typeParam.withBound(Type(fixLeadingDot(expr)));
			}
			node.withTypeParameter(typeParam);
		}

		final JCModifiers mods = M.Modifiers(methodSymbol.flags() & (~Flags.ABSTRACT), build(node.getAnnotations(), JCAnnotation.class));
		final JCExpression restype = build(node.getReturnType());
		final Name name = methodSymbol.name;
		final List<JCExpression> thrown = build(node.getThrownExceptions(), JCExpression.class);
		final List<JCTypeParameter> typarams = build(node.getTypeParameters(), JCTypeParameter.class);
		final List<JCVariableDecl> params = build(node.getArguments(), JCVariableDecl.class);
		JCBlock body = null;
		if (!node.noBody()) {
			body = M.Block(0, build(node.getStatements(), JCStatement.class));
		}
		final JCMethodDecl method = M.MethodDef(mods, name, restype, typarams, params, thrown, body, null);
		return method;
	}

	@Override
	public JCTree visitWrappedStatement(final lombok.ast.WrappedStatement node, final Void p) {
		final JCStatement statement = new TreeCopier<Void>(M).copy((JCStatement) node.getWrappedObject());
		return statement;
	}

	@Override
	public JCTree visitWrappedTypeRef(final lombok.ast.WrappedTypeRef node, final Void p) {
		JCExpression typeReference = null;
		if (node.getWrappedObject() instanceof Type) {
			typeReference = fixLeadingDot(M.Type((Type) node.getWrappedObject()));
		} else if (node.getWrappedObject() instanceof JCExpression) {
			typeReference = new TreeCopier<Void>(M).copy((JCExpression) node.getWrappedObject());
		}
		return typeReference;
	}
}
