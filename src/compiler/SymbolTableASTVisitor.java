package compiler;

import compiler.AST.*;
import compiler.exc.VoidException;
import compiler.lib.BaseASTVisitor;
import compiler.lib.TypeNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Questa classe implementa la fase di collegamento del compilatore.
 * Usa ASVisitor per visitare l'AST e costruire la symbol table.
 * La symbol table viene utilizzata per collegare gli identificatori alle loro dichiarazioni.
 * AST dopo la visita viene chiamata enriched AST (AST arricchito).
 */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void, VoidException> {
    /**
     * Tiene la virtualTable delle classi
     * E' una mappa che mappa l'id di un metodo in una STentry.
     * Viene utilizzata come alias della classe HashMap<Stringa, STentry>.
     */
    static class VirtualTable extends HashMap<String, STentry> {
    }
    /**
     * La ClassTable è usata per salvare le virtual table delle classi.
     */
    private final Map<String, VirtualTable> classTable = new HashMap<>();
    /**
     * La symbolTable è usata per salvare la symbol table di ogni scope.
     * È un elenco di mappe, perché ogni scope è una mappa.
     * Il primo elemento dell'elenco è l'ambito globale.
     * L'ultimo elemento dell'elenco è l'ambito corrente.
     */
    private final List<Map<String, STentry>> symbolTable = new ArrayList<>();
    /**
     * Il nestingLevel viene utilizzato per tenere traccia del livello di nesting level corrente.
     */
    private int nestingLevel = 0; // current nesting level
    /**
     * Il decOffset è usato per tenere traccia dell'offset delle dichiarazioni locali
     * al livello di nesting level corrente.
     * È inizializzato a -2 perché $fp punta al primo argomento della funzione (offset 0)
     * e l'elemento successivo sullo stack è l'indirizzo di ritorno che ha offset -1.
     */
    private int decOffset = -2; // counter for offset of local declarations at current nesting level
    public int stErrors = 0;

    public SymbolTableASTVisitor() {
    }

    SymbolTableASTVisitor(boolean debug) {
        super(true, debug);
    } // enables print for debugging


    /**
     * Effettua una ricerca nella tabella dei simboli per l'id dato.
     * La ricerca parte dal livello di nidificazione corrente e va avanti
     * finché non viene trovata la prima occorrenza di id.
     * Se non viene trovata alcuna voce, viene restituito null.
     * Se viene trovata una voce, questa viene restituita.
     *
     * @param id l'id da cercare
     * @return la voce trovata o null
     */
    private STentry stLookup(String id) {
        int j = nestingLevel;
        STentry entry = null;
        while (j >= 0 && entry == null)
            entry = symbolTable.get(j--).get(id);
        return entry;
    }
    /**
     * Visita un ProgLetInNode.
     * Viene creato un nuovo scope e vengono visitate le dichiarazioni.
     * Quindi viene visitata l'espressione.
     * Lo scope viene quindi rimosso.
     *
     * @param node il ProgLetInNode da visitare
     * @return null
     */
    @Override
    public Void visitNode(ProgLetInNode node) {
        if (print) printNode(node);
        symbolTable.add(new HashMap<>());
        node.declarations.forEach(this::visit);
        visit(node.exp);
        symbolTable.remove(0);
        return null;
    }
    /**
     * Visita un ProgNode.
     * L'espressione viene visitata.
     *
     * @param node il ProgNode da visitare
     * @return null
     */
    @Override
    public Void visitNode(ProgNode node) {
        if (print) printNode(node);
        visit(node.exp);
        return null;
    }

    /* *******************
     *********************
     * Basic Declaration Nodes
     *********************
     ******************* */

    /**
     * Visitare un FunNode.
     * Crea STentry per la funzione e lo aggiunge alla symbol table corrente.
     * Creare un nuovo scope per i parametri della funzione e aggiungerli alla nuova symbol table
     * Quindi visita ogni dichiarazione e corpo della funzione e rimuovere lo scope
     *
     * @param node the FunNode to visit
     * @return null
     */
    @Override
    public Void visitNode(FunNode node) {
        if (print) printNode(node);

        final Map<String, STentry> currentSymbolTable = symbolTable.get(nestingLevel);
        final List<TypeNode> parametersTypes = node.parameters.stream().map(ParNode::getType).collect(Collectors.toList());
        final ArrowTypeNode arrowTypeNode = new ArrowTypeNode(parametersTypes, node.returnType);
        final STentry entry = new STentry(nestingLevel, arrowTypeNode, decOffset--);

        // inserimento di ID nella symbol table
        if (currentSymbolTable.put(node.id, entry) != null) {
            System.out.println("Fun id " + node.id + " at line " + node.getLine() + " already declared");
            stErrors++;
        }
        // creare una nuova hashmap per la symTable
        // ovvero un nuovo scope per i parametri della funzione
        nestingLevel++;
        // salva il contatore per offset delle diciarazioni al livello di nesting level precedente
        int prevNLDecOffset = decOffset;
        // re-inizializza il contatore per l'offset delle dichiarazioni al livello di nesting level corrente
        decOffset = -2;
        final Map<String, STentry> newSymbolTable = new HashMap<>();
        symbolTable.add(newSymbolTable);

        int parOffset = 1;
        for (ParNode par : node.parameters) {
            final STentry parEntry = new STentry(nestingLevel, par.getType(), parOffset++);
            if (newSymbolTable.put(par.id, parEntry) != null) {
                System.out.println("Par id " + par.id + " at line " + node.getLine() + " already declared");
                stErrors++;
            }
        }
        node.declarations.forEach(this::visit);
        visit(node.exp);

        //rimuovere la hashmap corrente poiche' esco dallo scope
        symbolTable.remove(nestingLevel);
        // ripristina il contatore per l'offset delle dichiarazioni al livello di nesting level precedente
        decOffset = prevNLDecOffset;
        nestingLevel--;
        return null;
    }
    /**
     * Visitare un VarNode.
     * Creare STentry per la variabile e l'aggiunge alla symbol table corrente.
     * Visitare l'espressione.
     *
     * @param node the VarNode to visit
     * @return null
     */
    @Override
    public Void visitNode(VarNode node) {
        if (print) printNode(node);
        visit(node.exp);
        final Map<String, STentry> currentSymbolTable = symbolTable.get(nestingLevel);
        final STentry entry = new STentry(nestingLevel, node.getType(), decOffset--);
        //inserimento di ID nella symbol table
        if (currentSymbolTable.put(node.id, entry) != null) {
            System.out.println("Var id " + node.id + " at line " + node.getLine() + " already declared");
            stErrors++;
        }
        return null;
    }

    /* *******************
     *********************
     * Operators Nodes
     *********************
     ******************* */

    /**
     * Visita un IfNode.
     * Visita la condizione, poi il ramo e il ramo else.
     *
     * @param node l'IfNode da visitare
     * @return null
     */
    @Override
    public Void visitNode(IfNode node) {
        if (print) printNode(node);
        visit(node.cond);
        visit(node.thenBranch);
        visit(node.elseBranch);
        return null;
    }
    /**
     * Visitare un NotNode.
     * Visita l'espressione.
     *
     * @param node the NotNode to visit
     * @return null
     */
    @Override
    public Void visitNode(NotNode node) {
        if (print) printNode(node);
        visit(node.exp);
        return null;
    }
    /**
     * Visita un OrNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the OrNode to visit
     * @return null
     */
    @Override
    public Void visitNode(OrNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visita un AndNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the AndNode to visit
     * @return null
     */
    @Override
    public Void visitNode(AndNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visita un EqualNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the EqualNode to visit
     * @return null
     */
    @Override
    public Void visitNode(EqualNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visitare un LessEqualNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the LessEqualNode to visit
     * @return null
     */
    @Override
    public Void visitNode(LessEqualNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visitare un nodo GreaterEqual.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the GreaterEqualNode to visit
     * @return null
     */
    @Override
    public Void visitNode(GreaterEqualNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visitare un TimesNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the TimesNode to visit
     * @return null
     */
    @Override
    public Void visitNode(TimesNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visita un DivNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the DivNode to visit
     * @return null
     */
    @Override
    public Void visitNode(DivNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visitare un PlusNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the PlusNode to visit
     * @return null
     */
    @Override
    public Void visitNode(PlusNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }
    /**
     * Visitare un MinusNode.
     * Visita l'espressione sinistra e destra.
     *
     * @param node the MinusNode to visit
     * @return null
     */
    @Override
    public Void visitNode(MinusNode node) {
        if (print) printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    /* *******************
     *********************
     * Values Nodes
     *********************
     ******************* */

    /**
     * Visitare un BoolNode.
     *
     * @param node the BoolNode to visit
     * @return null
     */
    @Override
    public Void visitNode(BoolNode node) {
        if (print) printNode(node, node.value.toString());
        return null;
    }
    /**
     * Visitare un IntNode.
     *
     * @param node the IntNode to visit
     * @return null
     */
    @Override
    public Void visitNode(IntNode node) {
        if (print) printNode(node, node.value.toString());
        return null;
    }
    /**
     * Visitare un IdNode.
     * Cercare la variabile nella tabella dei simboli e impostare il livello di ingresso e di annidamento.
     *
     * @param node the IdNode to visit
     * @return null
     */
    @Override
    public Void visitNode(IdNode node) {
        if (print) printNode(node);
        STentry entry = stLookup(node.id);
        if (entry == null) {
            System.out.println("Var or Par id " + node.id + " at line " + node.getLine() + " not declared");
            stErrors++;
        } else {
            node.entry = entry;
            node.nestingLevel = nestingLevel;
        }
        return null;
    }
    /**
     * Visita un PrintNode.
     * Visita l'espressione.
     *
     * @param node il PrintNode da visitare
     * @return null
     */
    @Override
    public Void visitNode(PrintNode node) {
        if (print) printNode(node);
        visit(node.exp);
        return null;
    }
    /**
     * Visitare un CallNode.
     * Lookup la funzione nella symbol table e setta il STentry e il nesting level
     * Visitare gli argomenti.
     *
     * @param node the CallNode to visit
     * @return null
     */
    @Override
    public Void visitNode(CallNode node) {
        if (print) printNode(node);
        final STentry entry = stLookup(node.id);
        if (entry == null) {
            System.out.println("Fun id " + node.id + " at line " + node.getLine() + " not declared");
            stErrors++;
        } else {
            node.entry = entry;
            node.nestingLevel = nestingLevel;
        }
        node.arguments.forEach(this::visit);
        return null;
    }

    // *************************
    // *************************
    // OBJECT-ORIENTED EXTENSION
    // *************************
    // *************************
    /**
     * Visitare un ClassNode.
     * Controlla se la superclasse è dichiarata e setta l'entry della superclasse.
     * Creare un ClassTypeNode e setta i campi e i metodi della superclasse, se presenti.
     * Creare STentry e l'aggiunge alla symbol table globale.
     * Creare la virtual table che eredita i metodi della superclasse, se presenti.
     * Aggiungere la tabella virtuale alla tabella delle classi.
     * Aggiungere la nuova symbol table per i metodi e i campi.
     * Per ogni campo, visitato si creare la STentry, e arricchendo anche il classTypeNode.
     * Per ogni metodo, si arricchisce il classTypeNode e lo si visita
     * Rimuovere la symbol table per i metodi e i campi e ripristina il nesting level.
     *
     * @param node the ClassNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final ClassNode node) {
        if (print) printNode(node);

        ClassTypeNode tempClassTypeNode = new ClassTypeNode();
        final boolean isSubClass = node.superId.isPresent();
        final String superId = isSubClass ? node.superId.get() : null;

        if (isSubClass) {
            // controlla se è dichiarata una super classe
            if (classTable.containsKey(superId)) {
                final STentry superSTEntry = symbolTable.get(0).get(superId);
                final ClassTypeNode superTypeNode = (ClassTypeNode) superSTEntry.type;
                tempClassTypeNode = new ClassTypeNode(superTypeNode);
                node.superEntry = superSTEntry;
            } else {
                System.out.println("Class " + superId + " at line " + node.getLine() + " not declared");
                stErrors++;
            }
        }

        final ClassTypeNode classTypeNode = tempClassTypeNode;
        node.setType(classTypeNode);

        // Aggiunge l'id della classe alla tabella dello scope globale controllando i duplicati
        final STentry entry = new STentry(0, classTypeNode, decOffset--);
        final Map<String, STentry> globalScopeTable = symbolTable.get(0);
        if (globalScopeTable.put(node.classId, entry) != null) {
            System.out.println("Class id " + node.classId + " at line " + node.getLine() + " already declared");
            stErrors++;
        }

        // Aggiunge la tabella virtuale alla tabella delle classi
        final Set<String> visitedClassNames = new HashSet<>();
        final VirtualTable virtualTable = new VirtualTable();
        if (isSubClass) {
            final VirtualTable superClassVirtualTable = classTable.get(superId);
            virtualTable.putAll(superClassVirtualTable);
        }
        classTable.put(node.classId, virtualTable);

        symbolTable.add(virtualTable);
        // Setta l'offset dei campi
        nestingLevel++;
        int fieldOffset = -1;
        if (isSubClass) {
            final ClassTypeNode superTypeNode = (ClassTypeNode) symbolTable.get(0).get(superId).type;
            fieldOffset = -superTypeNode.fields.size() - 1;
        }

        // gestisce le dichiarazioni dei campi
        for (final FieldNode field : node.fields) {
            if (visitedClassNames.contains(field.id)) {
                System.out.println(
                        "Field with id " + field.id + " on line " + field.getLine() + " was already declared"
                );
                stErrors++;
            } else {
                visitedClassNames.add(field.id);
            }
            visit(field);

            STentry fieldEntry = new STentry(nestingLevel, field.getType(), fieldOffset--);
            final boolean isFieldOverridden = isSubClass && virtualTable.containsKey(field.id);
            if (isFieldOverridden) {
                final STentry overriddenFieldEntry = virtualTable.get(field.id);
                final boolean isOverridingAMethod = overriddenFieldEntry.type instanceof MethodTypeNode;
                if (isOverridingAMethod) {
                    System.out.println("Cannot override method " + field.id + " with a field");
                    stErrors++;
                } else {
                    fieldEntry = new STentry(nestingLevel, field.getType(), overriddenFieldEntry.offset);
                    classTypeNode.fields.set(-fieldEntry.offset - 1, fieldEntry.type);
                }
            } else {
                classTypeNode.fields.add(-fieldEntry.offset - 1, fieldEntry.type);
            }

            // aggiunge il campo alla virtual table
            virtualTable.put(field.id, fieldEntry);
            field.offset = fieldEntry.offset;
        }

        // setta l'offset dei metodi
        int prevDecOffset = decOffset;
        decOffset = 0;
        if (isSubClass) {
            final ClassTypeNode superTypeNode = (ClassTypeNode) symbolTable.get(0).get(superId).type;
            decOffset = superTypeNode.methods.size();
        }

        for (final MethodNode method : node.methods) {
            if (visitedClassNames.contains(method.id)) {
                System.out.println(
                        "Method with id " + method.id + " on line " + method.getLine() + " was already declared"
                );
                stErrors++;
            } else {
                visitedClassNames.add(method.id);
            }
            visit(method);
            final MethodTypeNode methodTypeNode = (MethodTypeNode) symbolTable.get(nestingLevel).get(method.id).type;
            classTypeNode.methods.add(method.offset, methodTypeNode);
        }

        // Rimuove la classe dalla symbol table
        symbolTable.remove(nestingLevel--);
        decOffset = prevDecOffset;
        return null;
    }
    /**
     * Visitare un FieldNode.
     *
     * @param node the FieldNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final FieldNode node) {
        if (print) printNode(node);
        return null;
    }
    /**
     * Visitare un MethodNode.
     * Crea il MethodTypeNode e la STentry che lo aggiunge alla symbol table
     * Se il metodo è sovrascritto a un altro metodo, verificare se la sovrascrittura è corretta.
     * Creare la nuova SymbolTable per lo scope del metodo.
     * Per ogni parametro, creare la STentry e aggiungerla alla symbol table
     * Visitare le dichiarazioni e l'espressione.
     * Infine, rimuovere lo scope del metodo dalla symbol table
     *
     * @param node the MethodNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final MethodNode node) {
        if (print) printNode(node);
        final Map<String, STentry> currentTable = symbolTable.get(nestingLevel);
        final List<TypeNode> params = node.parameters.stream().map(ParNode::getType).collect(Collectors.toList());
        final boolean isOverriding = currentTable.containsKey(node.id);
        final TypeNode methodType = new MethodTypeNode(params, node.returnType);
        STentry entry = new STentry(nestingLevel, methodType, decOffset++);

        if (isOverriding) {
            final var overriddenMethodEntry = currentTable.get(node.id);
            final boolean isOverridingAMethod = overriddenMethodEntry != null && overriddenMethodEntry.type instanceof MethodTypeNode;
            if (isOverridingAMethod) {
                entry = new STentry(nestingLevel, methodType, overriddenMethodEntry.offset);
                decOffset--;
            } else {
                System.out.println("Cannot override a class attribute with a method: " + node.id);
                stErrors++;
            }
        }

        node.offset = entry.offset;
        currentTable.put(node.id, entry);

        // si crea una nuova tabella per i metodi
        nestingLevel++;
        final Map<String, STentry> methodTable = new HashMap<>();
        symbolTable.add(methodTable);

        // setta l'offset delle dichiarazioni
        int prevDecOffset = decOffset;
        decOffset = -2;
        int parameterOffset = 1;

        for (final ParNode parameter : node.parameters) {
            final STentry parameterEntry = new STentry(nestingLevel, parameter.getType(), parameterOffset++);
            if (methodTable.put(parameter.id, parameterEntry) != null) {
                System.out.println("Par id " + parameter.id + " at line " + node.getLine() + " already declared");
                stErrors++;
            }
        }
        node.declarations.forEach(this::visit);
        visit(node.exp);

        // Remove the current nesting level symbolTable.
        // Rimuove il corrente nesting level della symbol table
        symbolTable.remove(nestingLevel--);
        decOffset = prevDecOffset;
        return null;
    }

    /* *******************
     *********************
     * Value Nodes
     *********************
     ******************* */

    /**
     * Visitare un EmptyNode.
     *
     * @param node the EmptyNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final EmptyNode node) {
        if (print) printNode(node);
        return null;
    }

    /* *******************
     *********************
     * Operations Nodes
     *********************
     ******************* */

    /**
     * Visitare un ClassCallNode.
     * Controlla se l'id dell'oggetto è stato dichiarato, facendo lookup nella symbol table.
     * Se l'id dell'oggetto non è stato dichiarato, stampa errore.
     * Se l'id dell'oggetto è stato dichiarato, verifica se il tipo è un RefTypeNode.
     * Se il tipo non è un RefTypeNode, stampa errore.
     * Se il tipo è un RefTypeNode, verifica se l'id del metodo è nella virtual table.
     * Se l'id del metodo non è nella tabella virtuale, stampa errore.
     * Se l'id del metodo è nella virtual table, setta STentry e il nesting level del nodo.
     * Finalmente, visita gli argomenti.
     *
     * @param node the ClassCallNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final ClassCallNode node) {
        if (print) printNode(node);
        final STentry entry = stLookup(node.objectId);

        if (entry == null) {
            System.out.println("Object id " + node.objectId + " was not declared");
            stErrors++;
        } else if (entry.type instanceof final RefTypeNode refTypeNode) {
            node.entry = entry;
            node.nestingLevel = nestingLevel;
            final VirtualTable virtualTable = classTable.get(refTypeNode.typeId);
            if (virtualTable.containsKey(node.methodId)) {
                node.methodEntry = virtualTable.get(node.methodId);
            } else {
                System.out.println("Object id " + node.objectId + " at line " + node.getLine() + " has no method " + node.methodId);
                stErrors++;
            }
        } else {
            System.out.println("Object id " + node.objectId + " at line " + node.getLine() + " is not a RefType");
            stErrors++;
        }

        node.args.forEach(this::visit);
        return null;
    }
    /**
     * Visitare un NewNode.
     * Verifica se l'id della classe è stato dichiarato, facendo lookup nella virtual table delle classi
     * Se l'id della classe non è stato dichiarato, stampa errore.
     * Se l'id della classe è stato dichiarato, setta STentry del nodo.
     * Infine, visita gli argomenti.
     *
     * @param node the NewNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final NewNode node) {
        if (print) printNode(node);
        if (!classTable.containsKey(node.classId)) {
            System.out.println("Class id " + node.classId + " was not declared");
            stErrors++;
        }


        node.entry = symbolTable.get(0).get(node.classId);
        node.args.forEach(this::visit);
        return null;
    }

    /* *******************
     *********************
     * OOP Type Nodes
     *********************
     ******************* */

    /**
     * Visitare un ClassTypeNode.
     *
     * @param node the ClassTypeNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final ClassTypeNode node) {
        if (print) printNode(node);
        return null;
    }
    /**
     * Visita un MethodTypeNode.
     *
     * @param node the MethodTypeNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final MethodTypeNode node) {
        if (print) printNode(node);
        return null;
    }
    /**
     * Visita un RefTypeNode.
     * Verifica se il tipo è stato dichiarato, facendo lookup nella virtual table delle classi.
     * Se l'id del tipo non è stato dichiarato, stampa errore.
     *
     * @param node the RefTypeNode to visit
     * @return null
     */
    @Override
    public Void visitNode(final RefTypeNode node) {
        if (print) printNode(node);
        if (!this.classTable.containsKey(node.typeId)) {
            System.out.println("Class with id: " + node.typeId + " on line: " + node.getLine() + " was not declared");
            stErrors++;
        }
        return null;
    }
    /**
     * Visitare un EmptyTypeNode.
     *
     * @param node the EmptyTypeNode to visit
     * @return null
     */
    @Override
    public Void visitNode(EmptyTypeNode node) {
        if (print) printNode(node);
        return null;
    }

}
