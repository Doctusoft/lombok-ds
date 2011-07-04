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

import static lombok.ast.AST.*;
import static lombok.javac.handlers.Javac.typeNodeOf;
import static lombok.javac.handlers.JavacHandlerUtil.*;
import static com.sun.tools.javac.code.Flags.*;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacASTVisitor;
import lombok.javac.JavacNode;
import lombok.javac.handlers.ast.JavacType;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.ListBuffer;
import org.mangosdk.spi.ProviderFor;

public class HandleEntrypoint {
	/**
	 * Handles the {@code lombok.Application} interface for javac.
	 */
	@ProviderFor(JavacASTVisitor.class)
	public static class HandleApplication extends AbstractHandleEntrypoint {
		public HandleApplication() {
			super(Application.class);
		}

		@Override protected void handle(JavacType type) {
			markInterfaceAsProcessed(type.node(), Application.class);
			createEntrypoint(type, "main", "runApp", new ParameterProvider(), new ArgumentProvider());
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<lombok.ast.Expression> getArgs(String name) {
				List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
				args.add(Name("args"));
				return args;
			}
		}

		private static class ParameterProvider implements IParameterProvider {
			@Override public List<lombok.ast.Argument> getParams(String name) {
				List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
				params.add(Arg(Type("java.lang.String").withDimensions(1), "args"));
				return params;
			}
		}
	}

	/**
	 * Handles the {@code lombok.JvmAgent} interface for javac.
	 */
	@ProviderFor(JavacASTVisitor.class)
	public static class HandleJvmAgent extends AbstractHandleEntrypoint {
		public HandleJvmAgent() {
			super(JvmAgent.class);
		}

		@Override protected void handle(JavacType type) {
			markInterfaceAsProcessed(type.node(), JvmAgent.class);
			IArgumentProvider argumentProvider = new ArgumentProvider();
			IParameterProvider parameterProvider = new ParameterProvider();
			createEntrypoint(type, "agentmain", "runAgent", parameterProvider, argumentProvider);
			createEntrypoint(type, "premain", "runAgent", parameterProvider, argumentProvider);
		}

		private static class ArgumentProvider implements IArgumentProvider {
			@Override public List<lombok.ast.Expression> getArgs(String name) {
				List<lombok.ast.Expression> args = new ArrayList<lombok.ast.Expression>();
				args.add(("agentmain".equals(name) ? True() : False()));
				args.add(Name("params"));
				args.add(Name("instrumentation"));
				return args;
			}
		}

		private static class ParameterProvider implements IParameterProvider {
			@Override public List<lombok.ast.Argument> getParams(String name) {
				List<lombok.ast.Argument> params = new ArrayList<lombok.ast.Argument>();
				params.add(Arg(Type("java.lang.String"), "params"));
				params.add(Arg(Type("java.lang.instrument.Instrumentation"), "instrumentation"));
				return params;
			}
		}
	}

	@RequiredArgsConstructor
	public static abstract class AbstractHandleEntrypoint extends JavacASTAdapter {
		private final Class<?> interfaze;

		@Override public void visitType(JavacNode typeNode, JCClassDecl type) {
			boolean implementsInterface = false;
			boolean isAnImport = typeNode.getImportStatements().contains(interfaze.getName());
			if (type.implementing != null) for (JCExpression exp : type.implementing) {
				if (exp.toString().equals(interfaze.getName()) || (isAnImport && exp.toString().equals(interfaze.getSimpleName()))) {
					implementsInterface = true;
					break;
				}
			}
			if (implementsInterface) {
				 handle(JavacType.typeOf(typeNode, type));
			}
		}

		@Override public void endVisitCompilationUnit(JavacNode top, JCCompilationUnit unit) {
			deleteImportFromCompilationUnit(top, interfaze.getName());
		}

		/**
		 * Called when an interface is found that is likely to match the interface you're interested in.
		 *
		 * @param type
		 */
		protected abstract void handle(JavacType type);
	}

	/**
	 * Removes the interface from javac's AST (it remains in lombok's AST),
	 * then removes any import statement that imports this exact interface (not star imports).
	 */
	public static void markInterfaceAsProcessed(JavacNode typeNode, Class<?> interfazeType) {
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl)typeNode.get();
		if (typeDecl != null) {
			ListBuffer<JCExpression> newImplementing = ListBuffer.lb();
			for (JCExpression exp : typeDecl.implementing) {
				if (!(exp.toString().equals(interfazeType.getName()) || exp.toString().equals(interfazeType.getSimpleName()))) newImplementing.append(exp);
			}
			typeDecl.implementing = newImplementing.toList();
		}
	}

	/**
	 * Checks if there is an entry point with the provided name.
	 *
	 * @param methodName the entry point name to check for.
	 * @param node Any node that represents the Type (JCClassDecl) to look in, or any child node thereof.
	 */
	public static boolean entrypointExists(String methodName, JavacNode node) {
		JavacNode typeNode = typeNodeOf(node);
		JCClassDecl typeDecl = (JCClassDecl)typeNode.get();
		for (JCTree def : typeDecl.defs) {
			if (def instanceof JCMethodDecl) {
				JCMethodDecl method = (JCMethodDecl)def;
				boolean nameMatches = method.name.toString().equals(methodName);
				boolean returnTypeIsVoid = (method.restype != null) && "void".equals(method.restype.toString());
				boolean isPublicStatic = (method.mods != null) && ((method.mods.flags & (PUBLIC | STATIC)) != 0);
				if (nameMatches && returnTypeIsVoid && isPublicStatic) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Creates an entrypoint like this:
	 * <pre>
	 * public static void &lt;NAME&gt;(&lt;PARAMETER&gt;) throws java.lang.Throwable {
	 *   new &lt;TYPENAME&gt;().&lt;METHODNAME&gt;(&lt;ARGUMENTS&gt;);
	 * }
	 * </pre>
	 * @param type Type (JCClassDecl)
	 * @param name name of the entrypoint ("main", "premain, "agentmain")
	 * @param methodName name of method that should be called in the entrypoint
	 * @param paramProvider parameter provider used for the entrypoint
	 * @param argsProvider argument provider used for the constructor
	 */
	public static void createEntrypoint(JavacType type, String name, String methodName, IParameterProvider paramProvider, IArgumentProvider argsProvider) {
		if (!type.hasMethod(methodName)) {
			return;
		}

		if (entrypointExists(name, type.node())) {
			return;
		}

		type.injectMethod(MethodDecl(Type("void"), name).makePublic().makeStatic().withArguments(paramProvider.getParams(name)).withThrownException(Type("java.lang.Throwable")) //
				.withStatement(Call(New(Type(type.name())), methodName).withArguments(argsProvider.getArgs(name))));
	}

	public static interface IArgumentProvider {
		public List<lombok.ast.Expression> getArgs(String name);
	}

	public static interface IParameterProvider {
		public List<lombok.ast.Argument> getParams(String name);
	}
}
