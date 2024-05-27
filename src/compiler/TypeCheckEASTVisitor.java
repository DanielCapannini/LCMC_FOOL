package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import java.util.List;
import java.util.Objects;

import static compiler.TypeRels.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	public TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions

	//checks that a type object is visitable (not incomplete) 
	private TypeNode ckvisit(TypeNode t) throws TypeException {
        this.visit(t);
		return t;
	}

	// Method to handle visit of declaration list with exception management
	private void visitNodeList(List<DecNode> nodeList) {
		for (Node dec : nodeList) {
			try {
				this.visit(dec);
			} catch (IncomplException e) {
				throw new RuntimeException(e);
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		}
	}

	/**
	 *
	 * @param node ProgLetInNode
	 * @return nodo analizzato
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(ProgLetInNode node) throws TypeException {
		if (this.print) this.printNode(node);
        this.visitNodeList(node.declarationlist);
		return this.visit(node.exp);
	}

	/**
	 *
	 * @param node ProgNode
	 * @return nodo analizzato
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(ProgNode node) throws TypeException {
		if (this.print) this.printNode(node);
		return this.visit(node.expression);
	}

	/**
	 *
	 * @param node FunNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(FunNode node) throws TypeException {
		if (this.print) this.printNode(node,node.id);
        this.visitNodeList(node.declarationlist);
		if ( !isSubtype(this.visit(node.expression), this.ckvisit(node.returnType)) )
			throw new TypeException("Wrong return type for function " + node.id,node.getLine());
		return null;
	}

	/**
	 *
	 * @param node VarNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(VarNode node) throws TypeException {
		if (this.print) this.printNode(node,node.id);
		if ( !isSubtype(this.visit(node.expression), this.ckvisit(node.getType())) )
			throw new TypeException("Incompatible value for variable " + node.id,node.getLine());
		return null;
	}

	/**
	 *
	 * @param node PrintNode
	 * @return nodo analizzato
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(PrintNode node) throws TypeException {
		if (this.print) this.printNode(node);
		return this.visit(node.expression);
	}

	/**
	 *
	 * @param node IfNode
	 * @return una visita al nodo dell'espressione
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(IfNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if ( !(isSubtype(this.visit(node.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if",node.getLine());
		TypeNode thenNode = this.visit(node.thenNode);
		TypeNode elseNode = this.visit(node.elseNode);
		if (isSubtype(thenNode, elseNode)) return elseNode;
		if (isSubtype(elseNode, thenNode)) return thenNode;
		final TypeNode returnType = lowestCommonAncestor(thenNode, elseNode);
		if (returnType == null)
			throw new TypeException("Incompatible types in then-else branches", node.getLine());
		return returnType;
	}

	/**
	 *
	 * @param node EqualNode
	 * @return new BoolTypeNode
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(EqualNode node) throws TypeException {
		if (this.print) this.printNode(node);
		TypeNode left = this.visit(node.left);
		TypeNode right = this.visit(node.right);
		if ( !(isSubtype(left, right) || isSubtype(right, left)) )
			throw new TypeException("Incompatible types in equal",node.getLine());
		return new BoolTypeNode();
	}

	/**
	 *
	 * @param node TimesNode
	 * @return new IntTypeNode
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(TimesNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if ( !(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",node.getLine());
		return new IntTypeNode();
	}

	/**
	 *
	 * @param node PlusNode
	 * @return new IntTypeNode
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(PlusNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if ( !(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",node.getLine());
		return new IntTypeNode();
	}

	/**
	 *
	 * @param node CallNode
	 * @return ArrowType
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(CallNode node) throws TypeException {
		if (this.print) this.printNode(node,node.id);
		TypeNode t = this.visit(node.entry);
		if ( !(t instanceof ArrowTypeNode arrowTypeNode) )
			throw new TypeException("Invocation of a non-function "+node.id,node.getLine());
        if ( !(arrowTypeNode.parameterList.size() == node.argumentList.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of "+node.id,node.getLine());
		for (int i = 0; i < node.argumentList.size(); i++)
			if ( !(isSubtype(this.visit(node.argumentList.get(i)),arrowTypeNode.parameterList.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+node.id,node.getLine());
		return arrowTypeNode.returnType;
	}

	/**
	 *
	 * @param node IdNode
	 * @return TypeNode che identifica il tipo del nodo
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(IdNode node) throws TypeException {
		if (this.print) this.printNode(node,node.id);
		TypeNode t = this.visit(node.entry);
		if (t instanceof ArrowTypeNode)
			throw new TypeException("Wrong usage of function identifier " + node.id,node.getLine());
		return t;
	}

	/**
	 *
	 * @param node BoolNode
	 * @return new BoolTypeNode
	 */
	@Override
	public TypeNode visitNode(BoolNode node) {
		if (this.print) this.printNode(node,node.value.toString());
		return new BoolTypeNode();
	}

	/**
	 *
	 * @param node IntNode
	 * @return new IntTypeNode
	 */
	@Override
	public TypeNode visitNode(IntNode node) {
		if (this.print) this.printNode(node,node.value.toString());
		return new IntTypeNode();
	}

	/**
	 *
	 * @param node ArrowTypeNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(ArrowTypeNode node) throws TypeException {
		if (this.print) this.printNode(node);
		for (Node parameter: node.parameterList) this.visit(parameter);
        this.visit(node.returnType,"->"); //marks return type
		return null;
	}

	/**
	 *
	 * @param node BoolTypeNode
	 * @return null
	 */
	@Override
	public TypeNode visitNode(BoolTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param node IntTypeNode
	 * @return null
	 */
	@Override
	public TypeNode visitNode(IntTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 * Prende l'input del codice associabile alla Symbol Table e lo manda al metodo checkVisit
	 * per controllare che sia visitabile
	 *
	 * @param entry STentry
	 * @return il risultato della visita
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (this.print) this.printSTentry("type");
		return this.ckvisit(entry.type);
	}

	/**
	 *
	 * @param node MinusNode
	 * @return new IntTypeNode
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
	 *
	 * @param node DivNode
	 * @return new IntTypeNode
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
	 *
	 * @param node GreaterEqualNode
	 * @return new BoolTypeNode
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
	 *
	 * @param node LessEqualNode
	 * @return new BoolTypeNode
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
	 *
	 * @param node OrNode
	 * @return new BoolTypeNode
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
	 *
	 * @param node AndNode
	 * @return new BoolTypeNode
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
	 *
	 * @param node NotNode
	 * @return new BoolTypeNode
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(NotNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.expression), new BoolTypeNode())))
			throw new TypeException("Non boolean in not", node.getLine());
		return new BoolTypeNode();
	}

	/**
	 *
	 * @param node ClassNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(ClassNode node) throws TypeException {
		if (this.print) this.printNode(node, node.classId);
		final boolean isSubClass = node.superClassId.isPresent();
		final String parent = isSubClass ? node.superClassId.get() : null;
		if (!isSubClass) {
			for(Node method : node.methodList) {
				try {
					this.visit(method);
				} catch (TypeException e) {
					System.out.println("Type checking error in a class declaration: " + e.text);
				}
			}
			return null;
		}
		superType.put(node.classId, parent); // eredito, quindi aggiungo la mia classe in superType
		final ClassTypeNode classType = (ClassTypeNode) node.getType();
		final ClassTypeNode superClassType = (ClassTypeNode) node.superClassEntry.type;
		for (final FieldNode field : node.fieldList) {
			int position = -field.offset - 1;
			final boolean isOverriding = position < superClassType.fieldList.size();
			if (isOverriding && !isSubtype(classType.fieldList.get(position), superClassType.fieldList.get(position))) {
				throw new TypeException("Wrong type for field " + field.id, field.getLine());
			}
		}
		for (final MethodNode method : node.methodList) {
			int position = method.offset;
			final boolean isOverriding = position < superClassType.fieldList.size();
			if (isOverriding) {
				ArrowTypeNode methodTypeNode = classType.methodList.get(position).functionalType;
				ArrowTypeNode superMethodNode = superClassType.methodList.get(position).functionalType;
				if (methodTypeNode.parameterList.size() != superMethodNode.parameterList.size()) {
					throw new TypeException("WRONG quantity parameters " + method.id, method.getLine());
               }
			}
			if (isOverriding && !isSubtype(classType.methodList.get(position), superClassType.methodList.get(position))) {
				throw new TypeException("Wrong type for method " + method.id, method.getLine());
			}
		}
		return null;
	}

	/**
	 *
	 * @param node MethodNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(final MethodNode node) throws TypeException {
		if (this.print) this.printNode(node, node.id);
        this.visitNodeList(node.declarationList);
		if (!isSubtype(this.visit(node.expression), this.ckvisit(node.returnType))) { // visita l'espressione e controlla se è un sottotipo del tipo restituito
			throw new TypeException("Wrong return type for method " + node.id, node.getLine());
		}
		return null;
	}

	/**
	 *
	 * @param node EmptyNode
	 * @return new EmptyTypeNode
	 */
	@Override
	public TypeNode visitNode(final EmptyNode node) {
		if (this.print) this.printNode(node);
		return new EmptyTypeNode();
	}

	/**
	 *
	 * @param node ClassCallNode
	 * @return arrowTypeNode.returnType
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(final ClassCallNode node) throws TypeException {
		if (this.print) this.printNode(node, node.objectId);
		if(Objects.isNull(node.methodEntry)) return null; // per evitare NullPointerException
		TypeNode methodType = this.visit(node.methodEntry);
		if (!(methodType instanceof MethodTypeNode)) {
			throw new TypeException("Invocation of a non-method " + node.methodId, node.getLine());
		}
		ArrowTypeNode arrowTypeNode = ((MethodTypeNode) methodType).functionalType;
		if (node.argumentList.size() != arrowTypeNode.parameterList.size()) {
			throw new TypeException("Wrong number of parameters in the invocation of " + node.methodId, node.getLine());
		}
		for (var i = 0; i < node.argumentList.size(); i++) {
			if (!isSubtype(this.visit(node.argumentList.get(i)), arrowTypeNode.parameterList.get(i))) {
				throw new TypeException(
						"Wrong type for " + (i+1) + "-th parameter in the invocation of " + node.methodId, node.getLine()
				);
			}
		}
		return arrowTypeNode.returnType;
	}

	/**
	 *
	 * @param node NewNode
	 * @return nodo con il riferimento alla classe
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(final NewNode node) throws TypeException {
		if (this.print) this.printNode(node, node.classId);
		final TypeNode typeNode = this.visit(node.classSymbolTableEntry);
		if (!(typeNode instanceof ClassTypeNode classTypeNode)) {
			throw new TypeException("Invocation of a non-constructor " + node.classId, node.getLine());
		}
		if (classTypeNode.fieldList.size() != node.argumentList.size()) {
			throw new TypeException("Wrong number of parameters in the invocation of constructor " + node.classId, node.getLine());
		}
		for (int i = 0; i < node.argumentList.size(); i++) {
			if (!(isSubtype(this.visit(node.argumentList.get(i)), classTypeNode.fieldList.get(i)))) {
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of constructor " + node.classId, node.getLine());
			}
		}
		return new RefTypeNode(node.classId);
	}

	/**
	 *
	 * @param node ClassTypeNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(final ClassTypeNode node) throws TypeException {
		if (this.print) this.printNode(node);
		for (final TypeNode field : node.fieldList) this.visit(field);
		for (final MethodTypeNode method : node.methodList) this.visit(method);
		return null;
	}

	/**
	 *
	 * @param node MethodTypeNode
	 * @return null
	 * @throws TypeException l'espressione non è corretta
	 */
	@Override
	public TypeNode visitNode(final MethodTypeNode node) throws TypeException {
		if (this.print) this.printNode(node);
		for (final TypeNode parameter : node.functionalType.parameterList) this.visit(parameter);
		this.visit(node.functionalType.returnType, "->");
		return null;
	}

	/**
	 *
	 * @param node RefTypeNode
	 * @return null
	 */
	@Override
	public TypeNode visitNode(final RefTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	/**
	 *
	 * @param node EmptyTypeNode
	 * @return null
	 */
	@Override
	public TypeNode visitNode(final EmptyTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

}