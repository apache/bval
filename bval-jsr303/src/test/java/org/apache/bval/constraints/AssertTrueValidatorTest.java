package org.apache.bval.constraints;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Checks correct behaviour of {@link AssertTrueValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be true.</li>
 * <li><code>null</code> elements are considered valid.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.3"
 * 
 * @author Carlos Vara
 */
public class AssertTrueValidatorTest extends TestCase {
	
    public static Test suite() {
        return new TestSuite(AssertTrueValidatorTest.class);
    }
    
    public AssertTrueValidatorTest(String name) {
    	super(name);
    }
    
    /**
     * Test {@link AssertTrueValidator} with null context.
     */
    public void testAssertTrueValidator() {
    	AssertTrueValidator atv = new AssertTrueValidator();
    	assertTrue("True value validation must succeed", atv.isValid(true, null));
    	assertFalse("False value validation must fail", atv.isValid(false, null));
    	assertTrue("Null value validation must succeed", atv.isValid(null, null));
    }

}
