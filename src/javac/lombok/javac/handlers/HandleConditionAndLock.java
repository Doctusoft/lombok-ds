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

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static lombok.javac.handlers.JavacTreeBuilder.*;

import java.lang.annotation.Annotation;

import lombok.Await;
import lombok.AwaitBeforeAndSignalAfter;
import lombok.Position;
import lombok.ReadLock;
import lombok.Signal;
import lombok.WriteLock;
import lombok.core.AnnotationValues;
import lombok.core.AST.Kind;
import lombok.javac.handlers.JavacHandlerUtil.MemberExistsResult;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

public class HandleConditionAndLock {
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleReadLock extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<ReadLock> {
		@Override public boolean handle(AnnotationValues<ReadLock> annotation, JCAnnotation ast, JavacNode annotationNode) {
			ReadLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("readLock")
					.handle(ann.value(), ReadLock.class, ast, annotationNode);
		}
	}
	
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleWriteLock extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<WriteLock> {
		@Override public boolean handle(AnnotationValues<WriteLock> annotation, JCAnnotation ast, JavacNode annotationNode) {
			WriteLock ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withLockMethod("writeLock")
					.handle(ann.value(), WriteLock.class, ast, annotationNode);
		}
	}
	
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleSignal extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<Signal> {
		@Override public boolean handle(AnnotationValues<Signal> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Signal ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withSignal(new SignalData(ann.value(), ann.pos()))
					.handle(ann.lockName(), Signal.class, ast, annotationNode);
		}
	}
	 
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwait extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<Await> {
		@Override public boolean handle(AnnotationValues<Await> annotation, JCAnnotation ast, JavacNode annotationNode) {
			Await ann = annotation.getInstance();
			return new HandleConditionAndLock()
					.withAwait(new AwaitData(ann.value(), ann.conditionMethod(), ann.pos()))
					.handle(ann.lockName(), Await.class, ast, annotationNode);
		}
	}
	
	@ProviderFor(JavacAnnotationHandler.class)
	public static class HandleAwaitBeforeAndSignalAfter extends JavacNonResolutionBasedHandler implements JavacAnnotationHandler<AwaitBeforeAndSignalAfter> {
		@Override public boolean handle(AnnotationValues<AwaitBeforeAndSignalAfter> annotation, JCAnnotation ast, JavacNode annotationNode) {
			AwaitBeforeAndSignalAfter ann = annotation.getInstance(); 
			return new HandleConditionAndLock()
					.withAwait(new AwaitData(ann.awaitConditionName(), ann.awaitConditionMethod(), Position.BEFORE))
					.withSignal(new SignalData(ann.signalConditionName(), Position.AFTER))
					.handle(ann.lockName(), AwaitBeforeAndSignalAfter.class, ast, annotationNode);
		}
	}
	
	private AwaitData await;
	private SignalData signal;
	private String lockMethod;
	
	public HandleConditionAndLock withAwait(final AwaitData await) {
		this.await = await;
		return this;
	}
	
	public HandleConditionAndLock withSignal(final SignalData signal) {
		this.signal = signal;
		return this;
	}
	
	public HandleConditionAndLock withLockMethod(final String lockMethod) {
		this.lockMethod = lockMethod;
		return this;
	}
	
