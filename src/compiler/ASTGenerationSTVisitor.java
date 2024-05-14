package compiler;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.DecNode;
import compiler.lib.Node;
import compiler.lib.TypeNode;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static compiler.lib.FOOLlib.extractCtxName;
import static compiler.lib.FOOLlib.lowerizeFirstChar;

/**
 *  ASTGenerationSTVisitor: genera un AST del linguaggio a partire dal ”ParseTree”
 *  (che viene generato dalla classe ”FOOLParser”).
 *
 * @print true: stampa, false: non stampa
 * @indent stringa di spazi bianchi per l'indentazione
 */
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

    public boolean print;
    String indent;
    public ASTGenerationSTVisitor() {
    }

    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }

    /**
     * stampa informazioni relative a questo contesto, come il tipo di contesto e
     * il numero di produzione associato, se applicabile.
     *
     * @param ctx rappresenta il contesto di parsing di una regola specifica
     */
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        final Class<?> ctxClass = ctx.getClass();
        final Class<?> parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) {
            // parentClass is the var context (and not ctxClass itself)
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        }
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    /**
     * Visita la radice dell'albero per stamparne il contenuto
     *
     * @param t albero sintattico da visitare
     * @return risultato della visita del nodo
     */
    @Override
    public Node visit(ParseTree t) {
        if (t == null) return null;
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    /**
     * Visita l'albero di parsing, primo nodo analizzato.
     *
     * @param c il contesto da visitare
     * @return una visita al contesto del progBody
     */
    @Override
    public Node visitProg(ProgContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.progbody());
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di un blocco di istruzione let-in
     *
     * @param context il contesto da visitare
     * @return un nodo del tipo let-in che contiene tutte le dichiarazioni e una visita al
     * contesto delle espressionie
     */
    @Override
    public Node visitLetInProg(LetInProgContext context) {
        if (print) printVarAndProdName(context);

        final List<DecNode> classDeclarations = context.cldec().stream()
                .map(this::visit)
                .map(node -> (DecNode) node)
                .collect(Collectors.toList());

        final List<DecNode> declarations = context.dec().stream()
                .map(this::visit)
                .map(node -> (DecNode) node)
                .collect(Collectors.toList());

        final List<DecNode> declist = new ArrayList<>();
        declist.addAll(classDeclarations);
        declist.addAll(declarations);

        return new ProgLetInNode(declist, visit(context.exp()));
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di nessuna dichiarazione
     * (quindi espressioni secche)
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo progetto che visita l'espressione
     */
    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) printVarAndProdName(c);
        return new ProgNode(visit(c.exp()));
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di espressione di divisione o moltiplicazione
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo times o div
     */
    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) printVarAndProdName(c);

        if (c.TIMES() != null) {
            Node n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.TIMES().getSymbol().getLine());        // setLine added
            return n;
        } else {
            Node n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.DIV().getSymbol().getLine());        // setLine added
            return n;
        }
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di espressione di addizzione o sottrazione
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo plus o minus
     */
    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) printVarAndProdName(c);

        if (c.PLUS() != null) {
            Node n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.PLUS().getSymbol().getLine());        // setLine added
            return n;
        } else {
            Node n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.MINUS().getSymbol().getLine());        // setLine added
            return n;
        }
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto delle comparazioni
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo equal, lessEqual o greaterEqual
     */
    @Override
    public Node visitComp(CompContext c) {
        if (print) printVarAndProdName(c);

        if (c.EQ() != null) {
            Node n = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.EQ().getSymbol().getLine());
            return n;
        } else if (c.LE() != null) {
            Node n = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.LE().getSymbol().getLine());
            return n;
        } else {
            Node n = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.GE().getSymbol().getLine());
            return n;
        }
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di espressione degli operatori di algebra
     * booleana (tranne il not)
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo or o and
     */
    @Override
    public Node visitAndOr(AndOrContext c) {
        if (print) printVarAndProdName(c);

        if (c.OR() != null) {
            Node n = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.OR().getSymbol().getLine());
            return n;
        } else {
            Node n = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.AND().getSymbol().getLine());
            return n;
        }
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di espressione dell'operatore booleano not
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo not
     */
    @Override
    public Node visitNot(NotContext c) {
        if (print) printVarAndProdName(c);
        Node n = new NotNode(visit(c.exp()));
        n.setLine(c.NOT().getSymbol().getLine());
        return n;
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto della dichiarazione di una variabile
     *
     * @param context il contesto da visitare
     * @return un nodo del tipo varNode
     */
    @Override
    public Node visitVardec(VardecContext context) {
        if (print) printVarAndProdName(context);
        //non-incomplete ST
        if (Objects.isNull(context.ID())) return null;

        final VarNode n = new VarNode(context.ID().getText(), (TypeNode) visit(context.type()), visit(context.exp()));
        n.setLine(context.VAR().getSymbol().getLine());
        return n;
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di una funzione (non di una classe)
     *
     * @param context il contesto da visitare
     * @return un nodo del tipo FunNode
     */
    @Override
    public Node visitFundec(FundecContext context) {
        if (print) printVarAndProdName(context);
        if (context.ID().isEmpty()) return null;

        List<ParNode> parametersList = new ArrayList<>();
        for (int i = 1; i < context.ID().size(); i++) {
            ParNode p = new ParNode(context.ID(i).getText(), (TypeNode) visit(context.type(i)));
            p.setLine(context.ID(i).getSymbol().getLine());
            parametersList.add(p);
        }

        final List<DecNode> declarationsList = context.dec().stream()
                .map(dec -> (DecNode) visit(dec))
                .collect(Collectors.toList());

        final String id = context.ID(0).getText();
        final TypeNode type = (TypeNode) visit(context.type(0));
        final FunNode node = new FunNode(id, type, parametersList, declarationsList, visit(context.exp()));
        node.setLine(context.FUN().getSymbol().getLine());
        return node;
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto del tipo Int
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo IntTypeNode
     */
    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) printVarAndProdName(c);
        return new IntTypeNode();
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto del tipo booleano
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo BoolTypeNode
     */
    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) printVarAndProdName(c);
        return new BoolTypeNode();
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto del tipo Integer
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo IntNode
     */
    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) printVarAndProdName(c);
        int value = Integer.parseInt(c.NUM().getText());
        return new IntNode(Objects.isNull(c.MINUS()) ? value : -value);
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto dell'espressione "true"
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo BoolNode con attributo true
     */
    @Override
    public Node visitTrue(TrueContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(true);
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto dell'espressione "false"
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo BoolNode con attributo false
     */
    @Override
    public Node visitFalse(FalseContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(false);
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto di un blocco If
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo IfNode
     */
    @Override
    public Node visitIf(IfContext c) {
        if (print) printVarAndProdName(c);
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));
        Node n = new IfNode(ifNode, thenNode, elseNode);
        n.setLine(c.IF().getSymbol().getLine());
        return n;
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto della print
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo PrintNode
     */
    @Override
    public Node visitPrint(PrintContext c) {
        if (print) printVarAndProdName(c);
        return new PrintNode(visit(c.exp()));
    }

    /**
     * Visita l'albero di parsing, se siamo nel contesto delle parentesi
     *
     * @param c il contesto da visitare
     * @return la visita al contesto di una espressione
     */
    @Override
    public Node visitPars(ParsContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.exp());
    }


    /**
     * Visita l'albero di parsing, se siamo nel contesto di un identificativo
     * (esempio: nome di una variabile)
     *
     * @param c il contesto da visitare
     * @return un nodo del tipo IdNode
     */
    @Override
    public Node visitId(IdContext c) {
        if (print) printVarAndProdName(c);
        Node n = new IdNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }


    /**
     * Visita l'albero di parsing, se siamo nel contesto di una chiamata
     * (esempio gli argomenti di una funzione)
     *
     * @param c il contesto da visitare
     * @return callNode
     */
    @Override
    public Node visitCall(CallContext c) {
        if (print) printVarAndProdName(c);
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
        if (print) printVarAndProdName(context);
        if (context.ID().isEmpty()) return null; // Incomplete ST

        final Optional<String> superId = Objects.isNull(context.EXTENDS()) ?
                Optional.empty() : Optional.of(context.ID(1).getText());
        final int idSuperPadding = superId.isPresent() ? 2 : 1;

        final List<FieldNode> fields = new ArrayList<>();
        for (int i = idSuperPadding; i < context.ID().size(); i++) {
            final String id = context.ID(i).getText();
            final TypeNode type = (TypeNode) visit(context.type(i - idSuperPadding));
            final FieldNode f = new FieldNode(id, type);
            f.setLine(context.ID(i).getSymbol().getLine());
            fields.add(f);
        }
        final List<MethodNode> methods = context.methdec().stream()
                .map(x -> (MethodNode) visit(x))
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
        if (print) printVarAndProdName(context);
        if (context.ID().isEmpty()) return null; // Incomplete ST
        final String methodId = context.ID(0).getText();
        final TypeNode returnType = (TypeNode) visit(context.type(0));

        final int idPadding = 1;
        final List<ParNode> params = new ArrayList<>();
        for (int i = idPadding; i < context.ID().size(); i++) {
            final String id = context.ID(i).getText();
            final TypeNode type = (TypeNode) visit(context.type(i));
            final ParNode p = new ParNode(id, type);
            p.setLine(context.ID(i).getSymbol().getLine());
            params.add(p);
        }

        final List<DecNode> declarations = context.dec().stream()
                .map(x -> (DecNode) visit(x))
                .toList();

        final Node exp = visit(context.exp());
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
        if (print) printVarAndProdName(context);
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
        if (print) printVarAndProdName(context);
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
        if (print) printVarAndProdName(context);
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
        if (print) printVarAndProdName(context);

        final String id = context.ID().getText();
        final RefTypeNode node = new RefTypeNode(id);
        node.setLine(context.ID().getSymbol().getLine());
        return node;
    }

}
