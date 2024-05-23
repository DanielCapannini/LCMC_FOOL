package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class TypeRels {
	public static Map<String, String> superType = new HashMap<>();

	private static boolean isEmptyTypeAndRefType(final TypeNode first, final TypeNode second) {
		return ((first instanceof EmptyTypeNode) && (second instanceof RefTypeNode));
	}

	private static boolean isBoolAndInt(final TypeNode first, final TypeNode second) {
		return ((first instanceof BoolTypeNode) && (second instanceof IntTypeNode | second instanceof BoolTypeNode))
				|| ((first instanceof IntTypeNode) && (second instanceof IntTypeNode));
	}

	private static Stream<String> superTypes(final String type) {
		return Stream.iterate(type, Objects::nonNull, superType::get);
	}

	private static boolean isSubclass(final TypeNode first, final TypeNode second) {
		if (!(first instanceof RefTypeNode firstRefTypeNode)
				|| !(second instanceof RefTypeNode secondRefTypeNode)) {
			return false;
		}

		return superTypes(firstRefTypeNode.typeId).anyMatch(secondRefTypeNode.typeId::equals);
	}

	public static boolean isSubtype(TypeNode first, TypeNode second) {
		return isBoolAndInt(first, second)
				|| isEmptyTypeAndRefType(first, second)
				|| isSubclass(first, second)
				|| isMethodOverride(first, second);
	}

	public static boolean isSupertype(final TypeNode first, final TypeNode second) {
		return isSubtype(second, first);
	}

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
		for (TypeNode parameterType : firstArrowTypeNode.parameterList) {
			if (!isSupertype(secondArrowTypeNode.returnType, parameterType)) {
				return false;
			}
		}

		return true;
	}

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
