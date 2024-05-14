package compiler;

import compiler.AST.*;
import compiler.exc.VoidException;
import compiler.lib.BaseASTVisitor;
import compiler.lib.DecNode;
import compiler.lib.Node;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {
    /**
     * Aggiunge il valore passato in cima allo stack.
     */
    private static final String PUSH = "push ";

    /**
     * Interrompe l'esecuzione del programma.
     */
    private static final String HALT = "halt";

    /**
     * Estrae il valore dalla cima dello stack e lo restituisce.
     */
    private static final String POP = "pop";
    /**
     * Copia il valore di FP (Frame Pointer).
     * Il Frame Pointer viene utilizzato per tenere traccia del frame di attivazione corrente
     * o dell'ambiente di esecuzione di una subroutine o di una funzione.
     */
    private static final String COPY_FP = "cfp";

    /**
     * Carica il valore di RA (Return Address).
     * Il Return Address indica il punto nel programma da cui è stata chiamata una funzione.
     */
    private static final String LOAD_RA = "lra";

    /**
     * Memorizza il valore in TM (Temporary Memory).
     * TM è utilizzato per memorizzare temporaneamente valori durante l'esecuzione
     * (struttura dati utilizzata nei linguaggi di programmazione per gestire l'esecuzione delle subroutine o delle funzioni)
     * di un programma, come operazioni intermedie o area di lavoro per operazioni temporanee.
     */
    private static final String STORE_TM = "stm";

    /**
     * Memorizza il valore in RA (Return Address).
     * Il Return Address indica il punto nel programma da cui è stata chiamata una funzione.
     */
    private static final String STORE_RA = "sra";

    /**
     * Memorizza il valore in FP (Frame Pointer).
     * Il Frame Pointer viene utilizzato per tenere traccia del frame di attivazione corrente
     * (struttura dati utilizzata nei linguaggi di programmazione per gestire l'esecuzione delle subroutine o delle funzioni)
     * o dell'ambiente di esecuzione di una subroutine o di una funzione.
     */
    private static final String STORE_FP = "sfp";

    /**
     * Carica il valore di TM (Temporary Memory).
     * TM è utilizzato per memorizzare temporaneamente valori durante l'esecuzione
     * di un programma, come operazioni intermedie o area di lavoro per operazioni temporanee.
     */
    private static final String LOAD_TM = "ltm";


    /**
     * Salto a sottoprogramma.
     */
    private static final String JUMP_SUBROUTINE = "js";

    /**
     * Stampa il valore in cima allo stack.
     */
    private static final String PRINT = "print";

    /**
     * Salto se i due valori in cima allo stack sono uguali.
     */
    private static final String BRANCH_EQUAL = "beq ";

    /**
     * Salto incondizionato.
     */
    private static final String BRANCH = "b ";

    /**
     * Moltiplica i due valori in cima allo stack.
     */
    private static final String MULT = "mult";

    /**
     * Fa la somma dei due valori in cima allo stack e la pusha.
     */
    private static final String ADD = "add";

    /**
     * Carica la parola dallo stack.
     */
    private static final String LOAD_WORD = "lw";

    /**
     * Carica il valore di FP.
     */
    private static final String LOAD_FP = "lfp";

    /**
     * Sottrae i due valori in cima allo stack.
     */
    private static final String SUB = "sub";

    /**
     * Divide i due valori in cima allo stack.
     */
    private static final String DIV = "div";

    /**
     * Salto se il primo valore in cima allo stack è minore o uguale al secondo.
     */
    private static final String BRANCH_LESS_EQUAL = "bleq ";

    /**
     * Carica il puntatore all'heap.
     */
    private static final String LOAD_HEAP_POINTER = "lhp";

    /**
     * Memorizza la parola.
     */
    private static final String STORE_WORD = "sw";

    /**
     * Memorizza il puntatore all'heap.
     */
    private static final String STORE_HP = "shp";

    //COMMENTO
    private final List<List<String>> dispatchTables = new ArrayList<>();

    public CodeGenerationASTVisitor() {
    }


    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    } //enables print for debugging

    /**
     * Da qui cominciano i visitatori che attraversano l'Abstract Syntax Tree generato dal parser
     */

    /**
     * Genera il codice per il nodo ProgLetIn.
     *
     * @param node nodo ProgLetIn
     * @return codice generato per il nodo ProgLetIn
     */
    @Override
    public String visitNode(ProgLetInNode node) {
        if (print) printNode(node);
        String declarationsCode = null;
        for (Node dec : node.declarations)
            declarationsCode = nlJoin(declarationsCode, visit(dec));
        return nlJoin(
                PUSH + 0,           //mette un valore fittizio sullo stack
                declarationsCode,          //genera il codice per la dichiarazione e lo alloca
                visit(node.exp),           //genera il codice per l'espressione
                HALT,                      //istruzione halt
                getCode()                  //recupera il codice generato per le funzioni
        );
    }

    /**
     * Genera il codice per il nodo ProgNode.
     *
     * @param node nodo ProgNode
     * @return codice generato per il nodo ProgNode
     */
    @Override
    public String visitNode(ProgNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.exp),        //genera il codice per l'espressione
                HALT                    //interrompe l'esecuzione del programma
        );
    }

    /**
     *
     * Dichiarazioni basi dei nodi
     *
     */

    /**
     * Genera il codice per il nodo FunNode.
     *
     * @param node il nodo FunNode
     * @return il codice generato per il nodo FunNode
     */
    @Override
    public String visitNode(FunNode node) {
        if (print) printNode(node, node.id);
        String declarationsCode = null;
        String popDeclarationsCode = null;
        String popParametersCode = null;

        for (Node declaration : node.declarations) {
            declarationsCode = nlJoin(declarationsCode, visit(declaration));
            popDeclarationsCode = nlJoin(popDeclarationsCode, POP);
        }

        for (final ParNode parameter : node.parameters) {
            popParametersCode = nlJoin(popParametersCode, POP);
        }

        final String funLabel = freshFunLabel();
        putCode(
                nlJoin(
                        funLabel + ":",         //segnamo dove comincia la funzione
                        COPY_FP,                       //setta il frame-pointer con lo stack-pointer
                        LOAD_RA,                       //carica il return address
                        declarationsCode,              //generano codice per dichiarazioni locali (usando un nuovo frame pointer)
                        visit(node.exp),               //generano codice per il corpo delle espressioni di funzioni
                        STORE_TM,                      //setta la temporary memory con il risultato della funzione, ovvero del valore poppato
                        popDeclarationsCode,           //rimuove le dichiarazioni locali dallo stack
                        STORE_RA,                      //setta il return address al valore poppato
                        POP,                           //rimuove l'access link dallo stack
                        popParametersCode,             //rimuove i parametri dallo stack
                        STORE_FP,                      //setta il frame pointer con il valore poppato, ovvero il control link
                        LOAD_TM,                       //carica il valore nella memoria temporanea del risultato della funzione
                        LOAD_RA,                       //carica il return address
                        JUMP_SUBROUTINE                //salta all'indirizzo poppato
                )
        );
        return PUSH + funLabel;
    }

    /**
     * Genera il codice per il nodo VarNode.
     *
     * @param node il nodo VarNode
     * @return il codice generato per il nodo VarNode
     */
    @Override
    public String visitNode(VarNode node) {
        if (print) printNode(node, node.id);
        return visit(node.exp);         //genera il codice per questa espressione
    }

    @Override
    public String visitNode(PrintNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.exp),        //genera il codice per questa espressione
                PRINT                   //stampa l'istruzione
        );
    }

    /**
     * Genera il codice per il nodo IfNode.
     *
     * @param node il nodo IfNode
     * @return il codice generato per il nodo IfNode
     */
    @Override
    public String visitNode(IfNode node) {
        if (print) printNode(node);
        String thenLabel = freshLabel();    //assegna un'etichetta, una stringa con concatenato un numero crescente
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.cond),
                PUSH + "1",
                BRANCH_EQUAL + thenLabel,   //se sono uguali salta alla thenLabel
                visit(node.elseBranch),     //Istruzioni in caso di else
                BRANCH + endLabel,          //caso Else, salta le istruzioni dell'If a endLabel
                thenLabel + ":",
                visit(node.thenBranch),     //thenLabel: Istruzioni in caso di If
                endLabel + ":"              //endLabel
        );
    }

    /**
     * Genera il codice per il nodo EqualNode.
     *
     * @param node il nodo EqualNode
     * @return il codice generato per il nodo EqualNode
     */
    @Override
    public String visitNode(EqualNode node) {
        if (print) printNode(node);
        String trueLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.left),        //visita il valore e lo pusha nella cima dello stack
                visit(node.right),              //visita il valore e lo pusha nella cima dello stack
                BRANCH_EQUAL + trueLabel,       //se il primo è minore o uguale al secondo salta all'etichetta trueLabel
                PUSH + 0,                       //se è maggiore uguale pusha 0 (false) nella cima dello stack
                BRANCH + endLabel,              //e termina, saltando alla endLabel
                trueLabel + ":",                //se è minore uguale
                PUSH + 1,                       //pusha 1 (true) nella cima dello stack
                endLabel + ":"                  //termina
        );
    }

    /**
     * Genera il codice per il nodo TimesNode.
     *
     * @param node il nodo TimesNode
     * @return il codice generato per il nodo TimesNode
     */
    @Override
    public String visitNode(TimesNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.left),               //pusha il valore nella cima dello stack
                visit(node.right),              //pusha il valore nella cima dello stack
                MULT                            //li toglie dallo stack e li moltiplica, pushando il nuovo valore nella cima dello stack
        );
    }

    /**
     * Genera il codice per il nodo PlusNode.
     *
     * @param node il nodo PlusNode
     * @return il codice generato per il nodo PlusNode
     */
    @Override
    public String visitNode(PlusNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.left),               //pusha il valore nella cima dello stack
                visit(node.right),              //pusha il valore nella cima dello stack
                ADD                             //li toglie dallo stack e li somma, pushando il nuovo valore nella cima dello stack
        );
    }

    /**
     * Genera il codice per il nodo CallNode.
     *
     * @param node il nodo CallNode
     * @return il codice generato per il nodo CallNode
     */
    @Override
    public String visitNode(CallNode node) {
        if (print) printNode(node, node.id);

        final String loadARAddress = node.entry.type instanceof MethodTypeNode ? LOAD_WORD : "";

        String argumentsCode = null;
        String getARCode = null;

        final List<Node> reversedArgumentsCode = new ArrayList<>(node.arguments);
        Collections.reverse(reversedArgumentsCode);
        for (final Node argument : reversedArgumentsCode) {
            argumentsCode = nlJoin(argumentsCode, visit(argument));
        }
        for (int i = 0; i < node.nestingLevel - node.entry.nl; i++) {
            getARCode = nlJoin(getARCode, LOAD_WORD);
        }

        return nlJoin(
                LOAD_FP,            //carica il Control Link (che è un puntatore all'id del chiamante)
                argumentsCode,             //genera il codice per gli argomenti delle espressione in ordine inverso
                LOAD_FP,
                getARCode,                 //restituisce l'indirizzo del frame contenente l'id della dichiarazione
                                           // seguendo la static chain (dell'access link)
                STORE_TM,                  //setta il valore poppato nella temporary memory (con l'obiettivo di duplicare la cima dello stack
                LOAD_TM,                   //carica l'Access Link (il puntatore al frame dell'id della dichiarazione della funzione
                LOAD_TM,                   //duplica la cima dello stack
                loadARAddress,
                PUSH + node.entry.offset,
                ADD,                       //calcola l'indirizzo dell'id della dichiarazione
                LOAD_WORD,                 //carica l'indirizzo dell'id della funzione
                JUMP_SUBROUTINE            //saltare all'indirizzo a cui abbiamo fatto la pop (salvando l'indirizzo alla seguente istruzione nel return address
        );
    }

    /**
     * Genera il codice per il nodo IdNode.
     *
     * @param node il nodo IdNode
     * @return il codice generato per il nodo IdNode
     */
    @Override
    public String visitNode(IdNode node) {
        if (print) printNode(node, node.id);
        String getARCode = null;
        for (int i = 0; i < node.nestingLevel - node.entry.nl; i++)
            getARCode = nlJoin(getARCode, LOAD_WORD);
        return nlJoin(
                LOAD_FP, getARCode, //restituisce un indirizzo contenente l'id della dichiarazione
                                           //seguendo la static chain dell'Access Link
                PUSH + node.entry.offset,
                ADD,                       //calcola l'indirizzo della dichiarazione dell'id
                LOAD_WORD                  //carica il valore dell'id della variabile
        );
    }

    /**
     * Genera il codice per il nodo BoolNode.
     *
     * @param node il nodo BoolNode
     * @return il codice generato per il nodo BoolNode
     */
    @Override
    public String visitNode(BoolNode node) {
        if (print) printNode(node, String.valueOf(node.value));
        return PUSH + (node.value ? 1 : 0);        // pusha 1 se true, 0 se false
    }

    /**
     * Genera il codice per il nodo IntNode.
     *
     * @param node il nodo IntNode
     * @return il codice generato per il nodo IntNode
     */
    @Override
    public String visitNode(IntNode node) {
        if (print) printNode(node, node.value.toString());
        return PUSH + node.value;                  //pusha l'intero specificato
    }

    /**
     * Genera il codice per il nodo MinusNode.
     *
     * @param node il nodo MinusNode
     * @return il codice generato per il nodo MinusNode
     */
    @Override
    public String visitNode(MinusNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.left),                   //pusha il valore nella cima dello stack
                visit(node.right),                  //pusha il valore nella cima dello stack
                SUB                                 //li toglie dallo stack e li sottrae, pushando il nuovo valore nella cima dello stack
        );
    }

    /**
     * Genera il codice per il nodo DivNode.
     *
     * @param node il nodo DivNode
     * @return il codice generato per il nodo DivNode
     */
    @Override
    public String visitNode(DivNode node) {
        if (print) printNode(node);
        return nlJoin(
                visit(node.left),                   //pusha il valore nella cima dello stack
                visit(node.right),                  //pusha il valore nella cima dello stack
                DIV                                 //li toglie dallo stack e li sottrae, pushando il nuovo valore nella cima dello stack
        );
    }

    /**
     * Genera il codice per il nodo LessEqualNode.
     *
     * @param node il nodo LessEqualNode
     * @return il codice generato per il nodo LessEqualNode
     */
    @Override
    public String visitNode(LessEqualNode node) {
        if (print) printNode(node);
        String trueLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.left),              //visita il valore e lo pusha nella cima dello stack
                visit(node.right),                    //visita il valore e lo pusha nella cima dello stack
                BRANCH_LESS_EQUAL + trueLabel,        //se il primo è minore o uguale al secondo salta all'etichetta trueLabel
                PUSH + 0,                             //se è maggiore pusha 0 (false) nella cima dello stack
                BRANCH + endLabel,                    //e termina, saltando alla endLabel
                trueLabel + ":",                      //se è minore uguale
                PUSH + "1",                           //pusha 1 (true) nella cima dello stack
                endLabel + ":"                        //termina
        );
    }

    /**
     * Genera il codice per il nodo GreaterEqualNode.
     *
     * @param node il nodo GreaterEqualNode
     * @return il codice generato per il nodo GreaterEqualNode
     */
    @Override
    public String visitNode(GreaterEqualNode node) {
        if (print) printNode(node);
        String trueLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.right),         //visita il valore e lo pusha nella cima dello stack
                visit(node.left),                 //visita il valore e lo pusha nella cima dello stack
                BRANCH_LESS_EQUAL + trueLabel,    //se il primo è maggiore o uguale al secondo salta all'etichetta trueLabel
                PUSH + 0,                         //se è minore pusha 0 (false) nella cima dello stack
                BRANCH + endLabel,                //e termina, saltando alla endLabel
                trueLabel + ":",                  //se è maggiore uguale
                PUSH + 1,                         //pusha 1 (true) nella cima dello stack
                endLabel + ":"                    //termina
        );
    }

    /**
     * Genera il codice per il nodo OrNode.
     *
     * @param node il nodo OrNode
     * @return il codice generato per il nodo OrNode
     */
    @Override
    public String visitNode(OrNode node) {
        if (print) printNode(node);
        String trueLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.left),           //visita il valore e lo pusha nella cima dello stack
                PUSH + 1,                          //pusha 1
                BRANCH_EQUAL + trueLabel,          //se il valore visitato è uguale a 1, salta all'etichetta trueLabel
                visit(node.right),                 //se il primo non era vero, valuta comunque il secondo
                PUSH + 1,                          //pusha 1
                BRANCH_EQUAL + trueLabel,          //se questa condizione è vera salta all'etichetta trueLabel
                PUSH + 0,                          //se nessuna delle due condizioni è vera pusha 0
                BRANCH + endLabel,                 //salta per terminare
                trueLabel + ":",                   //se uno dei due booleani risulta vero arriva in questa sezione
                PUSH + 1,                          //e pusha 1 sulla cima dello stack
                endLabel + ":"                     //termina
        );
    }

    /**
     * Genera il codice per il nodo AndNode.
     *
     * @param node il nodo AndNode
     * @return il codice generato per il nodo AndNode
     */
    @Override
    public String visitNode(AndNode node) {
        if (print) printNode(node);
        String falseLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.left),              //visita il valore e lo pusha nella cima dello stack
                PUSH + 0,                             //pusha 0
                BRANCH_EQUAL + falseLabel,            //se il valore visitato è uguale a 0, false, salta all'etichetta falseLabel
                visit(node.right),                    //se il primo era vero, valuta comunque il secondo
                PUSH + 0,                             //pusha 0
                BRANCH_EQUAL + falseLabel,            //se il valore visitato è uguale a 0, false, salta all'etichetta falseLabel
                PUSH + 1,                             //altrimenti pusha 1
                BRANCH + endLabel,                    //salta alla endLabel per terminare
                falseLabel + ":",                     //nel caso una delle due condizione o entrambe siano risultate false arriva qui
                PUSH + 0,                             //pusha 0 in cima allo stack
                endLabel + ":"                        //termina
        );
    }

    /**
     * Genera il codice per il nodo NotNode.
     *
     * @param node il nodo NotNode
     * @return il codice generato per il nodo NotNode
     */
    @Override
    public String visitNode(NotNode node) {
        if (print) printNode(node);
        String itWasFalseLabel = freshLabel();
        String endLabel = freshLabel();
        return nlJoin(
                visit(node.exp),             //visita il valore e lo pusha nella cima dello stack
                PUSH + 0,                           //pusha 0
                BRANCH_EQUAL + itWasFalseLabel,     //se la condizione è falsa salta all'etichetta itWasFalseLabel
                PUSH + 0,                           //altrimenti pusha in cima allo stack 0, traendo che inizialmente la condizione era vera
                BRANCH + endLabel,                  //salta alla endLabel
                itWasFalseLabel + ":",              //se era falsa arriva qui
                PUSH + 1,                           //pusha 1, true, in cima allo stack
                endLabel + ":"                      //termina
        );
    }

    /**
     * Genera il codice per il nodo ClassNode.
     *
     * @param node il nodo ClassNode
     * @return il codice generato per il nodo ClassNode
     */
    @Override
    public String visitNode(ClassNode node) {
        if (print) printNode(node, node.classId);

        final List<String> dispatchTable = new ArrayList<>();
        dispatchTables.add(dispatchTable);

        final boolean isSubclass = node.superEntry != null;
        if (isSubclass) {
            final List<String> superDispatchTable = dispatchTables.get(-node.superEntry.offset - 2);
            dispatchTable.addAll(superDispatchTable);
        }

        for (final MethodNode methodEntry : node.methods) {
            visit(methodEntry);

            final boolean isOverriding = methodEntry.offset < dispatchTable.size();
            if (isOverriding) {
                dispatchTable.set(methodEntry.offset, methodEntry.label);
            } else {
                dispatchTable.add(methodEntry.label);
            }
        }

        String dispatchTableHeapCode = "";
        for (final String label : dispatchTable) {
            dispatchTableHeapCode = nlJoin(
                    dispatchTableHeapCode,          //memorizza l'etichetta del metodo nel'heap
                    PUSH + label,                          //pusha l'etichetta del metodo
                    LOAD_HEAP_POINTER,                     //pusha il puntatore dell'heap
                    STORE_WORD,                            //memorizza l'etichetta del metodo nell'heap
                    LOAD_HEAP_POINTER,                     //pusha il puntatore dell'heap
                    PUSH + 1,                              //pusha 1
                    ADD,                                   //incrementa il puntatore dell'heap
                    STORE_HP                               //memorizza il puntatore dell'heap
            );
        }

        return nlJoin(
                LOAD_HEAP_POINTER,
                dispatchTableHeapCode
        );
    }

    /**
     * Genera il codice per il nodo MethodNode.
     *
     * @param node il nodo MethodNode
     * @return il codice generato per il nodo MethodNode
     */
    @Override
    public String visitNode(MethodNode node) {
        if (print) printNode(node);

        String declarationsCode = null;
        String popDeclarationsCode = null;
        String popParametersCode = null;

        for (final DecNode declaration : node.declarations) {
            declarationsCode = nlJoin(declarationsCode, visit(declaration));
            popDeclarationsCode = nlJoin(popDeclarationsCode, POP);
        }

        for (final ParNode parameter : node.parameters) {
            popParametersCode = nlJoin(popParametersCode, POP);
        }

        String methodLabel = freshFunLabel();
        node.label = methodLabel;

        putCode(
                nlJoin(
                        methodLabel + ":",
                        COPY_FP,                    //setta il frame-pointer con il valore dello stack-pointer
                        LOAD_RA,                    //carica il valore del return address
                        declarationsCode,           // genera il codice per le dichiarazioni locali usando un nuovo frame pointer
                        visit(node.exp),            //genera il codice per il corpo dell'espressione della funzione
                        STORE_TM,                   //setta la memoria temporanea al valore poppato, quindi con il risultato della funzione
                        popDeclarationsCode,        //rimuove le dichiarazioni locali dallo stack
                        STORE_RA,                   //setta il return address al valore poppato
                        POP,                        //rimuove l'Access Link dallo stack
                        popParametersCode,          //rimuove il parametri dallo stack
                        STORE_FP,                   //setta il frame pointer al valore poppato, ovvero il control Link
                        LOAD_TM,                    //carica il valore della memoria temporanea con il risultato della funzione
                        LOAD_RA,                    //carica il valore nel return access
                        JUMP_SUBROUTINE             //salta all'indirizzo poppato
                )
        );
        return null;
    }

    /**
     * Genera il codice per il nodo NewNode.
     *
     * @param node il nodo NewNode
     * @return il codice generato per il nodo NewNode
     */
    @Override
    public String visitNode(NewNode node) {
        if (print) printNode(node, node.classId);

        String argumentsCode = "";
        String moveArgumentsOnHeapCode = "";

        for (final Node argument : node.args) {
            argumentsCode = nlJoin(argumentsCode, visit(argument));
        }

        for (final Node argument : node.args) {
            moveArgumentsOnHeapCode = nlJoin(
                    moveArgumentsOnHeapCode,
                    LOAD_HEAP_POINTER,              //pusha il puntatore dell'heap
                    STORE_WORD,                     //memorizza l'etichetta della new nell'heap
                    LOAD_HEAP_POINTER,              //pusha il puntatore dell'heap
                    PUSH + 1,                       //pusha 1 per incrementarlo
                    ADD,
                    STORE_HP                        //lo memorizza sull'heap pointer
            );
        }

        return nlJoin(
                argumentsCode,                          //Aggiunge il codice per valutare gli argomenti
                moveArgumentsOnHeapCode,                       //Aggiunge il codice per spostare gli argomenti sull'heap
                PUSH + (ExecuteVM.MEMSIZE + node.entry.offset),//Pusha l'indirizzo dell'entry point nella VM
                LOAD_WORD,                                     //Carica il valore dall'indirizzo specificato (entry point)
                LOAD_HEAP_POINTER,                             //Carica il puntatore all'heap
                STORE_WORD,                                    //Memorizza il valore (entry point) nell'heap
                LOAD_HEAP_POINTER,                             //Carica il puntatore all'heap
                LOAD_HEAP_POINTER,                             //Carica il puntatore all'heap nuovamente
                PUSH + 1,                                      //Pusha 1 per incrementarlo
                ADD,
                STORE_HP                                       //Memorizza il nuovo valore del puntatore all'heap
        );

    }

    /**
     * Genera il codice per il nodo EmptyNode.
     *
     * @return -1 sullo stack
     */
    @Override
    public String visitNode(EmptyNode n) {
        if (print) printNode(n);
        return PUSH + "-1";
    }

    /**
     * Genera il codice per il nodo ClassCallNode.
     *
     * @param node il nodo ClassCallNode
     * @return il codice generato per il nodo ClassCallNode
     */
    @Override
    public String visitNode(ClassCallNode node) {
        if (print) printNode(node, node.objectId);

        String argumentsCode = null;
        String getARCode = null;

        for (int i = node.args.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.args.get(i)));
        }

        for (int i = 0; i < node.nestingLevel - node.entry.nl; i++) {
            getARCode = nlJoin(getARCode, LOAD_WORD);
        }

        return nlJoin(
                LOAD_FP,             //Carica il Control Link (puntatore al frame della funzione chiamante di "id")
                argumentsCode,              //Genera il codice per le espressioni degli argomenti nell'ordine invertito
                LOAD_FP, getARCode,         //Recupera l'indirizzo del frame che contiene la dichiarazione di "id"
                                            //seguendo la catena statica (degli Access Links)
                PUSH + node.entry.offset,
                ADD,                        //Calcola l'indirizzo della dichiarazione di "id"
                LOAD_WORD,                  //Carica l'indirizzo della funzione "id"
                STORE_TM,                   //Imposta la memoria temporanea al valore estratto (con l'obiettivo di duplicare la cima dello stack)
                LOAD_TM,                    //Carica l'Access Link (puntatore al frame della dichiarazione della funzione "id")
                LOAD_TM,                    //Duplica la cima dello stack
                LOAD_WORD,                  //Carica l'indirizzo della tabella di dispatch
                PUSH + node.methodEntry.offset,
                ADD,
                LOAD_WORD,                  //Carica l'indirizzo del metodo
                JUMP_SUBROUTINE             //Salta all'indirizzo estratto (salvando l'indirizzo dell'istruzione successiva in $ra)
        );

    }

}