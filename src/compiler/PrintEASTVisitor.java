package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void,VoidException> {

	public PrintEASTVisitor() { super(false,true); }

	/**
	 *
	 * @param node ProgLetInNode
	 * @return null
	 */
	@Override
	public Void visitNode(ProgLetInNode node) {
        this.printNode(node);
		for (Node declaration : node.declarationlist) this.visit(declaration);
        this.visit(node.exp);
		return null;
	}

	/**
	 *
	 * @param node ProgNode
	 * @return null
	 */
	@Override
	public Void visitNode(ProgNode node) {
        this.printNode(node);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node FunNode
	 * @return null
	 */
	@Override
	public Void visitNode(FunNode node) {
        this.printNode(node,node.id);
        this.visit(node.returnType);
		for (ParNode parameter : node.parameterlist) this.visit(parameter);
		for (Node declaration : node.declarationlist) this.visit(declaration);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node ParNode
	 * @return null
	 */
	@Override
	public Void visitNode(ParNode node) {
        this.printNode(node,node.id);
        this.visit(node.getType());
		return null;
	}

	/**
	 *
	 * @param node VarNode
	 * @return null
	 */
	@Override
	public Void visitNode(VarNode node) {
        this.printNode(node,node.id);
        this.visit(node.getType());
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node PrintNode
	 * @return null
	 */
	@Override
	public Void visitNode(PrintNode node) {
        this.printNode(node);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node IfNode
	 * @return null
	 */
	@Override
	public Void visitNode(IfNode node) {
        this.printNode(node);
        this.visit(node.cond);
        this.visit(node.thenNode);
        this.visit(node.elseNode);
		return null;
	}

	/**
	 *
	 * @param node EqualNode
	 * @return null
	 */
	@Override
	public Void visitNode(EqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node TimesNode
	 * @return null
	 */
	@Override
	public Void visitNode(TimesNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node PlusNode
	 * @return null
	 */
	@Override
	public Void visitNode(PlusNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node CallNode
	 * @return null
	 */
	@Override
	public Void visitNode(CallNode node) {
        this.printNode(node,node.id+" at nestinglevel "+node.nestingLevel);
        this.visit(node.entry);
		for (Node arg : node.argumentList) this.visit(arg);
		return null;
	}

	/**
	 *
	 * @param node IdNode
	 * @return null
	 */
	@Override
	public Void visitNode(IdNode node) {
        this.printNode(node,node.id+" at nestinglevel "+node.nestingLevel);
        this.visit(node.entry);
		return null;
	}

	/**
	 *
	 * @param node BoolNode
	 * @return null
	 */
	@Override
	public Void visitNode(BoolNode node) {
        this.printNode(node,node.value.toString());
		return null;
	}

	/**
	 *
	 * @param node IntNode
	 * @return null
	 */
	@Override
	public Void visitNode(IntNode node) {
        this.printNode(node,node.value.toString());
		return null;
	}

	/**
	 *
	 * @param node ArrowTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(ArrowTypeNode node) {
        this.printNode(node);
		for (Node par: node.parameterList) this.visit(par);
        this.visit(node.returnType,"->"); //marks return type
		return null;
	}

	/**
	 *
	 * @param node BoolTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(BoolTypeNode node) {
        this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param node IntTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(IntTypeNode node) {
        this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param entry STentry
	 * @return null
	 */
	@Override
	public Void visitSTentry(STentry entry) {
        this.printSTentry("nestlev "+entry.nl);
        this.printSTentry("type");
        this.visit(entry.type);
        this.printSTentry("offset "+entry.offset);
		return null;
	}

	/**
	 *
	 * @param node MinusNode
	 * @return null
	 */
	@Override
	public Void visitNode(MinusNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node DivNode
	 * @return null
	 */
	@Override
	public Void visitNode(DivNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node LessEqualNode
	 * @return null
	 */
	@Override
	public Void visitNode(LessEqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node GreaterEqualNode
	 * @return null
	 */
	@Override
	public Void visitNode(GreaterEqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node AndNode
	 * @return null
	 */
	@Override
	public Void visitNode(AndNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node OrNode
	 * @return null
	 */
	@Override
	public Void visitNode(OrNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node NotNode
	 * @return null
	 */
	@Override
	public Void visitNode(NotNode node) {
        this.printNode(node);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node ClassNode
	 * @return null
	 */
	@Override
	public Void visitNode(ClassNode node) {
        this.printNode(node, node.classId + " extends " + node.superClassId.orElse("nothing"));
		for (Node field : node.fieldList) this.visit(field);
		for (Node method : node.methodList) this.visit(method);
		return null;
	}

	/**
	 *
	 * @param node FieldNode
	 * @return null
	 */
	@Override
	public Void visitNode(FieldNode node) {
        this.printNode(node, node.id + " Offset: " + node.offset);
        this.visit(node.getType());
		return null;
	}

	/**
	 *
	 * @param node MethodNode
	 * @return null
	 */
	@Override
	public Void visitNode(MethodNode node) {
        this.printNode(node, node.id + " Offset: " + node.offset);
        this.visit(node.returnType);
		for (Node parameter : node.parameterList) this.visit(parameter);
		for (Node declaration : node.declarationList) this.visit(declaration);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node ClassCallNode
	 * @return null
	 */
	@Override
	public Void visitNode(ClassCallNode node) {
        this.printNode(node, node.objectId + "." + node.objectId + " at nestinglevel: " + node. nestingLevel);
        this.visit(node.symbolTableEntry);
        this.visit(node.methodEntry);
		for (Node argument : node.argumentList) this.visit(argument);
		return null;
	}

	/**
	 *
	 * @param node NewNode
	 * @return null
	 */
	@Override
	public Void visitNode(NewNode node) {
        this.printNode(node, node.classId + " at nestinglevel: " + node.classSymbolTableEntry.nl);
		for (Node argument : node.argumentList) this.visit(argument);
        this.visit(node.classSymbolTableEntry);
		return null;
	}

	/**
	 *
	 * @param node EmptyNode
	 * @return null
	 */
	@Override
	public Void visitNode(EmptyNode node) {
        this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param node ClassTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(ClassTypeNode node) {
        this.printNode(node);
		for (Node field : node.fieldList) this.visit(field);
		for (Node method : node.methodList) this.visit(method);
		return null;
	}

	/**
	 *
	 * @param node MethodTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(MethodTypeNode node) {
        this.printNode(node);
		for (Node parameter : node.functionalType.parameterList) this.visit(parameter);
        this.visit(node.functionalType.returnType, "->"); //marks return type
		return null;
	}

	/**
	 *
	 * @param node RefTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(RefTypeNode node) {
        this.printNode(node, node.typeId);
		return null;
	}

	/**
	 *
	 * @param node EmptyTypeNode
	 * @return null
	 */
	@Override
	public Void visitNode(EmptyTypeNode node) {
        this.printNode(node);
		return null;
	}

}
