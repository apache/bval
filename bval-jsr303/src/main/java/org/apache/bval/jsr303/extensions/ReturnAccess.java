package org.apache.bval.jsr303.extensions;

import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.NotImplementedException;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;

/**
 * Implementation of {@link AccessStrategy} for method return values.
 *
 * @author Carlos Vara
 */
public class ReturnAccess extends AccessStrategy {

	private Type returnType;

	public ReturnAccess(Type returnType) {
		this.returnType = returnType;
	}

	@Override
	public Object get(Object instance) {
		throw new NotImplementedException("Obtaining a method return value not yet implemented");
	}

	@Override
	public ElementType getElementType() {
		return ElementType.METHOD;
	}

	@Override
	public Type getJavaType() {
		return this.returnType;
	}

	@Override
	public String getPropertyName() {
		return "Return value";
	}

}
