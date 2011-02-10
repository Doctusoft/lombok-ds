package lombok.eclipse.handlers.ast;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public final class LocalDeclarationBuilder extends AbstractVariableDeclarationBuilder<LocalDeclarationBuilder, LocalDeclaration> {
	
	LocalDeclarationBuilder(ExpressionBuilder<? extends TypeReference> type, String name) {
		super(type, name);
	}

	@Override
	public LocalDeclaration build(final EclipseNode node, final ASTNode source) {
		LocalDeclaration proto = new LocalDeclaration(name.toCharArray(), 0, 0);
		setGeneratedByAndCopyPos(proto, source);
		proto.modifiers = modifiers;
		proto.annotations = buildArray(annotations, new Annotation[0], node, source);
		proto.bits |= bits | ECLIPSE_DO_NOT_TOUCH_FLAG;
		proto.type = type.build(node, source);
		if (initialization != null) {
			proto.initialization = initialization.build(node, source);
		}
		return proto;
	}
}
