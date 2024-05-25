package compiler;

import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	String indent;
    public boolean print;
	
    public ASTGenerationSTVisitor() {}

	/**
	 * stampa informazioni relative a questo contesto, come il tipo di contesto e
	 * il numero di produzione associato, se applicabile.
	 *
	 * @param ctx rappresenta il contesto di parsing di una regola specifica
	 */
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix="";        
    	Class<?> ctxClass=ctx.getClass(), parentClass=ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
        	prefix=lowerizeFirstChar(extractCtxName(parentClass.getName()))+": production #";
    	System.out.println(this.indent +prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

	/**
	 *
	 * @param t ParseTree da visitare
	 * @return risultato visita
	 */
	@Override
	public Node visit(ParseTree t) {
    	if (t==null) return null;
        String temp= this.indent;
        this.indent = (this.indent ==null)? "" : this.indent + "  ";
        Node result = super.visit(t);
        this.indent =temp;
        return result; 
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return
	 */
	@Override
	public Node visitProg(ProgContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return this.visit(ctx.progbody());
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return ProgLetInNode
	 */
	@Override
	public Node visitLetInProg(LetInProgContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		final List<DecNode> classDeclarationList = ctx.cldec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.collect(Collectors.toList());
		final List<DecNode> declarationList = ctx.dec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.collect(Collectors.toList());
		final List<DecNode> decList = new ArrayList<>();
		decList.addAll(classDeclarationList);
		decList.addAll(declarationList);
		return new ProgLetInNode(decList, this.visit(ctx.exp()));
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return ProgNode
	 */
	@Override
	public Node visitNoDecProg(NoDecProgContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new ProgNode(this.visit(ctx.exp()));
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return VarNode o null
	 */
	@Override
	public Node visitVardec(VardecContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		Node node = null;
		if (ctx.ID()!=null) {
			node = new VarNode(ctx.ID().getText(), (TypeNode) this.visit(ctx.type()), this.visit(ctx.exp()));
			node.setLine(ctx.VAR().getSymbol().getLine());
		}
        return node;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return FunNode
	 */
	@Override
	public Node visitFundec(FundecContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.ID().isEmpty()) return null;
		List<ParNode> parametersList = new ArrayList<>();
		for (int i = 1; i < ctx.ID().size(); i++) {
			ParNode p = new ParNode(ctx.ID(i).getText(), (TypeNode) this.visit(ctx.type(i)));
			p.setLine(ctx.ID(i).getSymbol().getLine());
			parametersList.add(p);
		}
		final List<DecNode> declarationsList = ctx.dec().stream()
				.map(dec -> (DecNode) this.visit(dec))
				.collect(Collectors.toList());
		final String id = ctx.ID(0).getText();
		final TypeNode type = (TypeNode) this.visit(ctx.type(0));
		final FunNode funNode = new FunNode(id, type, parametersList, declarationsList, this.visit(ctx.exp()));
		funNode.setLine(ctx.FUN().getSymbol().getLine());
		return funNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return TimesNode o DivNode
	 */
	@Override
	public Node visitTimesDiv(TimesDivContext ctx){
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.TIMES() != null) {
			Node timesNode = new TimesNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			timesNode.setLine(ctx.TIMES().getSymbol().getLine());        // setLine added
			return timesNode;
		} else {
			Node divNode = new DivNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			divNode.setLine(ctx.DIV().getSymbol().getLine());        // setLine added
			return divNode;
		}
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return PlusNode o MinusNode
	 */
	@Override
	public Node visitPlusMinus(PlusMinusContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.PLUS() != null) {
			Node plusNode = new PlusNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			plusNode.setLine(ctx.PLUS().getSymbol().getLine());        // setLine added
			return plusNode;
		} else {
			Node minusNode = new MinusNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			minusNode.setLine(ctx.MINUS().getSymbol().getLine());        // setLine added
			return minusNode;
		}
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return EqualNode o LessEqualNode o GreaterEqualNode
	 */
	@Override
	public Node visitComp(CompContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.EQ() != null) {
			Node equalNode = new EqualNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			equalNode.setLine(ctx.EQ().getSymbol().getLine());
			return equalNode;
		} else if (ctx.LE() != null) {
			Node lessEqualNode = new LessEqualNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			lessEqualNode.setLine(ctx.LE().getSymbol().getLine());
			return lessEqualNode;
		} else {
			Node greaterEqualNode = new GreaterEqualNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			greaterEqualNode.setLine(ctx.GE().getSymbol().getLine());
			return greaterEqualNode;
		}
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return OrNode o AndNode
	 */
	@Override
	public Node visitAndOr(AndOrContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.OR() != null) {
			Node orNode = new OrNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			orNode.setLine(ctx.OR().getSymbol().getLine());
			return orNode;
		} else {
			Node andNode = new AndNode(this.visit(ctx.exp(0)), this.visit(ctx.exp(1)));
			andNode.setLine(ctx.AND().getSymbol().getLine());
			return andNode;
		}
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return NotNode
	 */
	@Override
	public Node visitNot(NotContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		Node notNode = new NotNode(this.visit(ctx.exp()));
		notNode.setLine(ctx.NOT().getSymbol().getLine());
		return notNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return IntTypeNode
	 */
	@Override
	public Node visitIntType(IntTypeContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new IntTypeNode();
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return BoolTypeNode
	 */
	@Override
	public Node visitBoolType(BoolTypeContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new BoolTypeNode();
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return IntNode
	 */
	@Override
	public Node visitInteger(IntegerContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		int v = Integer.parseInt(ctx.NUM().getText());
		return new IntNode(ctx.MINUS()==null?v:-v);
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return BoolNode
	 */
	@Override
	public Node visitTrue(TrueContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new BoolNode(true);
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return BoolNode
	 */
	@Override
	public Node visitFalse(FalseContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new BoolNode(false);
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return IfNode
	 */
	@Override
	public Node visitIf(IfContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		Node ifNode = this.visit(ctx.exp(0));
		Node thenNode = this.visit(ctx.exp(1));
		Node elseNode = this.visit(ctx.exp(2));
		Node ifNode1 = new IfNode(ifNode, thenNode, elseNode);
		ifNode1.setLine(ctx.IF().getSymbol().getLine());
        return ifNode1;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return PrintNode
	 */
	@Override
	public Node visitPrint(PrintContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new PrintNode(this.visit(ctx.exp()));
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return
	 */
	@Override
	public Node visitPars(ParsContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return this.visit(ctx.exp());
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return IdNode
	 */
	@Override
	public Node visitId(IdContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		Node idNode = new IdNode(ctx.ID().getText());
		idNode.setLine(ctx.ID().getSymbol().getLine());
		return idNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return CallNode
	 */
	@Override
	public Node visitCall(CallContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		List<Node> arglist = ctx.exp().stream().map(this::visit).collect(Collectors.toList());
		Node callNode = new CallNode(ctx.ID().getText(), arglist);
		callNode.setLine(ctx.ID().getSymbol().getLine());
		return callNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return ClassNode
	 */
	@Override
	public Node visitCldec(final CldecContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.ID().isEmpty()) return null; // Incomplete ST
		final Optional<String> superId = Objects.isNull(ctx.EXTENDS()) ?
				Optional.empty() : Optional.of(ctx.ID(1).getText());
		final int idSuperPadding = superId.isPresent() ? 2 : 1;
		final List<FieldNode> fields = new ArrayList<>();
		for (int i = idSuperPadding; i < ctx.ID().size(); i++) {
			final String id = ctx.ID(i).getText();
			final TypeNode type = (TypeNode) this.visit(ctx.type(i - idSuperPadding));
			final FieldNode f = new FieldNode(id, type);
			f.setLine(ctx.ID(i).getSymbol().getLine());
			fields.add(f);
		}
		final List<MethodNode> methods = ctx.methdec().stream()
				.map(x -> (MethodNode) this.visit(x))
				.collect(Collectors.toList());
		final String classId = ctx.ID(0).getText();
		final ClassNode classNode = new ClassNode(classId, superId, fields, methods);
		classNode.setLine(ctx.ID(0).getSymbol().getLine());
		return classNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return MethodNode
	 */
	@Override
	public Node visitMethdec(final MethdecContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.ID().isEmpty()) return null; // Incomplete ST
		final String methodId = ctx.ID(0).getText();
		final TypeNode returnType = (TypeNode) this.visit(ctx.type(0));
		final int idPadding = 1;
		final List<ParNode> params = new ArrayList<>();
		for (int i = idPadding; i < ctx.ID().size(); i++) {
			final String id = ctx.ID(i).getText();
			final TypeNode type = (TypeNode) this.visit(ctx.type(i));
			final ParNode p = new ParNode(id, type);
			p.setLine(ctx.ID(i).getSymbol().getLine());
			params.add(p);
		}
		final List<DecNode> declarations = ctx.dec().stream()
				.map(x -> (DecNode) this.visit(x))
				.collect(Collectors.toList());
		final Node exp = this.visit(ctx.exp());
		final MethodNode methodNode = new MethodNode(methodId, returnType, params, declarations, exp);
		methodNode.setLine(ctx.ID(0).getSymbol().getLine());
		return methodNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return EmptyNode
	 */
	@Override
	public Node visitNull(final NullContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		return new EmptyNode();
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return ClassCallNode
	 */
	@Override
	public Node visitDotCall(final DotCallContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (ctx.ID().size() != 2) return null; // Incomplete ST
		final String objectId = ctx.ID(0).getText();
		final String methodId = ctx.ID(1).getText();
		final List<Node> args = ctx.exp().stream()
				.map(this::visit)
				.collect(Collectors.toList());
		final ClassCallNode classCallNode = new ClassCallNode(objectId, methodId, args);
		classCallNode.setLine(ctx.ID(0).getSymbol().getLine());
		return classCallNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return NewNode
	 */
	@Override
	public Node visitNew(final NewContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		if (Objects.isNull(ctx.ID())) return null; // Incomplete ST
		final String classId = ctx.ID().getText();
		final List<Node> args = ctx.exp().stream()
				.map(this::visit)
				.collect(Collectors.toList());
		final NewNode newNode = new NewNode(classId, args);
		newNode.setLine(ctx.ID().getSymbol().getLine());
		return newNode;
	}

	/**
	 *
	 * @param ctx the parse tree
	 * @return IdTypeNode
	 */
	@Override
	public Node visitIdType(final IdTypeContext ctx) {
		if (this.print) this.printVarAndProdName(ctx);
		final String id = ctx.ID().getText();
		final RefTypeNode node = new RefTypeNode(id);
		node.setLine(ctx.ID().getSymbol().getLine());
		return node;
	}
}