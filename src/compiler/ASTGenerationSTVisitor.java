package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

/**
 *  ASTGenerationSTVisitor: genera un AST del linguaggio a partire dal ”ParseTree”
 *  (che viene generato dalla classe ”FOOLParser”).
 *
 * @print true: stampa, false: non stampa
 * @indent stringa di spazi bianchi per l'indentazione
 */
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	String indent;
    public boolean print;
	
    public ASTGenerationSTVisitor() {}
    ASTGenerationSTVisitor(boolean debug) {
        this.print =debug; }
        
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix="";        
    	Class<?> ctxClass=ctx.getClass(), parentClass=ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
        	prefix=lowerizeFirstChar(extractCtxName(parentClass.getName()))+": production #";
    	System.out.println(this.indent +prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }
        
    @Override
	public Node visit(ParseTree t) {
    	if (t==null) return null;
        String temp= this.indent;
        this.indent =(this.indent ==null)?"": this.indent +"  ";
        Node result = super.visit(t);
        this.indent =temp;
        return result; 
	}

	@Override
	public Node visitProg(ProgContext c) {
		if (this.print) this.printVarAndProdName(c);
		return this.visit(c.progbody());
	}

	@Override
	public Node visitLetInProg(LetInProgContext c) {
		if (this.print) this.printVarAndProdName(c);
		final List<DecNode> classDeclarationList = c.cldec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.toList();
		final List<DecNode> declarationList = c.dec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.toList();
		final List<DecNode> decList = new ArrayList<>();
		decList.addAll(classDeclarationList);
		decList.addAll(declarationList);
		return new ProgLetInNode(decList, this.visit(c.exp()));
	}

	@Override
	public Node visitNoDecProg(NoDecProgContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new ProgNode(this.visit(c.exp()));
	}

	@Override
	public Node visitVardec(VardecContext c) {
		if (this.print) this.printVarAndProdName(c);
		Node n = null;
		if (c.ID()!=null) {
			n = new VarNode(c.ID().getText(), (TypeNode) this.visit(c.type()), this.visit(c.exp()));
			n.setLine(c.VAR().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitFundec(FundecContext c) {
		if (this.print) this.printVarAndProdName(c);
		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) { 
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) this.visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) this.visit(dec));
		Node n = null;
		if (!c.ID().isEmpty()) { //non-incomplete ST
			n = new FunNode(c.ID(0).getText(),(TypeNode) this.visit(c.type(0)),parList,decList, this.visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitTimesDiv(TimesDivContext c){
		if (this.print) this.printVarAndProdName(c);

		if (c.TIMES() != null) {
			Node n = new TimesNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.TIMES().getSymbol().getLine());        // setLine added
			return n;
		} else {
			Node n = new DivNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.DIV().getSymbol().getLine());        // setLine added
			return n;
		}
	}

	@Override
	public Node visitPlusMinus(PlusMinusContext c) {
		if (this.print) this.printVarAndProdName(c);

		if (c.PLUS() != null) {
			Node n = new PlusNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.PLUS().getSymbol().getLine());        // setLine added
			return n;
		} else {
			Node n = new MinusNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.MINUS().getSymbol().getLine());        // setLine added
			return n;
		}
	}

	@Override
	public Node visitComp(CompContext c) {
		if (this.print) this.printVarAndProdName(c);

		if (c.EQ() != null) {
			Node n = new EqualNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.EQ().getSymbol().getLine());
			return n;
		} else if (c.LE() != null) {
			Node n = new LessEqualNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.LE().getSymbol().getLine());
			return n;
		} else {
			Node n = new GreaterEqualNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.GE().getSymbol().getLine());
			return n;
		}
	}

	@Override
	public Node visitAndOr(AndOrContext c) {
		if (this.print) this.printVarAndProdName(c);

		if (c.OR() != null) {
			Node n = new OrNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.OR().getSymbol().getLine());
			return n;
		} else {
			Node n = new AndNode(this.visit(c.exp(0)), this.visit(c.exp(1)));
			n.setLine(c.AND().getSymbol().getLine());
			return n;
		}
	}

	@Override
	public Node visitNot(NotContext c) {
		if (this.print) this.printVarAndProdName(c);
		Node n = new NotNode(this.visit(c.exp()));
		n.setLine(c.NOT().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitIntType(IntTypeContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new IntTypeNode();
	}

	@Override
	public Node visitBoolType(BoolTypeContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new BoolTypeNode();
	}

	@Override
	public Node visitInteger(IntegerContext c) {
		if (this.print) this.printVarAndProdName(c);
		int v = Integer.parseInt(c.NUM().getText());
		return new IntNode(c.MINUS()==null?v:-v);
	}

	@Override
	public Node visitTrue(TrueContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new BoolNode(true);
	}

	@Override
	public Node visitFalse(FalseContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new BoolNode(false);
	}

	@Override
	public Node visitIf(IfContext c) {
		if (this.print) this.printVarAndProdName(c);
		Node ifNode = this.visit(c.exp(0));
		Node thenNode = this.visit(c.exp(1));
		Node elseNode = this.visit(c.exp(2));
		Node n = new IfNode(ifNode, thenNode, elseNode);
		n.setLine(c.IF().getSymbol().getLine());			
        return n;		
	}

	@Override
	public Node visitPrint(PrintContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new PrintNode(this.visit(c.exp()));
	}

	@Override
	public Node visitPars(ParsContext c) {
		if (this.print) this.printVarAndProdName(c);
		return this.visit(c.exp());
	}

	@Override
	public Node visitId(IdContext c) {
		if (this.print) this.printVarAndProdName(c);
		Node n = new IdNode(c.ID().getText());
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitCall(CallContext c) {
		if (this.print) this.printVarAndProdName(c);
		List<Node> argumentList = new ArrayList<>();
		for (ExpContext arg : c.exp()) argumentList.add(this.visit(arg));
		Node n = new CallNode(c.ID().getText(), argumentList);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	// OBJECT-ORIENTED EXTENSION

	@Override
	public Node visitCldec(final CldecContext c){
		if(this.print) this.printVarAndProdName(c);
		if(c.ID().isEmpty()) return null;

		final Optional<String> superID = Objects.isNull(c.EXTENDS()) ?
				Optional.empty() : Optional.of(c.ID(1).getText());
		final int idSuperPadding = superID.isPresent() ? 2 : 1;
		final List<FieldNode> fieldList = new ArrayList<>();
		for(int i = idSuperPadding; i < c.ID().size(); i++){
			final TypeNode type = (TypeNode) this.visit(c.type(i - idSuperPadding));
			final FieldNode f = new FieldNode(c.ID(i).getText(), type);
			f.setLine(c.ID(i).getSymbol().getLine());
			fieldList.add(f);
		}

		final List<MethodNode> methodList = new ArrayList<>();
		for (ParseTree node : c.methdec()) {
			MethodNode method = (MethodNode) this.visit(node);
			methodList.add(method);
		}

		final String classID = c.ID(0).getText();
		final ClassNode classNode = new ClassNode(classID, superID, fieldList, methodList);
		classNode.setLine(c.ID(0).getSymbol().getLine());
		return classNode;
	}

	@Override
	public Node visitMethdec(final MethdecContext c){
		if (this.print) this.printVarAndProdName(c);
		if (c.ID().isEmpty()) return null; // Incomplete ST
		final TypeNode returnType = (TypeNode) this.visit(c.type(0));

		final int idPadding = 1;
		final List<ParNode> paramList = new ArrayList<>();
		for (int i = idPadding; i < c.ID().size(); i++) {
			final TypeNode type = (TypeNode) this.visit(c.type(i));
			final ParNode p = new ParNode(c.ID(i).getText(), type);
			p.setLine(c.ID(i).getSymbol().getLine());
			paramList.add(p);
		}

		List<DecNode> declarationList = new ArrayList<>();
		for (ParseTree node : c.dec()) {
			DecNode declaration = (DecNode) this.visit(node);
			declarationList.add(declaration);
		}

		final Node exp = this.visit(c.exp());
		final MethodNode methodNode = new MethodNode(c.ID(0).getText(), returnType, paramList, declarationList, exp);
		methodNode.setLine(c.ID(0).getSymbol().getLine());
		return methodNode;
	}

	@Override
	public Node visitNull(final NullContext c) {
		if (this.print) this.printVarAndProdName(c);
		return new EmptyNode();
	}

	@Override
	public Node visitDotCall(final DotCallContext c) {
		if (this.print) this.printVarAndProdName(c);
		if (c.ID().size() != 2) return null; // Incomplete ST

		final String objectId = c.ID(0).getText();
		final String methodId = c.ID(1).getText();
		final List<Node> argumentList = new ArrayList<>();
		for (ParseTree exp : c.exp()) {
			Node argument = this.visit(exp);
			argumentList.add(argument);
		}

		final ClassCallNode classCallNode = new ClassCallNode(objectId, methodId, argumentList);
		classCallNode.setLine(c.ID(0).getSymbol().getLine());
		return classCallNode;
	}

	@Override
	public Node visitNew(final NewContext c) {
		if (this.print) this.printVarAndProdName(c);
		if (Objects.isNull(c.ID())) return null; // Incomplete ST

		final String classId = c.ID().getText();
		final List<Node> argumentList = new ArrayList<>();
		for (ParseTree exp : c.exp()) {
			Node argument = this.visit(exp);
			argumentList.add(argument);
		}

		final NewNode newNode = new NewNode(classId, argumentList);
		newNode.setLine(c.ID().getSymbol().getLine());
		return newNode;
	}

	@Override
	public Node visitIdType(final IdTypeContext c) {
		if (this.print) this.printVarAndProdName(c);

		final String id = c.ID().getText();
		final RefTypeNode node = new RefTypeNode(id);
		node.setLine(c.ID().getSymbol().getLine());
		return node;
	}
}

