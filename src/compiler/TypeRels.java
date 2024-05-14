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
	 * Dato il tipo di un nodo, controlla da quale tipo eredita
	 *
	 * @param type tipo del dato scelto
	 * @return uno stream dei super tipi del dato
	 */
	private static Stream<String> superTypes(final String type) {
		return Stream.iterate(type, Objects::nonNull, superType::get);
	}

	/**
	 * Utilizzato dal metodo subtype, controlla che il primo nodo sia null e il secondo sia un riferimento
	 *
	 * @param first nodo da controllare se null
	 * @param second nodo da controllare se riferimento
	 * @return true se le condizione sono avverate, false altrimenti
	 */
	private static boolean isEmptyTypeAndRefType(final TypeNode first, final TypeNode second) {
		return ((first instanceof EmptyTypeNode) && (second instanceof RefTypeNode));
	}

	/**
	 * Utilizzato dal metodo subtype, controlla che due nodi siano di tipo Int o Booleano, ma se il primo
	 * nodo dovesse essere un'intero, l'espressione restituisce true solo se anche il secondo è un'intero
	 *
	 * @param first primo nodo da controllare se Int o Boolean
	 * @param second secondo nodo da controllare se Int o Boolean
	 * @return true se le condizioni sono rispettate int/boolean, false altrimenti
	 */
	private static boolean isBoolAndInt(final TypeNode first, final TypeNode second) {
		return ((first instanceof BoolTypeNode) && (second instanceof IntTypeNode | second instanceof BoolTypeNode))
				|| ((first instanceof IntTypeNode) && (second instanceof IntTypeNode));
	}

	/**
	 * Controlla che un tipo sia dello stesso tipo di un altro o un suo sottotipo
	 *
	 * @param first il nodo con il tipo da controllare se è dello stesso tipo del secondo o un sottotipo
	 * @param second il nodo di riferimento sul quale applicare il paragone
	 * @return true se il primo nodo è dello stesso tipo o sottotipo, false in caso contrario
	 */
	public static boolean isSubtype(TypeNode first, TypeNode second) {
		return isBoolAndInt(first, second)
				|| isEmptyTypeAndRefType(first, second)
				|| isSubclass(first, second)
				|| isMethodOverride(first, second);
	}


	/**
	 * Controlla che il primo nodo sia della classe padre del secondo, in sostanza è un subtype con i nodi invertiti
	 *
	 * @param first nodo da controllare se è della classe padre del secondo
	 * @param second nodo di riferimento sul quale fare il paragone
	 * @return true se il primo nodo è della classe padre del primo, false in caso contrario
	 */
	public static boolean isSupertype(final TypeNode first, final TypeNode second) {
		return isSubtype(second, first);
	}

	/**
	 *  Verifica se il primo tipo del nodo è una sottoclasse del secondo tipo
	 *
	 * @param first nodo da controllare se è della classe figlia del secondo
	 * @param second nodo di riferimento sul quale fare il paragone
	 * @return true se il primo è sottoclasse del secondo, false altrimenti
	 */
	private static boolean isSubclass(final TypeNode first, final TypeNode second) {
		if (!(first instanceof RefTypeNode firstRefTypeNode)
				|| !(second instanceof RefTypeNode secondRefTypeNode)) {
			return false;
		}

		return superTypes(firstRefTypeNode.typeId)
				.anyMatch(secondRefTypeNode.typeId::equals);
	}

	/**
	 * Controlla la classe originaria da cui sono derivati i due nodi presi in esame
	 *
	 * @param first primo nodo di cui trovare la classe primordiale
	 * @param second secondo nodo di cui trovare la classe primordiale
	 * @return la classe del tipo originario da cui sono derivate le altre classi
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

	/**
	 * Controlla che il metodo di una classe sia ereditato da un metodo di una classe padre
	 *
	 * @param first nodo che contiene il metodo da controllare se è ereditato dal secondo
	 * @param second nodo su cui effettuare il paragone
	 * @return true se il metodo è ereditato dal secondo, false altrimenti
	 */
	private static boolean isMethodOverride(final TypeNode first, final TypeNode second) {
		if (!(first instanceof ArrowTypeNode firstArrowTypeNode) ||
				!(second instanceof ArrowTypeNode secondArrowTypeNode)) {
			return false;
		}

		// Covariance of return type
		if (!isSubtype(firstArrowTypeNode.returnType, secondArrowTypeNode.returnType)) {
			return false;
		}

		// Contravariance of parameters
		for (TypeNode parameterType : firstArrowTypeNode.parameters) {
			if (!isSupertype(secondArrowTypeNode.returnType, parameterType)) {
				return false;
			}
		}

		return true;
	}




}
