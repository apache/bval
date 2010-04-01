package org.apache.bval.jsr303.extensions;

import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.NotImplementedException;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;


/**
 * Implementation of {@link AccessStrategy} for method parameters.
 *
 * @author Carlos Vara
 */
public class ParameterAccess extends AccessStrategy {

	private Type paramType;
	private int paramIdx;

	public ParameterAccess(Type paramType, int paramIdx ) {
		this.paramType = paramType;
		this.paramIdx = paramIdx;
	}

	@Override
	public Object get(Object instance) {
		throw new NotImplementedException("Obtaining a parameter value not yet implemented");
	}

	@Override
	public ElementType getElementType() {
		return ElementType.PARAMETER;
	}

	@Override
	public Type getJavaType() {
		return this.paramType;
	}

	@Override
	public String getPropertyName() {
		return "" + paramIdx;
	}

}
