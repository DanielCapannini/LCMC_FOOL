package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class TypeRels {
	public static Map<String, String> superType = new HashMap<>();

	/**
	 *
	 * @param type tipo
	 * @return stream dei sopratipi del tipo di input
	 */
	private static Stream<String> superTypes(final String type) {
		return Stream.iterate(type, Objects::nonNull, superType::get);
	}

	/**
	 *
	 * @param first nodo contenente il metodo
	 * @param second nodo con cui paragonare
	 * @return true se il metodo è ereditato dal secondo
	 */
	private static boolean isMethodOverride(final TypeNode first, final TypeNode second) {
		if (!(first instanceof ArrowTypeNode firstArrowTypeNode) ||
				!(second instanceof ArrowTypeNode secondArrowTypeNode)) {
			return false;
		}
		if (!isSubtype(firstArrowTypeNode.returnType, secondArrowTypeNode.returnType)) {
			return false;
		}
		for (TypeNode parameterType : firstArrowTypeNode.parameterList) {
			if (!isSubtype(parameterType, secondArrowTypeNode.returnType)) {
				return false;
			}
		}
		return true;
    }

	/**
	 *
	 * @param first TypeNode su cui controllare
	 * @param second TypeNode di confronto
	 * @return true se in primo TypeNode è dello stesso tipi o sottotipo
	 */
	public static boolean isSubtype(TypeNode first, TypeNode second) {
		//controllo sui tipi Int e Boolean
		if(((first instanceof BoolTypeNode) && (second instanceof IntTypeNode | second instanceof BoolTypeNode))
				|| ((first instanceof IntTypeNode) && (second instanceof IntTypeNode))) return true;
		//controllo che il primo sia di tipo Empty e il secondo riferimento
		if(((first instanceof EmptyTypeNode) && (second instanceof RefTypeNode))) return true;
		//controllo che il metodo di una classe sia ereditato da un metodo della classe padre
		if(isMethodOverride(first, second)) return true;
		//controllo che il primo sia sottoclasse del secondo
		if (!(first instanceof RefTypeNode firstRefTypeNode)
				|| !(second instanceof RefTypeNode secondRefTypeNode)) {
			return false;
		}
        return superTypes(firstRefTypeNode.typeId).anyMatch(secondRefTypeNode.typeId::equals);
    }

	/**
	 * ottimizzazione 3
	 *
	 * @param first TypeNode
	 * @param second TypeNode
	 * @return supertipo in comune se presente o null
	 */
	public static TypeNode lowestCommonAncestor(final TypeNode first, final TypeNode second) {
		if (isSubtype(first, second)) return second;
		if (isSubtype(second, first)) return first;
		if (!(first instanceof RefTypeNode firstRefTypeNode)) return null;
		return superTypes(firstRefTypeNode.typeId)
				.map(RefTypeNode::new)
				.filter(typeOfSuperA -> isSubtype(second, typeOfSuperA))
				.findFirst()
				.orElse(null);
	}
}
