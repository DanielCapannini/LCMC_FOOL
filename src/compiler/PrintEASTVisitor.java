package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void,VoidException> {

	public PrintEASTVisitor() { super(false,true); }

	@Override
	public Void visitNode(ProgLetInNode n) {
        this.printNode(n);
		for (Node dec : n.declarationlist) this.visit(dec);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
        this.printNode(n);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(FunNode n) {
        this.printNode(n,n.id);
        this.visit(n.retType);
		for (ParNode par : n.parameterlist) this.visit(par);
		for (Node dec : n.declarationlist) this.visit(dec);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ParNode n) {
        this.printNode(n,n.id);
        this.visit(n.getType());
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
        this.printNode(n,n.id);
        this.visit(n.getType());
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
        this.printNode(n);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
        this.printNode(n);
        this.visit(n.cond);
        this.visit(n.thenNode);
        this.visit(n.elseNode);
		return null;
	}

	@Override
	public Void visitNode(EqualNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(TimesNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
        this.printNode(n,n.id+" at nestinglevel "+n.nestingLevel);
        this.visit(n.entry);
		for (Node arg : n.argumentList) this.visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
        this.printNode(n,n.id+" at nestinglevel "+n.nestingLevel);
        this.visit(n.entry);
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
        this.printNode(n,n.value.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
        this.printNode(n,n.value.toString());
		return null;
	}
	
	@Override
	public Void visitNode(ArrowTypeNode n) {
        this.printNode(n);
		for (Node par: n.parameterList) this.visit(par);
        this.visit(n.returnType,"->"); //marks return type
		return null;
	}

	@Override
	public Void visitNode(BoolTypeNode n) {
        this.printNode(n);
		return null;
	}

	@Override
	public Void visitNode(IntTypeNode n) {
        this.printNode(n);
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
	public Void visitNode(MinusNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
        this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
        this.printNode(n);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ClassNode n) {
        this.printNode(n, n.classId + " extends " + n.superId.orElse("nothing"));
		n.fieldList.forEach(this::visit);
		n.methodList.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(FieldNode n) {
        this.printNode(n, n.id + " Offset: " + n.offset);
        this.visit(n.getType());
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
        this.printNode(n, n.id + " Offset: " + n.offset);
        this.visit(n.returnType);
		n.parameterList.forEach(this::visit);
		n.declarationList.forEach(this::visit);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
        this.printNode(n, n.objectId + "." + n.objectId + " at nestinglevel: " + n. nestingLevel);
        this.visit(n.entry);
        this.visit(n.methodEntry);
		n.argumentList.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
        this.printNode(n, n.classId + " at nestinglevel: " + n.entry.nl);
		n.argumentList.forEach(this::visit);
        this.visit(n.entry);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) {
        this.printNode(n);
		return null;
	}

	@Override
	public Void visitNode(ClassTypeNode n) {
        this.printNode(n);
		n.fieldList.forEach(this::visit);
		n.methodList.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(MethodTypeNode n) {
        this.printNode(n);
		n.functionalType.parameterList.forEach(this::visit);
        this.visit(n.functionalType.returnType, "->"); //marks return type
		return null;
	}

	@Override
	public Void visitNode(RefTypeNode n) {
        this.printNode(n, n.typeId);
		return null;
	}

	@Override
	public Void visitNode(EmptyTypeNode n) {
        this.printNode(n);
		return null;
	}

}
