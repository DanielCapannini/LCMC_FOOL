package compiler;

import java.util.*;
import java.util.stream.Collectors;

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
				.collect(Collectors.toList());
		final List<DecNode> declarationList = c.dec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.collect(Collectors.toList());
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
		if (c.ID().isEmpty()) return null;

		List<ParNode> parametersList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) this.visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parametersList.add(p);
		}

		final List<DecNode> declarationsList = c.dec().stream()
				.map(dec -> (DecNode) this.visit(dec))
				.collect(Collectors.toList());

		final String id = c.ID(0).getText();
		final TypeNode type = (TypeNode) this.visit(c.type(0));
		final FunNode node = new FunNode(id, type, parametersList, declarationsList, this.visit(c.exp()));
		node.setLine(c.FUN().getSymbol().getLine());
		return node;
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
		List<Node> arglist = c.exp().stream().map(this::visit).collect(Collectors.toList());

		Node n = new CallNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	// OBJECT-ORIENTED EXTENSION

	/**
	 * Visita l'albero di parsing, se siamo nel contesto della dichiarazione di una classe
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo ClassNode
	 */
	@Override
	public Node visitCldec(final CldecContext context) {
		if (this.print) this.printVarAndProdName(context);
		if (context.ID().isEmpty()) return null; // Incomplete ST

		final Optional<String> superId = Objects.isNull(context.EXTENDS()) ?
				Optional.empty() : Optional.of(context.ID(1).getText());
		final int idSuperPadding = superId.isPresent() ? 2 : 1;

		final List<FieldNode> fields = new ArrayList<>();
		for (int i = idSuperPadding; i < context.ID().size(); i++) {
			final String id = context.ID(i).getText();
			final TypeNode type = (TypeNode) this.visit(context.type(i - idSuperPadding));
			final FieldNode f = new FieldNode(id, type);
			f.setLine(context.ID(i).getSymbol().getLine());
			fields.add(f);
		}
		final List<MethodNode> methods = context.methdec().stream()
				.map(x -> (MethodNode) this.visit(x))
				.collect(Collectors.toList());

		final String classId = context.ID(0).getText();
		final ClassNode classNode = new ClassNode(classId, superId, fields, methods);
		classNode.setLine(context.ID(0).getSymbol().getLine());
		return classNode;
	}


	/**
	 * Visita l'albero di parsing, se siamo nel contesto di un metodo di una funzione
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo MethodNode
	 */
	@Override
	public Node visitMethdec(final MethdecContext context) {
		if (this.print) this.printVarAndProdName(context);
		if (context.ID().isEmpty()) return null; // Incomplete ST
		final String methodId = context.ID(0).getText();
		final TypeNode returnType = (TypeNode) this.visit(context.type(0));

		final int idPadding = 1;
		final List<ParNode> params = new ArrayList<>();
		for (int i = idPadding; i < context.ID().size(); i++) {
			final String id = context.ID(i).getText();
			final TypeNode type = (TypeNode) this.visit(context.type(i));
			final ParNode p = new ParNode(id, type);
			p.setLine(context.ID(i).getSymbol().getLine());
			params.add(p);
		}

		final List<DecNode> declarations = context.dec().stream()
				.map(x -> (DecNode) this.visit(x))
				.collect(Collectors.toList());

		final Node exp = this.visit(context.exp());
		final MethodNode methodNode = new MethodNode(methodId, returnType, params, declarations, exp);
		methodNode.setLine(context.ID(0).getSymbol().getLine());
		return methodNode;
	}


	/**
	 * Visita l'albero di parsing, se siamo nel contesto dell'espressione "null"
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo EmptyNode
	 */
	@Override
	public Node visitNull(final NullContext context) {
		if (this.print) this.printVarAndProdName(context);
		return new EmptyNode();
	}

	/**
	 * Visita l'albero di parsing, se siamo nel contesto dell'espressione "."
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo classCallNode
	 */
	@Override
	public Node visitDotCall(final DotCallContext context) {
		if (this.print) this.printVarAndProdName(context);
		if (context.ID().size() != 2) return null; // Incomplete ST

		final String objectId = context.ID(0).getText();
		final String methodId = context.ID(1).getText();
		final List<Node> args = context.exp().stream()
				.map(this::visit)
				.collect(Collectors.toList());

		final ClassCallNode classCallNode = new ClassCallNode(objectId, methodId, args);
		classCallNode.setLine(context.ID(0).getSymbol().getLine());
		return classCallNode;
	}

	/**
	 * Visita l'albero di parsing, se siamo nel contesto dell'espressione "new"
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo NewNode
	 */

	@Override
	public Node visitNew(final NewContext context) {
		if (this.print) this.printVarAndProdName(context);
		if (Objects.isNull(context.ID())) return null; // Incomplete ST

		final String classId = context.ID().getText();
		final List<Node> args = context.exp().stream()
				.map(this::visit)
				.collect(Collectors.toList());

		final NewNode newNode = new NewNode(classId, args);
		newNode.setLine(context.ID().getSymbol().getLine());
		return newNode;
	}

	/**
	 * Visita l'albero di parsing, se siamo nel contesto dell'identificativo del tipo di una classe
	 *
	 * @param context il contesto da visitare
	 * @return un nodo del tipo IdTypeNode
	 */

	@Override
	public Node visitIdType(final IdTypeContext context) {
		if (this.print) this.printVarAndProdName(context);

		final String id = context.ID().getText();
		final RefTypeNode node = new RefTypeNode(id);
		node.setLine(context.ID().getSymbol().getLine());
		return node;
	}
}

