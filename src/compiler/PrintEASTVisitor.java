package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void,VoidException> {

	public PrintEASTVisitor() { super(false,true); }

	@Override
	public Void visitNode(ProgLetInNode node) {
        this.printNode(node);
		for (Node declaration : node.declarationlist) this.visit(declaration);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(ProgNode node) {
        this.printNode(node);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(FunNode node) {
        this.printNode(node,node.id);
        this.visit(node.retType);
		for (ParNode parameter : node.parameterlist) this.visit(parameter);
		for (Node declaration : node.declarationlist) this.visit(declaration);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(ParNode node) {
        this.printNode(node,node.id);
        this.visit(node.getType());
		return null;
	}

	@Override
	public Void visitNode(VarNode node) {
        this.printNode(node,node.id);
        this.visit(node.getType());
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(PrintNode node) {
        this.printNode(node);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode node) {
        this.printNode(node);
        this.visit(node.cond);
        this.visit(node.thenNode);
        this.visit(node.elseNode);
		return null;
	}

	@Override
	public Void visitNode(EqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(TimesNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode node) {
        this.printNode(node,node.id+" at nestinglevel "+node.nestingLevel);
        this.visit(node.entry);
		for (Node arg : node.argumentList) this.visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode node) {
        this.printNode(node,node.id+" at nestinglevel "+node.nestingLevel);
        this.visit(node.entry);
		return null;
	}

	@Override
	public Void visitNode(BoolNode node) {
        this.printNode(node,node.value.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode node) {
        this.printNode(node,node.value.toString());
		return null;
	}
	
	@Override
	public Void visitNode(ArrowTypeNode node) {
        this.printNode(node);
		for (Node par: node.parameterList) this.visit(par);
        this.visit(node.returnType,"->"); //marks return type
		return null;
	}

	@Override
	public Void visitNode(BoolTypeNode node) {
        this.printNode(node);
		return null;
	}

	@Override
	public Void visitNode(IntTypeNode node) {
        this.printNode(node);
		return null;
	}
	
	@Override
	public Void visitSTentry(STentry entry) {
        this.printSTentry("nestlev "+entry.nl);
        this.printSTentry("type");
        this.visit(entry.type);
        this.printSTentry("offset "+entry.offset);
		return null;
	}



	@Override
	public Void visitNode(MinusNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode node) {
        this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode node) {
        this.printNode(node);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(ClassNode node) {
        this.printNode(node, node.classId + " extends " + node.superId.orElse("nothing"));
		for (Node field : node.fieldList) this.visit(field);
		for (Node method : node.methodList) this.visit(method);
		return null;
	}

	@Override
	public Void visitNode(FieldNode node) {
        this.printNode(node, node.id + " Offset: " + node.offset);
        this.visit(node.getType());
		return null;
	}

	@Override
	public Void visitNode(MethodNode node) {
        this.printNode(node, node.id + " Offset: " + node.offset);
        this.visit(node.returnType);
		for (Node parameter : node.parameterList) this.visit(parameter);
		for (Node declaration : node.declarationList) this.visit(declaration);
        this.visit(node.exp);
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode node) {
        this.printNode(node, node.objectId + "." + node.objectId + " at nestinglevel: " + node. nestingLevel);
        this.visit(node.entry);
        this.visit(node.methodEntry);
		for (Node argument : node.argumentList) this.visit(argument);
		return null;
	}

	@Override
	public Void visitNode(NewNode node) {
        this.printNode(node, node.classId + " at nestinglevel: " + node.entry.nl);
		for (Node argument : node.argumentList) this.visit(argument);
        this.visit(node.entry);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode node) {
        this.printNode(node);
		return null;
	}

	@Override
	public Void visitNode(ClassTypeNode node) {
        this.printNode(node);
		for (Node field : node.fieldList) this.visit(field);
		for (Node method : node.methodList) this.visit(method);
		return null;
	}

	@Override
	public Void visitNode(MethodTypeNode node) {
        this.printNode(node);
		for (Node parameter : node.functionalType.parameterList) this.visit(parameter);
        this.visit(node.functionalType.returnType, "->"); //marks return type
		return null;
	}

	@Override
	public Void visitNode(RefTypeNode node) {
        this.printNode(node, node.typeId);
		return null;
	}

	@Override
	public Void visitNode(EmptyTypeNode node) {
        this.printNode(node);
		return null;
	}

}
