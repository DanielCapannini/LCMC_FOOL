package compiler;

import compiler.AST.*;
import compiler.exc.VoidException;
import compiler.lib.BaseEASTVisitor;
import compiler.lib.Node;

/**
 * Questa classe implementa un visitor che stampa l'E-AST.
 * Usato per il debug.
 */
public class PrintEASTVisitor extends BaseEASTVisitor<Void, VoidException> {

    public PrintEASTVisitor() {
        super(false, true);
    }

    @Override
    public Void visitNode(ProgLetInNode n) {
        printNode(n);
        for (Node dec : n.declarations) visit(dec);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ProgNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        printNode(n, n.id);
        visit(n.returnType);
        for (ParNode par : n.parameters) visit(par);
        for (Node dec : n.declarations) visit(dec);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ParNode n) {
        printNode(n, n.id);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        printNode(n, n.id);
        visit(n.getType());
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(PrintNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        printNode(n);
        visit(n.cond);
        visit(n.thenBranch);
        visit(n.elseBranch);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(TimesNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nestingLevel);
        visit(n.entry);
        for (Node arg : n.arguments) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(IdNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nestingLevel);
        visit(n.entry);
        return null;
    }

    @Override
    public Void visitNode(BoolNode n) {
        printNode(n, n.value.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        printNode(n, n.value.toString());
        return null;
    }

    @Override
    public Void visitNode(ArrowTypeNode n) {
        printNode(n);
        for (Node par : n.parameters) visit(par);
        visit(n.returnType, "->"); //marks return type
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitSTentry(STentry entry) {
        printSTentry("nestlev " + entry.nl);
        printSTentry("type");
        visit(entry.type);
        printSTentry("offset " + entry.offset);
        return null;
    }

    // new Nodes
    @Override
    public Void visitNode(MinusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    // OO nodes
    @Override
    public Void visitNode(ClassNode n) {
        printNode(n, n.classId + " extends " + n.superId.orElse("nothing"));
        n.fields.forEach(this::visit);
        n.methods.forEach(this::visit);
        return null;
    }

    @Override
    public Void visitNode(FieldNode n) {
        printNode(n, n.id + " Offset: " + n.offset);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(MethodNode n) {
        printNode(n, n.id + " Offset: " + n.offset);
        visit(n.returnType);
        n.parameters.forEach(this::visit);
        n.declarations.forEach(this::visit);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode n) {
        printNode(n, n.objectId + "." + n.objectId + " at nestinglevel: " + n. nestingLevel);
        visit(n.entry);
        visit(n.methodEntry);
        n.args.forEach(this::visit);
        return null;
    }

    @Override
    public Void visitNode(NewNode n) {
        printNode(n, n.classId + " at nestinglevel: " + n.entry.nl);
        n.args.forEach(this::visit);
        visit(n.entry);
        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(ClassTypeNode n) {
        printNode(n);
        n.fields.forEach(this::visit);
        n.methods.forEach(this::visit);
        return null;
    }

    @Override
    public Void visitNode(MethodTypeNode n) {
        printNode(n);
        n.functionalType.parameters.forEach(this::visit);
        visit(n.functionalType.returnType, "->"); //marks return type
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode n) {
        printNode(n, n.typeId);
        return null;
    }

    @Override
    public Void visitNode(EmptyTypeNode n) {
        printNode(n);
        return null;
    }
}
