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
        this.indent = (this.indent ==null)? "" : this.indent + "  ";
        Node result = super.visit(t);
        this.indent =temp;
        return result; 
	}

	@Override
	public Node visitProg(ProgContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return this.visit(Context.progbody());
	}

	@Override
	public Node visitLetInProg(LetInProgContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		final List<DecNode> classDeclarationList = Context.cldec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.collect(Collectors.toList());
		final List<DecNode> declarationList = Context.dec().stream()
				.map(this::visit)
				.map(node -> (DecNode) node)
				.collect(Collectors.toList());
		final List<DecNode> decList = new ArrayList<>();
		decList.addAll(classDeclarationList);
		decList.addAll(declarationList);
		return new ProgLetInNode(decList, this.visit(Context.exp()));
	}

	@Override
	public Node visitNoDecProg(NoDecProgContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new ProgNode(this.visit(Context.exp()));
	}

	@Override
	public Node visitVardec(VardecContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		Node n = null;
		if (Context.ID()!=null) {
			n = new VarNode(Context.ID().getText(), (TypeNode) this.visit(Context.type()), this.visit(Context.exp()));
			n.setLine(Context.VAR().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitFundec(FundecContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		if (Context.ID().isEmpty()) return null;

		List<ParNode> parametersList = new ArrayList<>();
		for (int i = 1; i < Context.ID().size(); i++) {
			ParNode p = new ParNode(Context.ID(i).getText(), (TypeNode) this.visit(Context.type(i)));
			p.setLine(Context.ID(i).getSymbol().getLine());
			parametersList.add(p);
		}

		final List<DecNode> declarationsList = Context.dec().stream()
				.map(dec -> (DecNode) this.visit(dec))
				.collect(Collectors.toList());

		final String id = Context.ID(0).getText();
		final TypeNode type = (TypeNode) this.visit(Context.type(0));
		final FunNode node = new FunNode(id, type, parametersList, declarationsList, this.visit(Context.exp()));
		node.setLine(Context.FUN().getSymbol().getLine());
		return node;
	}

	@Override
	public Node visitTimesDiv(TimesDivContext Context){
		if (this.print) this.printVarAndProdName(Context);

		if (Context.TIMES() != null) {
			Node n = new TimesNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.TIMES().getSymbol().getLine());        // setLine added
			return n;
		} else {
			Node n = new DivNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.DIV().getSymbol().getLine());        // setLine added
			return n;
		}
	}

	@Override
	public Node visitPlusMinus(PlusMinusContext Context) {
		if (this.print) this.printVarAndProdName(Context);

		if (Context.PLUS() != null) {
			Node n = new PlusNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.PLUS().getSymbol().getLine());        // setLine added
			return n;
		} else {
			Node n = new MinusNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.MINUS().getSymbol().getLine());        // setLine added
			return n;
		}
	}

	@Override
	public Node visitComp(CompContext Context) {
		if (this.print) this.printVarAndProdName(Context);

		if (Context.EQ() != null) {
			Node n = new EqualNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.EQ().getSymbol().getLine());
			return n;
		} else if (Context.LE() != null) {
			Node n = new LessEqualNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.LE().getSymbol().getLine());
			return n;
		} else {
			Node n = new GreaterEqualNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.GE().getSymbol().getLine());
			return n;
		}
	}

	@Override
	public Node visitAndOr(AndOrContext Context) {
		if (this.print) this.printVarAndProdName(Context);

		if (Context.OR() != null) {
			Node n = new OrNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.OR().getSymbol().getLine());
			return n;
		} else {
			Node n = new AndNode(this.visit(Context.exp(0)), this.visit(Context.exp(1)));
			n.setLine(Context.AND().getSymbol().getLine());
			return n;
		}
	}

	@Override
	public Node visitNot(NotContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		Node n = new NotNode(this.visit(Context.exp()));
		n.setLine(Context.NOT().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitIntType(IntTypeContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new IntTypeNode();
	}

	@Override
	public Node visitBoolType(BoolTypeContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new BoolTypeNode();
	}

	@Override
	public Node visitInteger(IntegerContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		int v = Integer.parseInt(Context.NUM().getText());
		return new IntNode(Context.MINUS()==null?v:-v);
	}

	@Override
	public Node visitTrue(TrueContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new BoolNode(true);
	}

	@Override
	public Node visitFalse(FalseContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new BoolNode(false);
	}

	@Override
	public Node visitIf(IfContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		Node ifNode = this.visit(Context.exp(0));
		Node thenNode = this.visit(Context.exp(1));
		Node elseNode = this.visit(Context.exp(2));
		Node n = new IfNode(ifNode, thenNode, elseNode);
		n.setLine(Context.IF().getSymbol().getLine());
        return n;		
	}

	@Override
	public Node visitPrint(PrintContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return new PrintNode(this.visit(Context.exp()));
	}

	@Override
	public Node visitPars(ParsContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		return this.visit(Context.exp());
	}

	@Override
	public Node visitId(IdContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		Node n = new IdNode(Context.ID().getText());
		n.setLine(Context.ID().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitCall(CallContext Context) {
		if (this.print) this.printVarAndProdName(Context);
		List<Node> arglist = Context.exp().stream().map(this::visit).collect(Collectors.toList());

		Node n = new CallNode(Context.ID().getText(), arglist);
		n.setLine(Context.ID().getSymbol().getLine());
		return n;
	}

	// OBJECT-ORIENTED EXTENSION

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

	@Override
	public Node visitNull(final NullContext context) {
		if (this.print) this.printVarAndProdName(context);
		return new EmptyNode();
	}

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

	@Override
	public Node visitIdType(final IdTypeContext context) {
		if (this.print) this.printVarAndProdName(context);

		final String id = context.ID().getText();
		final RefTypeNode node = new RefTypeNode(id);
		node.setLine(context.ID().getSymbol().getLine());
		return node;
	}
}