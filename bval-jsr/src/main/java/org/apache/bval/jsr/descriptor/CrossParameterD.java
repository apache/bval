package org.apache.bval.jsr.descriptor;

import java.lang.reflect.Executable;

import javax.validation.metadata.CrossParameterDescriptor;

public class CrossParameterD<P extends ExecutableD<?, ?, P>, E extends Executable>
    extends ElementD.NonRoot<P, E, MetadataReader.ForElement<E, ?>> implements CrossParameterDescriptor {

    protected CrossParameterD(MetadataReader.ForElement<E, ?> reader, P parent) {
        super(reader, parent);
    }

    @Override
    public Class<?> getElementClass() {
        return Object[].class;
    }
}
