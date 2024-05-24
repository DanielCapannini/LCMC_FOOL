package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

/**
 * Questa classe implementa la fase di collegamento del compilatore.
 * Utilizza l'ASVisitor per visitare l'AST e costruire la symbol table.
 * La symbol table viene utilizzata per collegare gli identificatori alle loro dichiarazioni.
 * Dopo la visita, l'AST viene arricchito e viene chiamato enriched AST
 */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

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
	 * Il primo elemento dell'elenco è l'ambito globale.
	 * L'ultimo elemento dell'elenco è l'ambito corrente.
	 */
	private final List<Map<String, STentry>> symbolTable = new ArrayList<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	public int stErrors=0;

	public SymbolTableASTVisitor() {}

	/**
	 * Effettua una ricerca nella tabella dei simboli per l'id dato.
	 *
	 * @param id l'id da cercare
	 * @return voce trovato o null
	 */
	private STentry stLookup(String id) {
		int j = this.nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = this.symbolTable.get(j--).get(id);
		return entry;
	}

	/**
	 * Viene creato un nuovo scope e vengono visitate le dichiarazioni.
	 * Lo scope viene quindi rimosso.
	 *
	 * @param node il ProgLetInNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(ProgLetInNode node) {
		if (this.print) this.printNode(node);
        this.symbolTable.add(new HashMap<>());
		for (Node declaration : node.declarationlist) this.visit(declaration);
        this.visit(node.exp);
        this.symbolTable.remove(0);
		return null;
	}

	/**
	 *
	 * @param node il ProgNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(ProgNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.expression);
		return null;
	}

	/**
	 * Crea un'istanza di STentry per la funzione e aggiungila alla symbol table corrente.
	 * Crea un nuovo scope per i parametri della funzione e aggiungili alla nuova symbol table.
	 * Successivamente, visita ogni dichiarazione e il corpo della funzione e rimuovi lo scope.
	 *
	 * @param node il FunNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(FunNode node) {
		if (this.print) this.printNode(node);
		Map<String, STentry> currentSymbolTable = this.symbolTable.get(this.nestingLevel);
		List<TypeNode> parameterTypeList = new ArrayList<>();
		for (ParNode par : node.parameterlist) parameterTypeList.add(par.getType());
		STentry entry = new STentry(this.nestingLevel, new ArrowTypeNode(parameterTypeList,node.returnType), this.decOffset--);
		//inserimento di ID nella symtable
		if (currentSymbolTable.put(node.id, entry) != null) {
			System.out.println("Fun id " + node.id + " at line "+ node.getLine() +" already declared");
            this.stErrors++;
		} 
		//creare una nuova hashmap per la symTable
        this.nestingLevel++;
		Map<String, STentry> newSymbolTable = new HashMap<>();
        this.symbolTable.add(newSymbolTable);
		int prevNLDecOffset= this.decOffset; // stores counter for offset of declarations at previous nesting level
        this.decOffset =-2;
		int parOffset=1;
		for (ParNode par : node.parameterlist)
			if (newSymbolTable.put(par.id, new STentry(this.nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ node.getLine() +" already declared");
                this.stErrors++;
			}
		for (Node dec : node.declarationlist) this.visit(dec);
        this.visit(node.expression);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
        this.symbolTable.remove(this.nestingLevel--);
        this.decOffset =prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	/**
	 *Creare STentry per la variabile e l'aggiunge alla symbol table corrente.
	 * Visitare l'espressione.
	 *
	 * @param node il VarNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(VarNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.expression);
		Map<String, STentry> currentSymbolTable = this.symbolTable.get(this.nestingLevel);
		STentry entry = new STentry(this.nestingLevel,node.getType(), this.decOffset--);
		//inserimento di ID nella symtable
		if (currentSymbolTable.put(node.id, entry) != null) {
			System.out.println("Var id " + node.id + " at line "+ node.getLine() +" already declared");
            this.stErrors++;
		}
		return null;
	}

	/**
	 *
	 * @param node il PrintNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(PrintNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node IfNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(IfNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.cond);
        this.visit(node.thenNode);
        this.visit(node.elseNode);
		return null;
	}

	/**
	 *
	 * @param node EqualNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(EqualNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node AndNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(AndNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node OrNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(OrNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node il NotNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(NotNode node) {
		if (this.print) this.printNode(node);
		this.visit(node.expression);
		return null;
	}

	/**
	 *
	 * @param node LessEqualNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(LessEqualNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node GreaterEqualNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(GreaterEqualNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node TimesNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(TimesNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node PlusNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(PlusNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node DivNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(DivNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 *
	 * @param node MinusNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(MinusNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	/**
	 * Lookup la funzione nella symbol table e setta il STentry e il nesting level
	 * Visitare gli argomenti.
	 *
	 * @param node CallNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(CallNode node) {
		if (this.print) this.printNode(node);
		STentry entry = this.stLookup(node.id);
		if (entry == null) {
			System.out.println("Fun id " + node.id + " at line "+ node.getLine() + " not declared");
            this.stErrors++;
		} else {
			node.entry = entry;
			node.nestingLevel = this.nestingLevel;
		}
		for (Node arg : node.argumentList) this.visit(arg);
		return null;
	}

	/**
	 * Cercare la variabile nella tabella dei simboli e impostare il livello di ingresso e di annidamento.
	 *
	 * @param node IdNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(IdNode node) {
		if (this.print) this.printNode(node);
		STentry entry = this.stLookup(node.id);
		if (entry == null) {
			System.out.println("Var or Par id " + node.id + " at line "+ node.getLine() + " not declared");
            this.stErrors++;
		} else {
			node.entry = entry;
			node.nestingLevel = this.nestingLevel;
		}
		return null;
	}

	/**
	 * Controlla se la superclass è stata dichiarata e imposta l'entry della superclass.
	 * Crea un oggetto ClassTypeNode e imposta i campi e i metodi della superclass, se presenti.
	 * Crea un'istanza di STentry e aggiungila alla symbol table globale.
	 * Crea la virtual table che eredita i metodi dalla superclass, se presenti.
	 * Aggiungi la virtual table alla tabella delle classi.
	 * Aggiungi la nuova symbol table per i metodi e i campi.
	 * Per ogni campo visitato, crea l'istanza di STentry e arricchendo anche il ClassTypeNode.
	 * Per ogni metodo, arricchisce il ClassTypeNode e visita il metodo.
	 * Rimuove la symbol table per i metodi e i campi e ripristina il livello di annidamento
	 *
	 * @param node ClassNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(ClassNode node) {
		if (this.print) this.printNode(node);
		ClassTypeNode tempClassTypeNode = new ClassTypeNode();
		final boolean isSubClass = node.superId.isPresent();
		final String superId = isSubClass ? node.superId.get() : null;
		if (isSubClass) {
			// controlla se è dichiarata una super classe
			if (this.classTable.containsKey(superId)) {
				final STentry superSTEntry = this.symbolTable.get(0).get(superId);
				final ClassTypeNode superTypeNode = (ClassTypeNode) superSTEntry.type;
				tempClassTypeNode = new ClassTypeNode(superTypeNode);
				node.superEntry = superSTEntry;
			} else {
				System.out.println("Class " + superId + " at line " + node.getLine() + " not declared");
                this.stErrors++;
			}
		}
		final ClassTypeNode classTypeNode = tempClassTypeNode;
		node.setType(classTypeNode);
		// Aggiunge l'id della classe alla tabella dello scope globale controllando i duplicati
		final STentry entry = new STentry(0, classTypeNode, this.decOffset--);
		final Map<String, STentry> globalScopeTable = this.symbolTable.get(0);
		if (globalScopeTable.put(node.classId, entry) != null) {
			System.out.println("Class id " + node.classId + " at line " + node.getLine() + " already declared");
            this.stErrors++;
		}
		// Aggiunge la tabella virtuale alla tabella delle classi
		final Set<String> visitedClassNames = new HashSet<>();
		final VirtualTable virtualTable = new VirtualTable();
		if (isSubClass) {
			final VirtualTable superClassVirtualTable = this.classTable.get(superId);
			virtualTable.putAll(superClassVirtualTable);
		}
        this.classTable.put(node.classId, virtualTable);
        this.symbolTable.add(virtualTable);
		// Setta l'offset dei campi
        this.nestingLevel++;
		int fieldOffset = -1;
		if (isSubClass) {
			final ClassTypeNode superTypeNode = (ClassTypeNode) this.symbolTable.get(0).get(superId).type;
			fieldOffset = -superTypeNode.fieldList.size() - 1;
		}
		// gestisce le dichiarazioni dei campi
		for (final FieldNode field : node.fieldList) {
			if (visitedClassNames.contains(field.id)) {
				System.out.println(
						"Field with id " + field.id + " on line " + field.getLine() + " was already declared"
				);
                this.stErrors++;
			} else {
				visitedClassNames.add(field.id);
			}
            this.visit(field);
			STentry fieldEntry = new STentry(this.nestingLevel, field.getType(), fieldOffset--);
			final boolean isFieldOverridden = isSubClass && virtualTable.containsKey(field.id);
			if (isFieldOverridden) {
				final STentry overriddenFieldEntry = virtualTable.get(field.id);
				final boolean isOverridingAMethod = overriddenFieldEntry.type instanceof MethodTypeNode;
				if (isOverridingAMethod) {
					System.out.println("Cannot override method " + field.id + " with a field");
                    this.stErrors++;
				} else {
					fieldEntry = new STentry(this.nestingLevel, field.getType(), overriddenFieldEntry.offset);
					classTypeNode.fieldList.set(-fieldEntry.offset - 1, fieldEntry.type);
				}
			} else {
				classTypeNode.fieldList.add(-fieldEntry.offset - 1, fieldEntry.type);
			}
			// aggiunge il campo alla virtual table
			virtualTable.put(field.id, fieldEntry);
			field.offset = fieldEntry.offset;
		}
		// setta l'offset dei metodi
		int prevDecOffset = this.decOffset;
        this.decOffset = 0;
		if (isSubClass) {
			final ClassTypeNode superTypeNode = (ClassTypeNode) this.symbolTable.get(0).get(superId).type;
            this.decOffset = superTypeNode.methodList.size();
		}
		for (final MethodNode method : node.methodList) {
			if (visitedClassNames.contains(method.id)) {
				System.out.println(
						"Method with id " + method.id + " on line " + method.getLine() + " was already declared"
				);
                this.stErrors++;
			} else {
				visitedClassNames.add(method.id);
			}
            this.visit(method);
			final MethodTypeNode methodTypeNode = (MethodTypeNode) this.symbolTable.get(this.nestingLevel).get(method.id).type;
			classTypeNode.methodList.add(method.offset, methodTypeNode);
		}
		// Rimuove la classe dalla symbol table
        this.symbolTable.remove(this.nestingLevel--);
        this.decOffset = prevDecOffset;
		return null;
	}

	/**
	 *
	 * @param node ClassTypeNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final ClassTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 * Crea il MethodTypeNode e la STentry che lo aggiunge alla symbol table
	 * Se il metodo è sovrascritto a un altro metodo, verificare se la sovrascrittura è corretta.
	 * Creare la nuova SymbolTable per lo scope del metodo.
	 * Per ogni parametro, creare la STentry e aggiungerla alla symbol table
	 * Visitare le dichiarazioni e l'espressione.
	 * Infine, rimuovere lo scope del metodo dalla symbol table
	 * @param node MethodNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final MethodNode node) {
		if (this.print) this.printNode(node);
		final Map<String, STentry> currentTable = this.symbolTable.get(this.nestingLevel);
		List<TypeNode> params = new ArrayList<>();
		for (ParNode parNode : node.parameterList) {
			params.add(parNode.getType());
		}
		final boolean isOverriding = currentTable.containsKey(node.id);
		final TypeNode methodType = new MethodTypeNode(params, node.returnType);
		STentry entry = new STentry(this.nestingLevel, methodType, this.decOffset++);

		if (isOverriding) {
			final var overriddenMethodEntry = currentTable.get(node.id);
			final boolean isOverridingAMethod = overriddenMethodEntry != null && overriddenMethodEntry.type instanceof MethodTypeNode;
			if (isOverridingAMethod) {
				entry = new STentry(this.nestingLevel, methodType, overriddenMethodEntry.offset);
                this.decOffset--;
			} else {
				System.out.println("Cannot override a class attribute with a method: " + node.id);
                this.stErrors++;
			}
		}

		node.offset = entry.offset;
		currentTable.put(node.id, entry);

		// si crea una nuova tabella per i metodi
        this.nestingLevel++;
		final Map<String, STentry> methodTable = new HashMap<>();
        this.symbolTable.add(methodTable);

		// setta l'offset delle dichiarazioni
		int prevDecOffset = this.decOffset;
        this.decOffset = -2;
		int parameterOffset = 1;

		for (final ParNode parameter : node.parameterList) {
			final STentry parameterEntry = new STentry(this.nestingLevel, parameter.getType(), parameterOffset++);
			if (methodTable.put(parameter.id, parameterEntry) != null) {
				System.out.println("Par id " + parameter.id + " at line " + node.getLine() + " already declared");
                this.stErrors++;
			}
		}
		for (Node declaration : node.declarationList) this.visit(declaration);
        this.visit(node.expression);

		// Remove the current nesting level symbolTable.
		// Rimuove il corrente nesting level della symbol table
        this.symbolTable.remove(this.nestingLevel--);
        this.decOffset = prevDecOffset;
		return null;
	}

	/**
	 *
	 * @param node MethodTypeNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final MethodTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 * Controlla se l'id dell'oggetto è stato dichiarato, facendo lookup nella symbol table.
	 * Se l'id dell'oggetto non è stato dichiarato, stampa errore.
	 * Se l'id dell'oggetto è stato dichiarato, verifica se il tipo è un RefTypeNode.
	 * Se il tipo non è un RefTypeNode, stampa errore.
	 * Se il tipo è un RefTypeNode, verifica se l'id del metodo è nella virtual table.
	 * Se l'id del metodo non è nella tabella virtuale, stampa errore.
	 * Se l'id del metodo è nella virtual table, setta STentry e il nesting level del nodo.
	 * Finalmente, visita gli argomenti.
	 *
	 * @param node ClassCallNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final ClassCallNode node) {
		if (this.print) this.printNode(node);
		final STentry entry = this.stLookup(node.objectId);

		if (entry == null) {
			System.out.println("Object id " + node.objectId + " was not declared");
            this.stErrors++;
		} else if (entry.type instanceof final RefTypeNode refTypeNode) {
			node.symbolTableEntry = entry;
			node.nestingLevel = this.nestingLevel;
			final VirtualTable virtualTable = this.classTable.get(refTypeNode.typeId);
			if (virtualTable.containsKey(node.methodId)) {
				node.methodEntry = virtualTable.get(node.methodId);
			} else {
				System.out.println("Object id " + node.objectId + " at line " + node.getLine() + " has no method " + node.methodId);
                this.stErrors++;
			}
		} else {
			System.out.println("Object id " + node.objectId + " at line " + node.getLine() + " is not a RefType");
            this.stErrors++;
		}
		for(Node argument : node.argumentList) this.visit(argument);
		return null;
	}

	/**
	 *
	 * @param node FieldNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final FieldNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 * Verifica se l'id della classe è stato dichiarato, facendo lookup nella virtual table delle classi
	 * restituisce errore se l'ID non è dichiarato
	 *
	 * @param node NewNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final NewNode node) {
		if (this.print) this.printNode(node);
		if (!this.classTable.containsKey(node.classId)) {
			System.out.println("Class id " + node.classId + " was not declared");
            this.stErrors++;
		}
		node.classSymbolTableEntry = this.symbolTable.get(0).get(node.classId);
		for (Node argument : node.argumentList) this.visit(argument);
		return null;
	}

	/**
	 * erifica se il tipo è stato dichiarato, facendo lookup nella virtual table delle classi.
	 * se non è stato dichiarato stampa un errore
	 *
	 * @param node RefTypeNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final RefTypeNode node) {
		if (this.print) this.printNode(node);
		if (!this.classTable.containsKey(node.typeId)) {
			System.out.println("Class with id: " + node.typeId + " on line: " + node.getLine() + " was not declared");
            this.stErrors++;
		}
		return null;
	}

	/**
	 *
	 * @param node BoolNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(BoolNode node) {
		if (this.print) this.printNode(node, node.value.toString());
		return null;
	}

	/**
	 *
	 * @param node IntNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(IntNode node) {
		if (this.print) this.printNode(node, node.value.toString());
		return null;
	}

	/**
	 *
	 * @param node EmptyNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(final EmptyNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param node EmptyTypeNode da visitare
	 * @return null
	 */
	@Override
	public Void visitNode(EmptyTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}
}