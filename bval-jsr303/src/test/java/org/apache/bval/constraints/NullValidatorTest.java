package org.apache.bval.constraints;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Checks correct behaviour of {@link NullValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be null.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.1"
 * 
 * @author Carlos Vara
 */
public class NullValidatorTest extends TestCase {

    public static Test suite() {
        return new TestSuite(NullValidatorTest.class);
    }
    
    public NullValidatorTest(String name) {
    	super(name);
    }
	
    /**
     * Test {@link AssertFalseValidator} with null context.
     */
    public void testNullValidator() {
    	NullValidator nv = new NullValidator();
    	assertTrue("Null value validation must succeed", nv.isValid(null, null));
    	assertFalse("Non null value validation must fail", nv.isValid("hello", null));    	
    }
    
}
