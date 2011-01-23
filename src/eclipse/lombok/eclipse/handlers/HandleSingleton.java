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
package lombok.eclipse.handlers;

import static lombok.core.util.ErrorMessages.*;
import static lombok.core.util.Arrays.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseNodeBuilder.*;
import static org.eclipse.jdt.core.dom.Modifier.*;
import static org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.*;

import lombok.Singleton;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleSingleton implements EclipseAnnotationHandler<Singleton> {
	@Override public boolean handle(AnnotationValues<Singleton> annotation, Annotation source, EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();
		TypeDeclaration type = null;
		if (typeNode.get() instanceof TypeDeclaration) type = (TypeDeclaration) typeNode.get();
		int modifiers = type == null ? 0 : type.modifiers;
		boolean notAClass = (modifiers & (AccInterface | AccAnnotation | AccEnum)) != 0;
		if (type == null || notAClass) {
			annotationNode.addError(canBeUsedOnClassOnly(Singleton.class));
			return true;
		}
		if (type.superclass != null) {
			annotationNode.addError(canBeUsedOnConcreteClassOnly(Singleton.class));
			return true;
		}
		if (hasMultiArgumentConstructor(type)) {
			annotationNode.addError(requiresDefaultOrNoArgumentConstructor(Singleton.class));
			return true;
		}

		
		type.modifiers |= 0x00004000; // Modifier.ENUM
		replaceConstructorVisibility(type);
		
		AllocationExpression initialization = new AllocationExpression();
		setGeneratedByAndCopyPos(initialization, source);
		initialization.enumConstant = field(typeNode, source, 0, (TypeReference)null, "INSTANCE")
			.withInitialization(initialization).build();
		
		injectField(typeNode, initialization.enumConstant);
		
		typeNode.rebuild();
		
		return true;
	}
	
	private void replaceConstructorVisibility(TypeDeclaration type) {
		if (isNotEmpty(type.methods)) for (AbstractMethodDeclaration def : type.methods) {
			if (def instanceof ConstructorDeclaration) def.modifiers &= ~(PUBLIC | PROTECTED);
		}
	}
	
	private boolean hasMultiArgumentConstructor(TypeDeclaration type) {		
		if (isNotEmpty(type.methods)) for (AbstractMethodDeclaration def : type.methods) {
			if ((def instanceof ConstructorDeclaration) && isNotEmpty(def.arguments)) return true;
		}
		return false;
	}
}
