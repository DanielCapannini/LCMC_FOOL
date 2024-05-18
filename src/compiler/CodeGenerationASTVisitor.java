package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

	private static final String HALT = "halt";
    private static final String PUSH = "push ";
	private static final String POP = "pop";
	private static final String COPY_FP = "cfp";
	private static final String LOAD_RA = "lra";
	private static final String STORE_TM = "stm";
	private static final String STORE_RA = "sra";
	private static final String STORE_FP = "sfp";
	private static final String LOAD_TM = "ltm";
	private static final String JUMP_SUBROUTINE = "js";
	private static final String PRINT = "print";
	private static final String BRANCH_EQUAL = "beq ";
	private static final String BRANCH = "b ";
	private static final String MULT = "mult";
	private static final String ADD = "add";
	private static final String LOAD_WORD = "lw";
	private static final String LOAD_FP = "lfp";
	private static final String SUB = "sub";
	private static final String DIV = "div";
	private static final String BRANCH_LESS_EQUAL = "bleq ";
	private static final String LOAD_HEAP_POINTER = "lhp";
	private static final String STORE_WORD = "sw";
	private static final String STORE_HP = "shp";
	private final List<List<String>> dispatchTableList = new ArrayList<>();

    public CodeGenerationASTVisitor() {
    }

    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    } //enables print for debugging

    @Override
    public String visitNode(ProgLetInNode n) {
        if (this.print) this.printNode(n);
        String declarationCode = null;
        for (Node dec : n.declarationlist) declarationCode = nlJoin(declarationCode, this.visit(dec));
        return nlJoin(
                PUSH + 0,
                declarationCode, // generate code for declarations (allocation)
                this.visit(n.exp),
                HALT,
                getCode()
        );
    }

    @Override
    public String visitNode(ProgNode n) {
        if (this.print) this.printNode(n);
        return nlJoin(
                this.visit(n.exp),
                HALT
        );
    }

    @Override
    public String visitNode(FunNode n) {
		if (this.print) this.printNode(n, n.id);
		String declarationListCode = null;
		String popDeclaration = null;
		String popParameter = null;

		for (Node declaration : n.declarationlist) {
			declarationListCode = nlJoin(declarationListCode, this.visit(declaration));
			popDeclaration = nlJoin(popDeclaration, POP);
		}

		for (final ParNode ignored : n.parameterlist) {
			popParameter = nlJoin(popParameter, POP);
		}
        String funl = freshFunLabel();
        putCode(
                nlJoin(
                        funl + ":",
                        COPY_FP, // set $fp to $sp value
                        LOAD_RA, // load $ra value
                        declarationListCode, // generate code for local declarations (they use the new $fp!!!)
                        this.visit(n.exp), // generate code for function body expression
                        STORE_TM, // set $tm to popped value (function result)
                        popDeclaration, // remove local declarations from stack
                        STORE_RA, // set $ra to popped value
                        POP, // remove Access Link from stack
                        popParameter, // remove parameters from stack
                        STORE_FP, // set $fp to popped value (Control Link)
                        LOAD_FP, // load $tm value (function result)
                        LOAD_RA, // load $ra value
                        JUMP_SUBROUTINE  // jump to to popped address
                )
        );
        return PUSH + funl;
    }

    @Override
    public String visitNode(VarNode n) {
        if (this.print) this.printNode(n, n.id);
        return this.visit(n.exp);
    }

    @Override
    public String visitNode(PrintNode n) {
        if (this.print) this.printNode(n);
        return nlJoin(
                this.visit(n.exp),
                PRINT
        );
    }

    @Override
    public String visitNode(IfNode n) {
        if (this.print) this.printNode(n);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                this.visit(n.cond),
                PUSH + 1,
                BRANCH_EQUAL + l1,
                this.visit(n.elseNode),
                BRANCH + l2,
                l1 + ":",
                this.visit(n.thenNode),
                l2 + ":"
        );
    }

    @Override
    public String visitNode(EqualNode n) {
        if (this.print) this.printNode(n);
        String trueLabel = freshLabel();
        String falseLabel = freshLabel();
        return nlJoin(
                this.visit(n.left),
                this.visit(n.right),
                BRANCH_EQUAL + trueLabel,
                PUSH + 0,
                BRANCH + falseLabel,
                trueLabel + ":",
                PUSH + 1,
                falseLabel + ":"
        );
    }

    @Override
    public String visitNode(TimesNode n) {
        if (this.print) this.printNode(n);
        return nlJoin(
                this.visit(n.left),
                this.visit(n.right),
                MULT
        );
    }

    @Override
    public String visitNode(PlusNode n) {
        if (this.print) this.printNode(n);
        return nlJoin(
                this.visit(n.left),
                this.visit(n.right),
                ADD
        );
    }

    @Override
    public String visitNode(CallNode n) {
        if (this.print) this.printNode(n, n.id);
        String argumentCode = null, getAR = null;
		final String loadARAddress = n.entry.type instanceof MethodTypeNode ? LOAD_WORD : "";
        for (int i = n.argumentList.size() - 1; i >= 0; i--) argumentCode = nlJoin(argumentCode, this.visit(n.argumentList.get(i)));
        for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) getAR = nlJoin(getAR, LOAD_WORD);
        return nlJoin(
				LOAD_FP,            //carica il Control Link (che è un puntatore all'id del chiamante)
				argumentCode,             //genera il codice per gli argomenti delle espressione in ordine inverso
				LOAD_FP,
				getAR,                 //restituisce l'indirizzo del frame contenente l'id della dichiarazione
				// seguendo la static chain (dell'access link)
				STORE_TM,                  //setta il valore poppato nella temporary memory (con l'obiettivo di duplicare la cima dello stack
				LOAD_TM,                   //carica l'Access Link (il puntatore al frame dell'id della dichiarazione della funzione
				LOAD_TM,                   //duplica la cima dello stack
				loadARAddress,
				PUSH + n.entry.offset,
				ADD,                       //calcola l'indirizzo dell'id della dichiarazione
				LOAD_WORD,                 //carica l'indirizzo dell'id della funzione
				JUMP_SUBROUTINE            //saltare all'indirizzo a cui abbiamo fatto la pop (salvando l'indirizzo alla seguente istruzione nel return address
		);
    }

    @Override
    public String visitNode(IdNode n) {
        if (this.print) this.printNode(n, n.id);
        String getAR = null;
        for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) getAR = nlJoin(getAR, LOAD_WORD);
        return nlJoin(
                LOAD_FP, getAR, // retrieve address of frame containing "id" declaration
                // by following the static chain (of Access Links)
                PUSH + n.entry.offset, ADD, // compute address of "id" declaration
                LOAD_WORD // load value of "id" variable
        );
    }

    @Override
    public String visitNode(BoolNode n) {
        if (this.print) this.printNode(n, n.value.toString());
        return PUSH + (n.value ? 1 : 0);
    }

    @Override
    public String visitNode(IntNode n) {
        if (this.print) this.printNode(n, n.value.toString());
        return PUSH + n.value;
    }

	@Override
	public String visitNode(MinusNode node) {
		if (this.print) this.printNode(node);
		return nlJoin(
                this.visit(node.left),                   //pusha il valore nella cima dello stack
                this.visit(node.right),                  //pusha il valore nella cima dello stack
				SUB                                 //li toglie dallo stack e li sottrae, pushando il nuovo valore nella cima dello stack
		);
	}

	@Override
	public String visitNode(DivNode node) {
		if (this.print) this.printNode(node);
		return nlJoin(
                this.visit(node.left),                   //pusha il valore nella cima dello stack
                this.visit(node.right),                  //pusha il valore nella cima dello stack
				DIV                                 //li toglie dallo stack e li sottrae, pushando il nuovo valore nella cima dello stack
		);
	}

	@Override
	public String visitNode(LessEqualNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.left),              //visita il valore e lo pusha nella cima dello stack
                this.visit(node.right),                    //visita il valore e lo pusha nella cima dello stack
				BRANCH_LESS_EQUAL + trueLabel,        //se il primo è minore o uguale al secondo salta all'etichetta trueLabel
				PUSH + 0,                             //se è maggiore pusha 0 (false) nella cima dello stack
				BRANCH + endLabel,                    //e termina, saltando alla endLabel
				trueLabel + ":",                      //se è minore uguale
				PUSH + 1,                           //pusha 1 (true) nella cima dello stack
				endLabel + ":"                        //termina
		);
	}

	@Override
	public String visitNode(GreaterEqualNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.right),         //visita il valore e lo pusha nella cima dello stack
                this.visit(node.left),                 //visita il valore e lo pusha nella cima dello stack
				BRANCH_LESS_EQUAL + trueLabel,    //se il primo è maggiore o uguale al secondo salta all'etichetta trueLabel
				PUSH + 0,                         //se è minore pusha 0 (false) nella cima dello stack
				BRANCH + endLabel,                //e termina, saltando alla endLabel
				trueLabel + ":",                  //se è maggiore uguale
				PUSH + 1,                         //pusha 1 (true) nella cima dello stack
				endLabel + ":"                    //termina
		);
	}

	@Override
	public String visitNode(OrNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.left),           //visita il valore e lo pusha nella cima dello stack
				PUSH + 1,                          //pusha 1
				BRANCH_EQUAL + trueLabel,          //se il valore visitato è uguale a 1, salta all'etichetta trueLabel
                this.visit(node.right),                 //se il primo non era vero, valuta comunque il secondo
				PUSH + 1,                          //pusha 1
				BRANCH_EQUAL + trueLabel,          //se questa condizione è vera salta all'etichetta trueLabel
				PUSH + 0,                          //se nessuna delle due condizioni è vera pusha 0
				BRANCH + endLabel,                 //salta per terminare
				trueLabel + ":",                   //se uno dei due booleani risulta vero arriva in questa sezione
				PUSH + 1,                          //e pusha 1 sulla cima dello stack
				endLabel + ":"                     //termina
		);
	}

	@Override
	public String visitNode(AndNode node) {
		if (this.print) this.printNode(node);
		String falseLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.left),              //visita il valore e lo pusha nella cima dello stack
				PUSH + 0,                             //pusha 0
				BRANCH_EQUAL + falseLabel,            //se il valore visitato è uguale a 0, false, salta all'etichetta falseLabel
                this.visit(node.right),                    //se il primo era vero, valuta comunque il secondo
				PUSH + 0,                             //pusha 0
				BRANCH_EQUAL + falseLabel,            //se il valore visitato è uguale a 0, false, salta all'etichetta falseLabel
				PUSH + 1,                             //altrimenti pusha 1
				BRANCH + endLabel,                    //salta alla endLabel per terminare
				falseLabel + ":",                     //nel caso una delle due condizione o entrambe siano risultate false arriva qui
				PUSH + 0,                             //pusha 0 in cima allo stack
				endLabel + ":"                        //termina
		);
	}

	@Override
	public String visitNode(NotNode node) {
		if (this.print) this.printNode(node);
		String itWasFalseLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.exp),             //visita il valore e lo pusha nella cima dello stack
				PUSH + 0,                           //pusha 0
				BRANCH_EQUAL + itWasFalseLabel,     //se la condizione è falsa salta all'etichetta itWasFalseLabel
				PUSH + 0,                           //altrimenti pusha in cima allo stack 0, traendo che inizialmente la condizione era vera
				BRANCH + endLabel,                  //salta alla endLabel
				itWasFalseLabel + ":",              //se era falsa arriva qui
				PUSH + 1,                           //pusha 1, true, in cima allo stack
				endLabel + ":"                      //termina
		);
	}

	@Override
	public String visitNode(ClassNode node) {
		if (this.print) this.printNode(node, node.classId);

		final List<String> dispatchTable2 = new ArrayList<>();
        this.dispatchTableList.add(dispatchTable2);

		final boolean isSubclass = node.superEntry != null;
		if (isSubclass) {
			final List<String> superDispatchTable = this.dispatchTableList.get(-node.superEntry.offset - 2);
			dispatchTable2.addAll(superDispatchTable);
		}

		for (final MethodNode methodEntry : node.methodList) {
            this.visit(methodEntry);

			final boolean isOverriding = methodEntry.offset < dispatchTable2.size();
			if (isOverriding) {
				dispatchTable2.set(methodEntry.offset, methodEntry.label);
			} else {
				dispatchTable2.add(methodEntry.label);
			}
		}

		String dispatchTableHeapCode = "";
		for (final String label : dispatchTable2) {
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

	@Override
	public String visitNode(MethodNode node) {
		if (this.print) this.printNode(node);

		String declarationsCode = null;
		String popDeclarationsCode = null;
		String popParametersCode = null;

		for (final DecNode declaration : node.declarationList) {
			declarationsCode = nlJoin(declarationsCode, this.visit(declaration));
			popDeclarationsCode = nlJoin(popDeclarationsCode, POP);
		}

		for (final ParNode ignored : node.parameterList) {
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
                        this.visit(node.exp),            //genera il codice per il corpo dell'espressione della funzione
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

	@Override
	public String visitNode(NewNode node) {
		if (this.print) this.printNode(node, node.classId);

		String argumentsCode = "";
		String moveArgumentsOnHeapCode = "";

		for (final Node argument : node.argumentList) {
			argumentsCode = nlJoin(argumentsCode, this.visit(argument));
		}

		for (final Node ignored : node.argumentList) {
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

	@Override
	public String visitNode(EmptyNode n) {
		if (this.print) this.printNode(n);
		return PUSH + "-1";
	}

	@Override
	public String visitNode(ClassCallNode node) {
		if (this.print) this.printNode(node, node.objectId);

		String argumentCode = null;
		String getARCode = null;

		for (int i = node.argumentList.size() - 1; i >= 0; i--) {
			argumentCode = nlJoin(argumentCode, this.visit(node.argumentList.get(i)));
		}

		for (int i = 0; i < node.nestingLevel - node.entry.nl; i++) {
			getARCode = nlJoin(getARCode, LOAD_WORD);
		}

		return nlJoin(
				LOAD_FP,             //Carica il Control Link (puntatore al frame della funzione chiamante di "id")
				argumentCode,              //Genera il codice per le espressioni degli argomenti nell'ordine invertito
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