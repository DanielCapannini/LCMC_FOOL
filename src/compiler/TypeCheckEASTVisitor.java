package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import java.util.Objects;

import static compiler.TypeRels.*;

/**
 * TypeCheckEASTVisitor: classe che esplora un nodo e scopre:
 * per una espressione, il suo tipo (oggetto, BoolTypeNode o IntTypeNode)
 * per una dichiarazione controlla la correttezza interna della dichiarazione
 * per un tipo controlla che il tipo non sia incompleto
 *
 */

public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode, TypeException> {

    public TypeCheckEASTVisitor() {
        super(true);
    } // abilita le eccezioni per l'albero

    TypeCheckEASTVisitor(boolean debug) {
        super(true, debug);
    } // abilita le scritte di debug

    /**
     * controlla che il TypeNode sia visitabile, altrimenti lancia l'eccezione TypeException
     *
     * @param t un nodo del tipo TypeNode
     * @return il nodo analizzato
     * @throws TypeException il nodo non è del tipo giusto
     */
    private TypeNode ckvisit(TypeNode t) throws TypeException {
        this.visit(t);
        return t;
    }

    /**
     * controlla che il ProgLetInNode sia visitabile, altrimenti lancia l'eccezione TypeException o IncomplException
     *
     * @param  node un nodo del tipo ProgLetInNode
     * @return il nodo analizzato
     * @throws IncomplException l'espressione non è compelta
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(ProgLetInNode node) throws TypeException {
        if (this.print) this.printNode(node);
        for (Node dec : node.declarations)
            try {
                this.visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        return this.visit(node.exp);
    }

    /**
     * controlla che il ProgNode sia visitabile, altrimenti lancia l'eccezione TypeException
     *
     * @param  node un nodo del tipo ProgLetInNode
     * @return il nodo analizzato
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(ProgNode node) throws TypeException {
        if (this.print) this.printNode(node);
        return this.visit(node.exp);
    }

    /**
     * Controlla che il FunNode sia visitabile, altrimenti lancia l'eccezione TypeException o IncomplException
     * Controlla anche che il tipo del nodo sia figlio del macrotipo a cui appartiente
     *
     * @param  node un nodo del tipo ProgLetInNode
     * @return null
     * @throws IncomplException l'espressione non è compelta
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(FunNode node) throws TypeException {
        if (this.print) this.printNode(node, node.id);
        for (Node dec : node.declarations)
            try {
                this.visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        if (!isSubtype(this.visit(node.exp), this.ckvisit(node.returnType)))
            throw new TypeException("Wrong return type for function " + node.id, node.getLine());
        return null;
    }

    /**
     * Controlla che il VarNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che il tipo del nodo sia figlio del macrotipo a cui appartiente
     *
     * @param  node un nodo del tipo ProgLetInNode
     * @return null
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(VarNode node) throws TypeException {
        if (this.print) this.printNode(node, node.id);
        if (!isSubtype(this.visit(node.exp), this.ckvisit(node.getType())))
            throw new TypeException("Incompatible value for variable " + node.id, node.getLine());
        return null;
    }

    /**
     * Controlla che il PrintNode sia visitabile, altrimenti lancia l'eccezione TypeException
     *
     * @param  node un nodo del tipo PrintNode
     * @return una visita al nodo dell'espressione
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(PrintNode node) throws TypeException {
        if (this.print) this.printNode(node);
        return this.visit(node.exp);
    }

    /**
     * Controlla che l'IfNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che l'espressione sia sottotipo del macrotipo "booleano"
     *
     * @param  node un nodo del tipo IfNode
     * @return una visita al nodo dell'espressione
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(IfNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.cond), new BoolTypeNode()))) {
            throw new TypeException("Non boolean condition in if", node.getLine());
        }

        TypeNode thenBranch = this.visit(node.thenBranch);
        TypeNode elseBranch = this.visit(node.elseBranch);
        if (isSubtype(thenBranch, elseBranch)) return elseBranch;
        if (isSubtype(elseBranch, thenBranch)) return thenBranch;

        final TypeNode returnType = lowestCommonAncestor(thenBranch, elseBranch);
        if (returnType == null) {
            throw new TypeException("Incompatible types in then-else branches", node.getLine());
        }

        return returnType;
    }

    /**
     * Controlla che l'EqualNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi delle due espressioni siano compatibili
     *
     * @param  node un nodo del tipo EqualNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(EqualNode node) throws TypeException {
        if (this.print) this.printNode(node);
        TypeNode l = this.visit(node.left);
        TypeNode r = this.visit(node.right);
        if (!(isSubtype(l, r) || isSubtype(r, l)))
            throw new TypeException("Incompatible types in equal", node.getLine());
        return new BoolTypeNode();
    }

    /**
     * Controlla che il TimesNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero'
     *
     * @param  node un nodo del tipo TimesNode
     * @return un IntTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(TimesNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Non integers in multiplication", node.getLine());
        return new IntTypeNode();
    }

    /**
     * Controlla che il PlusNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero
     *
     * @param  node un nodo del tipo PlusNode
     * @return un IntTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(PlusNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Non integers in sum", node.getLine());
        return new IntTypeNode();
    }

    /**
     * Controlla che il CallNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che le parentesi siano messe nel punto giusto e glia ttributi passati siano del tipo
     * coretto
     *
     * @param  node un nodo del tipo CallNode
     * @return un il tipo di un ArrowType
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(CallNode node) throws TypeException {
        if (this.print) this.printNode(node, node.id);
        TypeNode typeNode = this.visit(node.entry);

        if (typeNode instanceof  MethodTypeNode methodTypeNode) {
            typeNode = methodTypeNode.functionalType;
        }

        if (!(typeNode instanceof ArrowTypeNode arrowTypeNode)) {
            throw new TypeException("Invocation of a non-function " + node.id, node.getLine());
        }

        if (!(arrowTypeNode.parameters.size() == node.arguments.size())) {
            throw new TypeException("Wrong number of parameters in the invocation of " + node.id, node.getLine());
        }

        for (int i = 0; i < node.arguments.size(); i++)
            if (!(isSubtype(this.visit(node.arguments.get(i)), arrowTypeNode.parameters.get(i))))
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + node.id, node.getLine());

        return arrowTypeNode.returnType;
    }

    /**
     * Controlla che l'IdNode sia visitabile
     *
     * @param node un IdNode
     * @return Un typenode che identifica il tipo del nodo
     * @throws TypeException
     */
    @Override
    public TypeNode visitNode(IdNode node) throws TypeException {
        if (this.print) this.printNode(node, node.id);
        TypeNode typeNode = this.visit(node.entry);
        if (typeNode instanceof ArrowTypeNode)
            throw new TypeException("Wrong usage of function identifier " + node.id, node.getLine());
        return typeNode;
    }

    /**
     * Stampa le informazioni relative al nodo e al suo tipo (BoolTypeNode)
     *
     * @param node un BoolNode
     * @return Un BoolTypeNode
     */
    @Override
    public TypeNode visitNode(BoolNode node) {
        if (this.print) this.printNode(node, node.value.toString());
        return new BoolTypeNode();
    }

    /**
     * Stampa le informazioni relative al nodo e al suo tipo (IntNode)
     *
     * @param node un IntNode
     * @return Un IntTypeNode
     */
    @Override
    public TypeNode visitNode(IntNode node) {
        if (this.print) this.printNode(node, node.value.toString());
        return new IntTypeNode();
    }

// gestione tipi incompleti	(se lo sono lancia eccezione)

    /**
     * Visita i parametri di un nodo "ArrowType" ovvero un riferimento al ritorno di un metodo
     * o dei parametri di un oggetto
     *
     * @param node un ArrowTypeNode
     * @return null
     */
    @Override
    public TypeNode visitNode(ArrowTypeNode node) throws TypeException {
        if (this.print) this.printNode(node);
        for (Node par : node.parameters) this.visit(par);
        this.visit(node.returnType, "->"); //marks return type
        return null;
    }

    /**
     * Visita un nodo BoolTypeNode e ne stampa il tipo
     *
     * @param node un BoolTypeNode
     * @return null
     */
    @Override
    public TypeNode visitNode(BoolTypeNode node) {
        if (this.print) this.printNode(node);
        return null;
    }

    /**
     * Visita un nodo IntTypeNode e ne stampa il tipo
     *
     * @param node un IntTypeNode
     * @return null
     */
    @Override
    public TypeNode visitNode(IntTypeNode node) {
        if (this.print) this.printNode(node);
        return null;
    }

// STentry (ritorna campo type)

    /**
     * Prende l'input del codice associabile alla Symbol Table e lo manda al metodo checkVisit
     * per controllare che sia visitabile
     *
     * @param entry una entry
     * @return il risultato della visita a quel simbolo
     */
    @Override
    public TypeNode visitSTentry(STentry entry) throws TypeException {
        if (this.print) this.printSTentry("type");
        return this.ckvisit(entry.type);
    }

    // new nodes
    /**
     * Controlla che il MinusNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero
     *
     * @param  node un nodo del tipo MinusNode
     * @return un IntTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(MinusNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Non integers in sum", node.getLine());
        return new IntTypeNode();
    }

    /**
     * Controlla che il DivNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero
     *
     * @param  node un nodo del tipo DivNode
     * @return un IntTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(DivNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Non integers in div", node.getLine());
        return new IntTypeNode();
    }

    /**
     * Controlla che il GreaterEqualNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero
     *
     * @param  node un nodo del tipo GreaterEqualNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(GreaterEqualNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Incompatible types in greaterEqual", node.getLine());
        return new BoolTypeNode();
    }

    /**
     * Controlla che il LessEqualNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo intero
     *
     * @param  node un nodo del tipo LessEqualNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(LessEqualNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new IntTypeNode())
                && isSubtype(this.visit(node.right), new IntTypeNode())))
            throw new TypeException("Incompatible types in lessEqual", node.getLine());
        return new BoolTypeNode();
    }

    /**
     * Controlla che il OrNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo booleano
     *
     * @param  node un nodo del tipo OrNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(OrNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new BoolTypeNode())
                && isSubtype(this.visit(node.right), new BoolTypeNode())))
            throw new TypeException("Non booleans in or", node.getLine());
        return new BoolTypeNode();
    }

    /**
     * Controlla che il AndNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che i tipi dell'espressione siano di tipo booleano
     *
     * @param  node un nodo del tipo AndNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(AndNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.left), new BoolTypeNode())
                && isSubtype(this.visit(node.right), new BoolTypeNode())))
            throw new TypeException("Non booleans in and", node.getLine());
        return new BoolTypeNode();
    }

    /**
     * Controlla che il NotNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che il tipo dell'espressione sia di tipo booleano
     *
     * @param  node un nodo del tipo NotNode
     * @return un BoolTypeNode
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(NotNode node) throws TypeException {
        if (this.print) this.printNode(node);
        if (!(isSubtype(this.visit(node.exp), new BoolTypeNode())))
            throw new TypeException("Non boolean in not", node.getLine());
        return new BoolTypeNode();
    }

    // OO Nodes

    /**
     * Controlla che il ClassNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Nell'ambito dell'ereditarietà controlla l'overriding dei campi e dei metodi
     *
     * @param  node un nodo del tipo ClassNode
     * @return null
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(ClassNode node) throws TypeException {
        if (this.print) this.printNode(node, node.classId);
        final boolean isSubClass = node.superId.isPresent();
        final String parent = isSubClass ? node.superId.get() : null;

        if (!isSubClass) {
            node.methods.forEach(method -> {
                try {
                    this.visit(method);
                } catch (TypeException e) {
                    System.out.println("Type checking error in a class declaration: " + e.text);
                }
            });

            return null;
        }

        // eredito, quindi aggiungo la mia classe in superType
        superType.put(node.classId, parent);
        final ClassTypeNode classType = (ClassTypeNode) node.getType();
        //Otimizzazione 2
        final ClassTypeNode superClassType = (ClassTypeNode) node.superEntry.type;

        //CAMPI: controllo che gli overriding siano corretti.
        //Per ogni campo calcolo la posizione che, in fields di superClassType,
        //corrisponde al suo offset. Se la pos è < allora ovveriding e faccio il check sottotipo
        for (final FieldNode field : node.fields) {
            int position = -field.offset - 1;
            final boolean isOverriding = position < superClassType.fields.size();
            if (isOverriding && !isSubtype(classType.fields.get(position), superClassType.fields.get(position))) {
                throw new TypeException("Wrong type for field " + field.id, field.getLine());
            }
        }

        //METODI: controllo che gli overriding siano corretti
        //Per ogni metodo calcolo la posizione che, in methods di superClassType,
        //corrisponde al suo offset. Se la pos è < allora ovveriding e faccio il check sottotipo
        for (final MethodNode method : node.methods) {
            int position = method.offset;
            final boolean isOverriding = position < superClassType.fields.size();
            if (isOverriding && !isSubtype(classType.methods.get(position), superClassType.methods.get(position))) {
                throw new TypeException("Wrong type for method " + method.id, method.getLine());
            }
        }

        return null;
    }

    /**
     * Controlla che il metodo di una classe sia visitabile altrimenti lancia un TypeException
     *
     * @param  node un nodo del tipo MethodNode
     * @return null
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(final MethodNode node) throws TypeException {
        if (this.print) this.printNode(node, node.id);

        for (final DecNode declaration : node.declarations) {
            try {
                this.visit(declaration);
            } catch (TypeException e) {
                System.out.println("Type checking error in a method declaration: " + e.text);
            }
        }
        // visit expression and check if it is a subtype of the return type
        if (!isSubtype(this.visit(node.exp), this.ckvisit(node.returnType))) {
            throw new TypeException("Wrong return type for method " + node.id, node.getLine());
        }

        return null;
    }

    /**
     * Visita un nodo EmptyNode e ne stampa il tipo
     *
     * @param node un EmptyNode
     * @return nodo del tipo EmptyTypeNode
     */
    @Override
    public TypeNode visitNode(final EmptyNode node) {
        if (this.print) this.printNode(node);
        return new EmptyTypeNode();
    }

    /**
     * Controlla che il ClassCallNode sia visitabile, altrimenti lancia l'eccezione TypeException
     *
     * @param  node un nodo del tipo ClassCallNode
     * @return il tipo dell'ArrowType, cioò del ritorno della chiamata
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(final ClassCallNode node) throws TypeException {
        if (this.print) this.printNode(node, node.objectId);

        // return anticipato per evitare NullPointerException su ClassCallNode errato
        if(Objects.isNull(node.methodEntry)) return null;

        TypeNode type = this.visit(node.methodEntry);

        // visit method, if it is a method type, get the functional type
        if (type instanceof MethodTypeNode methodTypeNode) {
            type = methodTypeNode.functionalType;
        }

        // if it is not an arrow type, throw an exception
        if (!(type instanceof ArrowTypeNode arrowTypeNode)) {
            throw new TypeException("Invocation of a non-function " + node.methodId, node.getLine());
        }

        // check if the number of parameters is correct
        if (arrowTypeNode.parameters.size() != node.args.size()) {
            throw new TypeException("Wrong number of parameters in the invocation of method " + node.methodId, node.getLine());
        }

        // check if the types of the parameters are correct
        for (int i = 0; i < node.args.size(); i++) {
            if (!(isSubtype(this.visit(node.args.get(i)), arrowTypeNode.parameters.get(i)))) {
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of method " + node.methodId, node.getLine());
            }
        }

        return arrowTypeNode.returnType;
    }

    /**
     * Controlla che il NewNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Controlla anche che il costruttore della classe sia corretto, con il giusto numero di parametri
     * e che siano del tipo giusto.
     *
     * @param  node un nodo del tipo NewNode
     * @return un nodo con il riferimento alla classe
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(final NewNode node) throws TypeException {
        if (this.print) this.printNode(node, node.classId);
        final TypeNode typeNode = this.visit(node.entry);

        if (!(typeNode instanceof ClassTypeNode classTypeNode)) {
            throw new TypeException("Invocation of a non-constructor " + node.classId, node.getLine());
        }

        if (classTypeNode.fields.size() != node.args.size()) {
            throw new TypeException("Wrong number of parameters in the invocation of constructor " + node.classId, node.getLine());
        }
        // check if the types of the parameters are correct
        for (int i = 0; i < node.args.size(); i++) {
            if (!(isSubtype(this.visit(node.args.get(i)), classTypeNode.fields.get(i)))) {
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of constructor " + node.classId, node.getLine());
            }
        }
        return new RefTypeNode(node.classId);
    }

    // OO Type Nodes

    /**
     * Controlla che il ClassTypeNode sia visitabile, altrimenti lancia l'eccezione TypeException
     * Prova a visitare anche tutti i metodi e campi della classe.
     *
     * @param  node un nodo del tipo NewNode
     * @return null
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(final ClassTypeNode node) throws TypeException {
        if (this.print) this.printNode(node);
        // Visit all fields and methods
        for (final TypeNode field : node.fields) this.visit(field);
        for (final MethodTypeNode method : node.methods) this.visit(method);
        return null;
    }

    /**
     * Controlla che il MethodNode sia visitabile, altrimenti lancia l'eccezione TypeException
     *
     * @param  node un nodo del tipo MethodTypeNode
     * @return null
     * @throws TypeException l'espressione non è corretta
     */
    @Override
    public TypeNode visitNode(final MethodTypeNode node) throws TypeException {
        if (this.print) this.printNode(node);
        // Visit all parameters and the return type
        for (final TypeNode parameter : node.functionalType.parameters) this.visit(parameter);
        this.visit(node.functionalType.returnType, "->");
        return null;
    }

    /**
     * Visita un nodo RefTypeNode e ne stampa il tipo
     *
     * @param node un RefTypeNode
     * @return null
     */
    @Override
    public TypeNode visitNode(final RefTypeNode node) {
        if (this.print) this.printNode(node);
        return null;
    }

    /**
     * Visita un nodo EmptyTypeNode e ne stampa il tipo
     *
     * @param node un EmptyTypeNode
     * @return null
     */
    @Override
    public TypeNode visitNode(final EmptyTypeNode node) {
        if (this.print) this.printNode(node);
        return null;
    }
}