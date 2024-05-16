package compiler;

import java.util.*;
import compiler.lib.*;

public class AST {

	/**
	 * Nodo radice dell'AST (abstract syntax tree)
	 *
	 * @declarations lista delle dichiarazioni
	 * @exp exp espressione principale
	 */
	public static class ProgLetInNode extends Node {
		final List<DecNode> declarationlist;
		final Node exp;
		ProgLetInNode(List<DecNode> d, Node e) {
            this.declarationlist = Collections.unmodifiableList(d);
            this.exp = e;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo radice dell'AST (abstract syntax tree)
	 *
	 * @exp espressione principale
	 */
	public static class ProgNode extends Node {
		final Node exp;
		ProgNode(Node e) {
            this.exp = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la dichiarazione di una funzione
	 *
	 * @id nome della funzione
	 * @returnType tipo di ritorno
	 * @parameters lista dei parametri
	 * @declarations lista delle dichiarazioni
	 * @exp espressione principale
	 */
	public static class FunNode extends DecNode {
		final String id;
		final TypeNode retType;
		final List<ParNode> parameterlist;
		final List<DecNode> declarationlist;
		final Node exp;
		FunNode(String i, TypeNode rt, List<ParNode> pl, List<DecNode> dl, Node e) {
            this.id =i;
            this.retType =rt;
            this.parameterlist =Collections.unmodifiableList(pl);
            this.declarationlist =Collections.unmodifiableList(dl);
            this.exp =e;
	    }
		
		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la dichiarazione di un parametro (per le funzioni)
	 *
	 * @id nome del parametro
	 */
	public static class ParNode extends DecNode {
		final String id;
		ParNode(String i, TypeNode t) {
            this.id = i;
            this.type = t;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la dichiarazione di una variabile
	 *
	 * @id nome della variabile
	 * @exp espressione di inizializzazione
	 */
	public static class VarNode extends DecNode {
		final String id;
		final Node exp;
		VarNode(String i, TypeNode t, Node v) {
            this.id = i;
            this.type = t;
            this.exp = v;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la stampa di un'espressione
	 *
	 * @exp espressione da stampare
	 */
	public static class PrintNode extends Node {
		final Node exp;
		PrintNode(Node e) {
            this.exp = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per IF-THEN-ELSE
	 *
	 * @cond nodo condizione
	 * @thenNode nodo eseguito se la condizione è vera
	 * @elseNode nodo eseguito se la condizione è falsa
	 */
	public static class IfNode extends Node {
		final Node cond;
		final Node thenNode;
		final Node elseNode;
		IfNode(Node c, Node t, Node e) {
            this.cond = c;
            this.thenNode = t;
            this.elseNode = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per l'uguaglianza
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class EqualNode extends Node {
		final Node left;
		final Node right;
		EqualNode(Node l, Node r) {
            this.left = l;
            this.right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la moltiplicazione
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class TimesNode extends Node {
		final Node left;
		final Node right;
		TimesNode(Node l, Node r) {
            this.left = l;
            this.right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per la somma
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class PlusNode extends Node {
		final Node left;
		final Node right;
		PlusNode(Node l, Node r) {
            this.left = l;
            this.right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo di una chiama a funzione
	 *
	 * @id id della funzione
	 * @argumentList lista degli argomenti
	 * @entry entry della funzione nella symbol table
	 * @nl livello di annidamento
	 */
	public static class CallNode extends Node {
		final String id;
		final List<Node> argumentList;
		STentry entry;
		int nestingLevel;
		CallNode(String i, List<Node> p) {
            this.id = i;
            this.argumentList = Collections.unmodifiableList(p);
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il valore dell'ID, è variabile
	 *
	 * @id id della variabile
	 * @entry entry della variabile nella symbol table
	 * @nl livello di annidamento
	 */
	public static class IdNode extends Node {
		final String id;
		STentry entry;
		int nestingLevel;
		IdNode(String i) {
            this.id = i;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il valore booleano
	 *
	 * @value valore booleano
	 */
	public static class BoolNode extends Node {
		final Boolean value;
		BoolNode(boolean n) {
            this.value = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il valore intero
	 *
	 * @value valore intero
	 */
	public static class IntNode extends Node {
		final Integer value;
		IntNode(Integer n) {
            this.value = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Node per l'arrow type di una funzione
	 * @parameters lista dei parametri
	 * @returnType tipo di ritorno
	 */
	public static class ArrowTypeNode extends TypeNode {
		final List<TypeNode> parameterList;
		final TypeNode returnType;
		ArrowTypeNode(List<TypeNode> p, TypeNode r) {
            this.parameterList = Collections.unmodifiableList(p);
            this.returnType = r;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il tipo booleano
	 */
	public static class BoolTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il tipo intero
	 */
	public static class IntTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/**
	 * Nodo per il tipo di un metodo
	 * @functionalType tipo del metodo
	 */
	public static class MethodTypeNode extends TypeNode {
		final ArrowTypeNode functionalType;

		MethodTypeNode(List<TypeNode> typeParams, TypeNode typeReturn) {
			this.functionalType = new ArrowTypeNode(typeParams, typeReturn);
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il maggiore uguale
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class GreaterEqualNode extends Node {
		final Node left;
		final Node right;

		GreaterEqualNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il minore uguale
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class LessEqualNode extends Node {
		final Node left;
		final Node right;

		LessEqualNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per NOT logico
	 *
	 * @exp nodo da negare
	 */
	public static class NotNode extends Node {
		final Node exp;

		NotNode(Node e) {
			this.exp = e;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la sottostrazione
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class MinusNode extends Node {
		final Node left;
		final Node right;

		MinusNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per OR logico
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class OrNode extends Node {
		final Node left;
		final Node right;

		OrNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la divisione
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class DivNode extends Node {
		final Node left;
		final Node right;

		DivNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per AND logico
	 *
	 * @left nodo sinistro
	 * @right nodo destro
	 */
	public static class AndNode extends Node {
		final Node left;
		final Node right;

		AndNode(Node l, Node r) {
			this.left = l;
			this.right = r;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la definizione di una classe
	 *
	 * @classId id della classe
	 * @superId id della superclasse
	 * @fieldList lista dei campi
	 * @methodList lista dei metodi
	 * @superEntry entry della superclasse nella symbol table
	 */
	public static class ClassNode extends DecNode {
		final String classId;
		final List<FieldNode> fieldList;
		final List<MethodNode> methodList;

		final Optional<String> superId;
		STentry superEntry;

		ClassNode(String classId, final Optional<String> superId, List<FieldNode> fields, List<MethodNode> methods) {
			this.classId = classId;
			this.superId = superId;
			this.fieldList = Collections.unmodifiableList(fields);
			this.methodList = Collections.unmodifiableList(methods);
		}

		public void setType(ClassTypeNode type) {
			this.type = type;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la definizione di un campo
	 *
	 * @id id del campo
	 * @offset offset del campo rispetto alla classe
	 */
	public static class FieldNode extends DecNode {
		final String id;
		int offset;

		FieldNode(String i, TypeNode t) {
			this.id = i;
			this.type = t;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la definizione di un metodo
	 *
	 * @id id del metodo
	 * @returnType tipo di ritorno
	 * @parameterList lista dei parametri
	 * @declarationList lista delle dichiarazioni nel corpo del metodo
	 * @exp espressione principale
	 * @offset offset del metodo rispetto alla classe
	 * @label etichetta del metodo usata per la generazione del codice
	 */
	public static class MethodNode extends DecNode {
		final String id;
		final TypeNode returnType;
		final List<ParNode> parameterList;
		final List<DecNode> declarationList;
		final Node exp;
		int offset = 0;

		String label;

		MethodNode(String id, TypeNode rt, List<ParNode> pl, List<DecNode> dl, Node e) {
			this.id = id;
			this.returnType = rt;
			this.parameterList = pl;
			this.declarationList = dl;
			this.exp = e;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la chiamata di un metodo
	 *
	 * @objectId id dell'oggetto
	 * @methodId id del metodo
	 * @argumentList lista degli argomenti
	 * @entry entry dell'oggetto nella symbol table
	 * @methodEntry entry del metodo nella symbol table
	 * @nestingLevel livello di annidamento
	 */
	public static class ClassCallNode extends Node {
		final String objectId;
		final List<Node> argumentList;
		STentry entry;
		final String methodId;
		STentry methodEntry;
		int nestingLevel;

		ClassCallNode(final String objId, final String methodId, final List<Node> args) {
			this.objectId = objId;
			this.methodId = methodId;
			this.argumentList = Collections.unmodifiableList(args);
			this.nestingLevel = 0;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per la creazione di un oggetto
	 *
	 * @classId id della classe
	 * @argumentList lista degli argomenti
	 */
	public static class NewNode extends Node {
		final String classId;
		final List<Node> argumentList;
		STentry entry;

		NewNode(String id, final List<Node> args) {
			this.classId = id;
			this.argumentList = Collections.unmodifiableList(args);
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il valore null
	 */
	public static class EmptyNode extends Node {
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il tipo di una classe
	 * @fieldList lista dei tipi dei campi
	 * @methodList lista dei tipi dei metodi
	 */
	public static class ClassTypeNode extends TypeNode {
		final List<TypeNode> fieldList;
		final List<MethodTypeNode> methodList;

		ClassTypeNode(final List<TypeNode> f, final List<MethodTypeNode> m) {
			this.fieldList = new ArrayList<>(f);
			this.methodList = new ArrayList<>(m);
		}

		ClassTypeNode(final ClassTypeNode parent) {
			this(parent.fieldList, parent.methodList);
		}

		ClassTypeNode() {
			this(new ArrayList<>(), new ArrayList<>());
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il tipo di un riferimento di un oggetto(classe)
	 * @typeId id della typo della classe
	 */
	public static class RefTypeNode extends TypeNode {
		final String typeId;

		RefTypeNode(String id) {
			this.typeId = id;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	/**
	 * Nodo per il tipo null
	 */
	public static class EmptyTypeNode extends TypeNode {
		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

}