	public boolean handle(String lockName, Class<? extends Annotation> annotationType, JCAnnotation ast, JavacNode annotationNode) {
		markAnnotationAsProcessed(annotationNode, annotationType);
		String annotationTypeName = annotationType.getSimpleName();
		if (isNoConcreteMethod(annotationTypeName, annotationNode)) {
			return false;
		}
		
		boolean lockMode = lockMethod != null;
		
		if (!lockMode && (await == null) && (signal == null)) {
			return false; // wrong configured handler, so better stop here
		}
		
		JavacNode methodNode = annotationNode.up();
		JCMethodDecl method = (JCMethodDecl)methodNode.get();
		String completeLockName = createCompleteLockName(lockName);
		
		if (!tryToAddLockField(lockMode ? lockName : completeLockName, lockMode, annotationTypeName, annotationNode)) {
			return false;
		}
		
		StringBuilder beforeMethodBlock = new StringBuilder();
		StringBuilder afterMethodBlock = new StringBuilder();
		
		if (!lockMode) {
			if (!getConditionStatements(await, completeLockName, annotationTypeName, annotationNode, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
			if (!getConditionStatements(signal, completeLockName, annotationTypeName, annotationNode, beforeMethodBlock, afterMethodBlock)) {
				return false;
			}
		}
		
		TreeMaker maker = methodNode.getTreeMaker();
		method.body = maker.Block(0, statements(methodNode, "this.%s.lock(); try { %s %s %s } finally { this.%s.unlock(); }",
				completeLockName, beforeMethodBlock, removeCurlyBrackets(method.body.toString()), afterMethodBlock, completeLockName));
		if (await != null) {
			method.thrown = method.thrown.append(chainDotsString(maker, methodNode, "java.lang.InterruptedException"));
		}
		
		methodNode.rebuild();
		
		return true;
	}
	
	private boolean getConditionStatements(ConditionData condition, String lockName, String annotationTypeName, JavacNode node, StringBuilder before, StringBuilder after) {
		if (condition == null) {
			return true;
		}
		if (tryToAddConditionField(condition.condition, lockName, annotationTypeName, node)) {
			switch (condition.pos) {
			case BEFORE:
				before.append(condition);
				break;
			default:
			case AFTER:
				after.append(condition);
				break;
			}
			return true;
		}
		return false;
	}
	
	private String createCompleteLockName(String lockName) {
		String completeLockName;
		if (lockMethod != null) {
			completeLockName = lockName + "." + lockMethod + "()";
		} else {
			if (trim(lockName).isEmpty()) {
				String awaitCondition = trim(await == null ? "" : await.condition);
				String signalCondition = trim(signal == null ? "" : signal.condition);
				if (!awaitCondition.isEmpty()) signalCondition = capitalize(signalCondition);
				completeLockName = "$";
				completeLockName += awaitCondition;
				completeLockName += signalCondition;
				completeLockName += "Lock";
			} else {
				completeLockName = lockName;
			}
		}
		return completeLockName;
	}
	
	private static boolean tryToAddLockField(String lockName, boolean isReadWriteLock, String annotationTypeName, JavacNode annotationNode) {
		lockName = trim(lockName);
		if (lockName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'lockName' may not be empty or null.", annotationTypeName));
			return false;
		}
		JavacNode methodNode = annotationNode.up();
		if (fieldExists(lockName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			if(isReadWriteLock) {
				addReadWriteLockField(methodNode, lockName);
			} else {
				addLockField(methodNode, lockName);	
			}
		} else {
			// TODO type check
			// java.util.concurrent.locks.ReadWriteLock
			// java.util.concurrent.locks.Lock
		}
		return true;
	}
	
	private static boolean tryToAddConditionField(String conditionName, String lockName, String annotationTypeName, JavacNode annotationNode) {
		conditionName = trim(conditionName);
		if (conditionName.isEmpty()) {
			annotationNode.addError(String.format("@%s 'conditionName' may not be empty or null.", annotationTypeName));
			return false;
		}
		JavacNode methodNode = annotationNode.up();
		if (fieldExists(conditionName, methodNode) == MemberExistsResult.NOT_EXISTS) {
			addConditionField(methodNode, conditionName, lockName);
		} else {
			// TODO type check
			// java.util.concurrent.locks.Condition
		}
		return true;
	}
	
	private static boolean isNoConcreteMethod(String annotationTypeName, JavacNode annotationNode) {
		JavacNode methodNode = annotationNode.up();
		if (methodNode == null || methodNode.getKind() != Kind.METHOD || !(methodNode.get() instanceof JCMethodDecl)) {
			annotationNode.addError(String.format("@%s is legal only on methods.", annotationTypeName));
			return true;
		}
		JCMethodDecl method = (JCMethodDecl)methodNode.get();
		if (((method.mods.flags & ABSTRACT) != 0) || ((method.body == null))) {
			annotationNode.addError(String.format("@%s is legal only on concrete methods.", annotationTypeName));
			return true;
		}
		return false;
	}
	
	private static void addLockField(JavacNode node, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.Lock %s = new java.util.concurrent.locks.ReentrantLock();", lockName).inject();
	}
	
	private static void addReadWriteLockField(JavacNode node, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.ReadWriteLock %s = new java.util.concurrent.locks.ReentrantReadWriteLock();", lockName).inject();
	}
	
	private static void addConditionField(JavacNode node, String conditionName, String lockName) {
		field(node.up(), "private final java.util.concurrent.locks.Condition %s = %s.newCondition();", conditionName, lockName).inject();
	}
	
	private static String removeCurlyBrackets(String s) {
		int startIndex = s.indexOf("{");
		if (startIndex < 0) return s;
		int endIndex = s.lastIndexOf("}");
		if (endIndex <= 0) return s;
		s = s.substring(startIndex + 1, endIndex - 1);
		return s;
	}
	
	private static String trim(String s) {
		if (s == null) return "";
		else return s.trim();
	}
	
	private String capitalize(String s) {
		if (trim(s).length() < 1) return s;
		else return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	private static class AwaitData extends ConditionData {
		public final String conditionMethod;
		
		public AwaitData(final String condition, final String conditionMethod, final Position pos) {
			super(condition, pos);
			this.conditionMethod = conditionMethod;
		}
		
		public String toString() {
			return String.format("while(this.%s()) { this.%s.await();}", conditionMethod, condition);
		}
	}
	
	private static class SignalData extends ConditionData {
		public SignalData(final String condition, final Position pos) {
			super(condition, pos);
		}
		
		public String toString() {
			return String.format("this.%s.signal();", condition);
		}
	}
	
	private static abstract class ConditionData {
		public final String condition;
		public final Position pos;
		
		public ConditionData(final String condition, final Position pos) {
			super();
			this.condition = condition;
			this.pos = pos;
		}
	}
	
	private static abstract class JavacNonResolutionBasedHandler {
		public final boolean isResolutionBased() {
			return false;
		} 
	}
}
