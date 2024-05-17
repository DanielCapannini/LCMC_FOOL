package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

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
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	//checks that a type object is visitable (not incomplete) 
	private TypeNode ckvisit(TypeNode t) throws TypeException {
        this.visit(t);
		return t;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode node) throws TypeException {
		if (this.print) this.printNode(node);
		for (Node dec : node.declarationlist)
			try {
                this.visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return this.visit(node.exp);
	}

	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (this.print) this.printNode(n);
		return this.visit(n.exp);
	}

	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (this.print) this.printNode(n,n.id);
		for (Node dec : n.declarationlist)
			try {
                this.visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if ( !isSubtype(this.visit(n.exp), this.ckvisit(n.retType)) )
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (this.print) this.printNode(n,n.id);
		if ( !isSubtype(this.visit(n.exp), this.ckvisit(n.getType())) )
			throw new TypeException("Incompatible value for variable " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (this.print) this.printNode(n);
		return this.visit(n.exp);
	}

	@Override
	public TypeNode visitNode(IfNode n) throws TypeException {
		if (this.print) this.printNode(n);
		if ( !(isSubtype(this.visit(n.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if",n.getLine());
		TypeNode thenNode = this.visit(n.thenNode);
		TypeNode elseNode = this.visit(n.elseNode);
		if (isSubtype(thenNode, elseNode)) return elseNode;
		if (isSubtype(elseNode, thenNode)) return thenNode;
		throw new TypeException("Incompatible types in then-else branches",n.getLine());
	}

	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (this.print) this.printNode(n);
		TypeNode left = this.visit(n.left);
		TypeNode right = this.visit(n.right);
		if ( !(isSubtype(left, right) || isSubtype(right, left)) )
			throw new TypeException("Incompatible types in equal",n.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (this.print) this.printNode(n);
		if ( !(isSubtype(this.visit(n.left), new IntTypeNode())
				&& isSubtype(this.visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (this.print) this.printNode(n);
		if ( !(isSubtype(this.visit(n.left), new IntTypeNode())
				&& isSubtype(this.visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (this.print) this.printNode(n,n.id);
		TypeNode t = this.visit(n.entry);
		if ( !(t instanceof ArrowTypeNode) )
			throw new TypeException("Invocation of a non-function "+n.id,n.getLine());
		ArrowTypeNode arrowTypeNode = (ArrowTypeNode) t;
		if ( !(arrowTypeNode.parameterList.size() == n.argumentList.size()) )
			throw new TypeException("Wrong number of parameters in the invocation of "+n.id,n.getLine());
		for (int i = 0; i < n.argumentList.size(); i++)
			if ( !(isSubtype(this.visit(n.argumentList.get(i)),arrowTypeNode.parameterList.get(i))) )
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.id,n.getLine());
		return arrowTypeNode.returnType;
	}

	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (this.print) this.printNode(n,n.id);
		TypeNode t = this.visit(n.entry);
		if (t instanceof ArrowTypeNode)
			throw new TypeException("Wrong usage of function identifier " + n.id,n.getLine());
		return t;
	}

	@Override
	public TypeNode visitNode(BoolNode n) {
		if (this.print) this.printNode(n,n.value.toString());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(IntNode n) {
		if (this.print) this.printNode(n,n.value.toString());
		return new IntTypeNode();
	}

// gestione tipi incompleti	(se lo sono lancia eccezione)
	
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (this.print) this.printNode(n);
		for (Node par: n.parameterList) this.visit(par);
        this.visit(n.returnType,"->"); //marks return type
		return null;
	}

	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (this.print) this.printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (this.print) this.printNode(n);
		return null;
	}

// STentry (ritorna campo type)

	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (this.print) this.printSTentry("type");
		return this.ckvisit(entry.type);
	}

	@Override
	public TypeNode visitNode(MinusNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())))
			throw new TypeException("Non integers in sum", node.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(DivNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())))
			throw new TypeException("Non integers in div", node.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(GreaterEqualNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())))
			throw new TypeException("Incompatible types in greaterEqual", node.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(LessEqualNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new IntTypeNode())
				&& isSubtype(this.visit(node.right), new IntTypeNode())))
			throw new TypeException("Incompatible types in lessEqual", node.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(OrNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new BoolTypeNode())
				&& isSubtype(this.visit(node.right), new BoolTypeNode())))
			throw new TypeException("Non booleans in or", node.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(AndNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.left), new BoolTypeNode())
				&& isSubtype(this.visit(node.right), new BoolTypeNode())))
			throw new TypeException("Non booleans in and", node.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(NotNode node) throws TypeException {
		if (this.print) this.printNode(node);
		if (!(isSubtype(this.visit(node.exp), new BoolTypeNode())))
			throw new TypeException("Non boolean in not", node.getLine());
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(ClassNode node) throws TypeException {
		if (this.print) this.printNode(node, node.classId);
		final boolean isSubClass = node.superId.isPresent();
		final String parent = isSubClass ? node.superId.get() : null;

		if (!isSubClass) {
			node.methodList.forEach(method -> {
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
		final ClassTypeNode superClassType = (ClassTypeNode) node.superEntry.type;

		//CAMPI: controllo che gli overriding siano corretti.
		//Per ogni campo calcolo la posizione che, in fields di superClassType,
		//corrisponde al suo offset. Se la pos è < allora ovveriding e faccio il check sottotipo
		for (final FieldNode field : node.fieldList) {
			int position = -field.offset - 1;
			final boolean isOverriding = position < superClassType.fieldList.size();
			if (isOverriding && !isSubtype(classType.fieldList.get(position), superClassType.fieldList.get(position))) {
				throw new TypeException("Wrong type for field " + field.id, field.getLine());
			}
		}

		//METODI: controllo che gli overriding siano corretti
		//Per ogni metodo calcolo la posizione che, in methods di superClassType,
		//corrisponde al suo offset. Se la pos è < allora ovveriding e faccio il check sottotipo
		for (final MethodNode method : node.methodList) {
			int position = method.offset;
			final boolean isOverriding = position < superClassType.fieldList.size();
			if (isOverriding && !isSubtype(classType.methodList.get(position), superClassType.methodList.get(position))) {
				throw new TypeException("Wrong type for method " + method.id, method.getLine());
			}
		}

		return null;
	}

	@Override
	public TypeNode visitNode(final MethodNode node) throws TypeException {
		if (this.print) this.printNode(node, node.id);

		for (final DecNode declaration : node.declarationList) {
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

	@Override
	public TypeNode visitNode(final EmptyNode node) {
		if (this.print) this.printNode(node);
		return new EmptyTypeNode();
	}

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
		if (arrowTypeNode.parameterList.size() != node.argumentList.size()) {
			throw new TypeException("Wrong number of parameters in the invocation of method " + node.methodId, node.getLine());
		}

		// check if the types of the parameters are correct
		for (int i = 0; i < node.argumentList.size(); i++) {
			if (!(isSubtype(this.visit(node.argumentList.get(i)), arrowTypeNode.parameterList.get(i)))) {
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of method " + node.methodId, node.getLine());
			}
		}

		return arrowTypeNode.returnType;
	}

	@Override
	public TypeNode visitNode(final NewNode node) throws TypeException {
		if (this.print) this.printNode(node, node.classId);
		final TypeNode typeNode = this.visit(node.entry);

		if (!(typeNode instanceof ClassTypeNode classTypeNode)) {
			throw new TypeException("Invocation of a non-constructor " + node.classId, node.getLine());
		}

		if (classTypeNode.fieldList.size() != node.argumentList.size()) {
			throw new TypeException("Wrong number of parameters in the invocation of constructor " + node.classId, node.getLine());
		}
		// check if the types of the parameters are correct
		for (int i = 0; i < node.argumentList.size(); i++) {
			if (!(isSubtype(this.visit(node.argumentList.get(i)), classTypeNode.fieldList.get(i)))) {
				throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of constructor " + node.classId, node.getLine());
			}
		}
		return new RefTypeNode(node.classId);
	}

	@Override
	public TypeNode visitNode(final ClassTypeNode node) throws TypeException {
		if (this.print) this.printNode(node);
		// Visit all fields and methods
		for (final TypeNode field : node.fieldList) this.visit(field);
		for (final MethodTypeNode method : node.methodList) this.visit(method);
		return null;
	}

	@Override
	public TypeNode visitNode(final MethodTypeNode node) throws TypeException {
		if (this.print) this.printNode(node);
		// Visit all parameters and the return type
		for (final TypeNode parameter : node.functionalType.parameterList) this.visit(parameter);
		this.visit(node.functionalType.returnType, "->");
		return null;
	}

	@Override
	public TypeNode visitNode(final RefTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

	@Override
	public TypeNode visitNode(final EmptyTypeNode node) {
		if (this.print) this.printNode(node);
		return null;
	}

}