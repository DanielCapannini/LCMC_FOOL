package compiler;

import java.util.*;
import java.util.stream.Collectors;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

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

	private List<Map<String, STentry>> symbolTable = new ArrayList<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	public int stErrors=0;

	public SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = this.nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = this.symbolTable.get(j--).get(id);
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (this.print) this.printNode(n);
        this.symbolTable.add(new HashMap<>());
		n.declarationlist.forEach(this::visit);
        this.visit(n.exp);
        this.symbolTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (this.print) this.printNode(n);
		Map<String, STentry> currentSymbolTable = this.symbolTable.get(this.nestingLevel);
		List<TypeNode> parameterTypeList = new ArrayList<>();
		for (ParNode par : n.parameterlist) parameterTypeList.add(par.getType());
		STentry entry = new STentry(this.nestingLevel, new ArrowTypeNode(parameterTypeList,n.retType), this.decOffset--);
		//inserimento di ID nella symtable
		if (currentSymbolTable.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
            this.stErrors++;
		} 
		//creare una nuova hashmap per la symTable
        this.nestingLevel++;
		Map<String, STentry> newSymbolTable = new HashMap<>();
        this.symbolTable.add(newSymbolTable);
		int prevNLDecOffset= this.decOffset; // stores counter for offset of declarations at previous nesting level
        this.decOffset =-2;


		int parOffset=1;
		for (ParNode par : n.parameterlist)
			if (newSymbolTable.put(par.id, new STentry(this.nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
                this.stErrors++;
			}
		for (Node dec : n.declarationlist) this.visit(dec);
        this.visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
        this.symbolTable.remove(this.nestingLevel--);
        this.decOffset =prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(NotNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.exp);
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.exp);
		Map<String, STentry> currentSymbolTable = this.symbolTable.get(this.nestingLevel);
		STentry entry = new STentry(this.nestingLevel,n.getType(), this.decOffset--);
		//inserimento di ID nella symtable
		if (currentSymbolTable.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
            this.stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.cond);
        this.visit(n.thenNode);
        this.visit(n.elseNode);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (this.print) this.printNode(n);
        this.visit(n.left);
        this.visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode node) {
		if (this.print) this.printNode(node);
        this.visit(node.left);
        this.visit(node.right);
		return null;
	}


	@Override
	public Void visitNode(CallNode n) {
		if (this.print) this.printNode(n);
		STentry entry = this.stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
            this.stErrors++;
		} else {
			n.entry = entry;
			n.nestingLevel = this.nestingLevel;
		}
		for (Node arg : n.argumentList) this.visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (this.print) this.printNode(n);
		STentry entry = this.stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
            this.stErrors++;
		} else {
			n.entry = entry;
			n.nestingLevel = this.nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (this.print) this.printNode(n, n.value.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (this.print) this.printNode(n, n.value.toString());
		return null;
	}

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

	@Override
	public Void visitNode(final FieldNode node) {
		if (this.print) this.printNode(node);
		return null;
	}


	@Override
	public Void visitNode(final MethodNode node) {
		if (this.print) this.printNode(node);
		final Map<String, STentry> currentTable = this.symbolTable.get(this.nestingLevel);
		final List<TypeNode> params = node.parameterList.stream().map(ParNode::getType).collect(Collectors.toList());
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
		node.declarationList.forEach(this::visit);
        this.visit(node.exp);

		// Remove the current nesting level symbolTable.
		// Rimuove il corrente nesting level della symbol table
        this.symbolTable.remove(this.nestingLevel--);
        this.decOffset = prevDecOffset;
		return null;
	}



	@Override
	public Void visitNode(final EmptyNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	@Override
	public Void visitNode(final ClassCallNode node) {
		if (this.print) this.printNode(node);
		final STentry entry = this.stLookup(node.objectId);

		if (entry == null) {
			System.out.println("Object id " + node.objectId + " was not declared");
            this.stErrors++;
		} else if (entry.type instanceof final RefTypeNode refTypeNode) {
			node.entry = entry;
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

		node.argumentList.forEach(this::visit);
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
		if (this.print) this.printNode(node);
		if (!this.classTable.containsKey(node.classId)) {
			System.out.println("Class id " + node.classId + " was not declared");
            this.stErrors++;
		}


		node.entry = this.symbolTable.get(0).get(node.classId);
		node.argumentList.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(final ClassTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	@Override
	public Void visitNode(final MethodTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	@Override
	public Void visitNode(final RefTypeNode node) {
		if (this.print) this.printNode(node);
		if (!this.classTable.containsKey(node.typeId)) {
			System.out.println("Class with id: " + node.typeId + " on line: " + node.getLine() + " was not declared");
            this.stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(EmptyTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}
}
