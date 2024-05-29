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
	 * Il Frame Pointer tiene traccia del frame di attivazione corrente
	 * o dell'ambiente di esecuzione di una subroutine o di una funzione.
	 */
	private static final String STORE_FP = "sfp";
	/**
	 * Carica il valore di TM (Temporary Memory).
	 * TM è utilizzato per memorizzare temporaneamente valori durante l'esecuzione
	 * di un programma, come operazioni intermedie o area di lavoro per operazioni temporanee.
	 */
	private static final String LOAD_TM = "ltm";
	private static final String JUMP_SUBROUTINE = "js";
	private static final String PRINT = "print";
	private static final String BRANCH_EQUAL = "beq ";
	private static final String BRANCH = "b ";
	private static final String MULT = "mult";
	private static final String ADD = "add";
	private static final String LOAD_WORD = "lw"; //Carica la parola dallo stack.
	private static final String LOAD_FP = "lfp"; //Carica il valore di FP.
	private static final String SUB = "sub";
	private static final String DIV = "div";
	private static final String BRANCH_LESS_EQUAL = "bleq "; //Salto se il primo valore in cima allo stack è minore o uguale al secondo.
	private static final String LOAD_HEAP_POINTER = "lhp";
	private static final String STORE_WORD = "sw";
	private static final String STORE_HP = "shp";

	/**
	 * Le dispatch tables delle classi.
	 * Ogni dispatch tables è un elenco di etichette, una per ciascun metodo della classe.
	 */
	private final List<List<String>> dispatchTables = new ArrayList<>();

    public CodeGenerationASTVisitor() {
    }

	/*
	 * i metodi seguenti sono i visitatori che attraversano l'Abstract Syntax Tree generato dal parser
	 */

	/**
	 *
	 * @param node ProgLetInNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(ProgLetInNode node) {
        if (this.print) this.printNode(node);
        String declarationCode = null;
        for (Node dec : node.declarationlist) declarationCode = nlJoin(declarationCode, this.visit(dec));
        return nlJoin(
                PUSH + 0,      //push un valore fittizio sullo stack
                declarationCode,      //genera il codice per la dichiarazione e lo alloca
                this.visit(node.exp), //genera il codice per l'espressione
                HALT,                 //istruzione halt
                getCode()             //recupera il codice generato per le funzioni
        );
    }

	/**
	 *
	 * @param node ProgNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(ProgNode node) {
        if (this.print) this.printNode(node);
        return nlJoin(
                this.visit(node.expression),
                HALT   //interrompe l'esecuzione del programma
        );
    }

	/**
	 *
	 * @param node FunNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(FunNode node) {
		if (this.print) this.printNode(node, node.id);
		String declarationListCode = null;
		String popDeclarationsList = null;
		String popParametersList = null;
		for (Node declaration : node.declarationlist) {
			declarationListCode = nlJoin(declarationListCode, this.visit(declaration));
			popDeclarationsList = nlJoin(popDeclarationsList, POP);
		}
		for (final ParNode ignored : node.parameterlist) popParametersList = nlJoin(popParametersList, POP);
        String functionLabel = freshFunLabel();
        putCode(
                nlJoin(
                        functionLabel + ":",
                        COPY_FP,                     // imposta il frame-pointer sul valore dello stack-pointer
                        LOAD_RA,                     // carica il return address
                        declarationListCode,         // generate code for local declarations (they use the new $fp!!!)
                        this.visit(node.expression), // generate code for function body expression
                        STORE_TM,                    // set $tm to popped value (function result)
                        popDeclarationsList,         // remove local declarations from stack
                        STORE_RA,                    // set $ra to popped value
                        POP,                         // remove Access Link from stack
                        popParametersList,           // remove parameters from stack
                        STORE_FP,                    // set $fp to popped value (Control Link)
                        LOAD_FP,                     // load $tm value (function result)
                        LOAD_RA,                     // load $ra value
                        JUMP_SUBROUTINE              // jump to to popped address
                )
        );
        return PUSH + functionLabel;
    }

	/**
	 *
	 * @param node VarNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(VarNode node) {
        if (this.print) this.printNode(node, node.id);
        return this.visit(node.expression);  // generate code for the expression
    }

	/**
	 *
	 * @param node PrintNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(PrintNode node) {
        if (this.print) this.printNode(node);
        return nlJoin(
                this.visit(node.expression),
                PRINT
        );
    }

	/**
	 *
	 * @param node IfNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(IfNode node) {
        if (this.print) this.printNode(node);
        String l1 = freshLabel();
        String l2 = freshLabel();
        return nlJoin(
                this.visit(node.cond),
                PUSH + 1,
                BRANCH_EQUAL + l1,
                this.visit(node.elseNode),
                BRANCH + l2,
                l1 + ":",
                this.visit(node.thenNode),
                l2 + ":"
        );
    }

	/**
	 *
	 * @param node EqualNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(EqualNode node) {
        if (this.print) this.printNode(node);
        String trueLabel = freshLabel();
        String falseLabel = freshLabel();
        return nlJoin(
                this.visit(node.left),
                this.visit(node.right),
                BRANCH_EQUAL + trueLabel,
                PUSH + 0,
                BRANCH + falseLabel,
                trueLabel + ":",
                PUSH + 1,
                falseLabel + ":"
        );
    }

	/**
	 *
	 * @param node OrNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(OrNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
				this.visit(node.left),
				PUSH + 1,
				BRANCH_EQUAL + trueLabel,
				this.visit(node.right),
				PUSH + 1,
				BRANCH_EQUAL + trueLabel,
				PUSH + 0,
				BRANCH + endLabel,
				trueLabel + ":",
				PUSH + 1,
				endLabel + ":"
		);
	}

	/**
	 *
	 * @param node AndNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(AndNode node) {
		if (this.print) this.printNode(node);
		String falseLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
				this.visit(node.left),
				PUSH + 0,
				BRANCH_EQUAL + falseLabel,
				this.visit(node.right),
				PUSH + 0,
				BRANCH_EQUAL + falseLabel,
				PUSH + 1,
				BRANCH + endLabel,
				falseLabel + ":",
				PUSH + 0,
				endLabel + ":"
		);
	}

	/**
	 *
	 * @param node NotNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(NotNode node) {
		if (this.print) this.printNode(node);
		String itWasFalseLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
				this.visit(node.expression),
				PUSH + 0,
				BRANCH_EQUAL + itWasFalseLabel,
				PUSH + 0,
				BRANCH + endLabel,
				itWasFalseLabel + ":",
				PUSH + 1,
				endLabel + ":"
		);
	}

	/**
	 *
	 * @param node TimesNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(TimesNode node) {
        if (this.print) this.printNode(node);
        return nlJoin(
                this.visit(node.left),
                this.visit(node.right),
                MULT
        );
    }

	/**
	 *
	 * @param node PlusNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(PlusNode node) {
        if (this.print) this.printNode(node);
        return nlJoin(
                this.visit(node.left),
                this.visit(node.right),
                ADD
        );
    }

	/**
	 *
	 * @param node CallNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(CallNode node) {
        if (this.print) this.printNode(node, node.id);
        String argumentCode = null;
		String getAR = null;
		final String loadARAddress = node.entry.type instanceof MethodTypeNode ? LOAD_WORD : "";
        for (int i = node.argumentList.size() - 1; i >= 0; i--) argumentCode = nlJoin(argumentCode, this.visit(node.argumentList.get(i)));
        for (int i = 0; i < node.nestingLevel - node.entry.nl; i++) getAR = nlJoin(getAR, LOAD_WORD);
        return nlJoin(
				LOAD_FP,            //carica il Control Link (che è un puntatore all'id del chiamante)
				argumentCode,              //genera il codice per gli argomenti delle espressione in ordine inverso
				LOAD_FP,
				getAR,                     //restituisce l'indirizzo del frame contenente l'id della dichiarazione
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
	 *
	 * @param node IdNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(IdNode node) {
        if (this.print) this.printNode(node, node.id);
        String getAR = null;
        for (int i = 0; i < node.nestingLevel - node.entry.nl; i++) getAR = nlJoin(getAR, LOAD_WORD);
        return nlJoin(
                LOAD_FP, getAR,     // retrieve address of frame containing "id" declaration
                                           // by following the static chain (of Access Links)
                PUSH + node.entry.offset,
				ADD,                       // compute address of "id" declaration
                LOAD_WORD                  // load value of "id" variable
        );
    }

	/**
	 *
	 * @param node BoolNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(BoolNode node) {
        if (this.print) this.printNode(node, node.value.toString());
        return PUSH + (node.value ? 1 : 0);
    }

	/**
	 *
	 * @param node IntNode
	 * @return codice generato
	 */
    @Override
    public String visitNode(IntNode node) {
        if (this.print) this.printNode(node, node.value.toString());
        return PUSH + node.value;
    }

	/**
	 *
	 * @param node MinusNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(MinusNode node) {
		if (this.print) this.printNode(node);
		return nlJoin(
                this.visit(node.left),
                this.visit(node.right),
				SUB
		);
	}

	/**
	 *
	 * @param node DivNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(DivNode node) {
		if (this.print) this.printNode(node);
		return nlJoin(
                this.visit(node.left),
                this.visit(node.right),
				DIV
		);
	}

	/**
	 *
	 * @param node LessEqualNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(LessEqualNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.left),       //visita il valore e lo pusha nella cima dello stack
                this.visit(node.right),             //visita il valore e lo pusha nella cima dello stack
				BRANCH_LESS_EQUAL + trueLabel,      //se il primo è minore o uguale al secondo salta all'etichetta trueLabel
				PUSH + 0,                           //se è maggiore pusha 0 (false) nella cima dello stack
				BRANCH + endLabel,                  //e termina, saltando alla endLabel
				trueLabel + ":",                    //se è minore uguale
				PUSH + 1,                           //pusha 1 (true) nella cima dello stack
				endLabel + ":"                      //termina
		);
	}

	/**
	 *
	 * @param node GreaterEqualNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(GreaterEqualNode node) {
		if (this.print) this.printNode(node);
		String trueLabel = freshLabel();
		String endLabel = freshLabel();
		return nlJoin(
                this.visit(node.right),
                this.visit(node.left),
				BRANCH_LESS_EQUAL + trueLabel,
				PUSH + 0,
				BRANCH + endLabel,
				trueLabel + ":",
				PUSH + 1,
				endLabel + ":"
		);
	}

	/**
	 *
	 * @param node ClassNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(ClassNode node) {
		if (this.print) this.printNode(node, node.classId);
		final List<String> dispatchTable = new ArrayList<>();
        this.dispatchTables.add(dispatchTable);
		final boolean isSubclass = node.superClassEntry != null;
		if (isSubclass) {
			final List<String> superDispatchTable = this.dispatchTables.get(-node.superClassEntry.offset - 2);
			dispatchTable.addAll(superDispatchTable);
		}
		for (final MethodNode methodEntry : node.methodList) {
            this.visit(methodEntry);
			final boolean isOverriding = methodEntry.offset < dispatchTable.size();
			if (isOverriding)
				dispatchTable.set(methodEntry.offset, methodEntry.label);
			else
				dispatchTable.add(methodEntry.label);
		}
		String createDispatchTable = null;
		for (final String label : dispatchTable) {
			createDispatchTable = nlJoin(
					createDispatchTable,    //memorizza l'etichetta del metodo nel'heap
					PUSH + label,                  //pusha l'etichetta del metodo
					LOAD_HEAP_POINTER,             //pusha il puntatore dell'heap
					STORE_WORD,                    //memorizza l'etichetta del metodo nell'heap
					LOAD_HEAP_POINTER,             //pusha il puntatore dell'heap
					PUSH + 1,                      //pusha 1
					ADD,                           //incrementa il puntatore dell'heap
					STORE_HP                       //memorizza il puntatore dell'heap
			);
		}
		return nlJoin(
				LOAD_HEAP_POINTER,     //push heap pointer, l'indirizzo della dispatch table
				createDispatchTable           //codice generato per creare la dispatch table nell'heap
		);
	}

	/**
	 *
	 * @param node MethodNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(MethodNode node) {
		if (this.print) this.printNode(node);
		String declarationListCode = null;
		String popDeclarationsList = null;
		String popParametersList = null;
		for (final DecNode declaration : node.declarationList) {
			declarationListCode = nlJoin(declarationListCode, this.visit(declaration));
			popDeclarationsList = nlJoin(popDeclarationsList, POP);
		}
		for (final ParNode ignored : node.parameterList) popParametersList = nlJoin(popParametersList, POP);
		String methodLabel = freshFunLabel();
		node.label = methodLabel;
		putCode(
				nlJoin(
						methodLabel + ":",
						COPY_FP,                     //setta il frame-pointer con il valore dello stack-pointer
						LOAD_RA,                     //carica il valore del return address
						declarationListCode,         // genera il codice per le dichiarazioni locali usando un nuovo frame pointer
                        this.visit(node.expression), //genera il codice per il corpo dell'espressione della funzione
						STORE_TM,                    //setta la memoria temporanea al valore poppato, quindi con il risultato della funzione
						popDeclarationsList,         //rimuove le dichiarazioni locali dallo stack
						STORE_RA,                    //setta il return address al valore poppato
						POP,                         //rimuove l'Access Link dallo stack
						popParametersList,           //rimuove il parametri dallo stack
						STORE_FP,                    //setta il frame pointer al valore poppato, ovvero il control Link
						LOAD_TM,                     //carica il valore della memoria temporanea con il risultato della funzione
						LOAD_RA,                     //carica il valore nel return access
						JUMP_SUBROUTINE              //salta all'indirizzo poppato
				)
		);
		return null;
	}

	/**
	 *
	 * @param node NewNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(NewNode node) {
		if (this.print) this.printNode(node, node.classId);
		String putArgumentsOnStack = "";
		String loadArgumentsOnHeap = "";
		for (final Node argument : node.argumentList) putArgumentsOnStack = nlJoin(putArgumentsOnStack, this.visit(argument));
		for (final Node ignored : node.argumentList) {
			loadArgumentsOnHeap = nlJoin(
					loadArgumentsOnHeap,
					LOAD_HEAP_POINTER,              //pusha il puntatore dell'heap
					STORE_WORD,                     //memorizza l'etichetta della new nell'heap
					LOAD_HEAP_POINTER,              //pusha il puntatore dell'heap
					PUSH + 1,                       //pusha 1 per incrementarlo
					ADD,
					STORE_HP                        //lo memorizza sull'heap pointer
			);
		}
		return nlJoin(
				putArgumentsOnStack,                    //Aggiunge il codice per valutare gli argomenti
				loadArgumentsOnHeap,                           //Aggiunge il codice per spostare gli argomenti sull'heap
				PUSH + (ExecuteVM.MEMSIZE
						+ node.classSymbolTableEntry.offset),  //Pusha l'indirizzo dell'entry point nella VM
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
	 *
	 * @param node EmptyNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(EmptyNode node) {
		if (this.print) this.printNode(node);
		return PUSH + -1;
	}

	/**
	 *
	 * @param node ClassCallNode
	 * @return codice generato
	 */
	@Override
	public String visitNode(ClassCallNode node) {
		if (this.print) this.printNode(node, node.objectId);
		String argumentCode = null;
		String getARCode = null;
		for (int i = node.argumentList.size() - 1; i >= 0; i--)
			argumentCode = nlJoin(argumentCode, this.visit(node.argumentList.get(i)));
		for (int i = 0; i < node.nestingLevel - node.symbolTableEntry.nl; i++)
			getARCode = nlJoin(getARCode, LOAD_WORD);
		return nlJoin(
				LOAD_FP,             //Carica il Control Link (puntatore al frame della funzione chiamante di "id")
				argumentCode,               //Genera il codice per le espressioni degli argomenti nell'ordine invertito
				LOAD_FP, getARCode,
				PUSH + node.symbolTableEntry.offset,
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