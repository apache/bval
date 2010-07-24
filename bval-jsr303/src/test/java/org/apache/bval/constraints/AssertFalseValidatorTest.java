package org.apache.bval.constraints;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Checks correct behaviour of {@link AssertFalseValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be false.</li>
 * <li><code>null</code> elements are considered valid.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.4"
 * 
 * @author Carlos Vara
 */
public class AssertFalseValidatorTest extends TestCase {
	
    public static Test suite() {
        return new TestSuite(AssertFalseValidatorTest.class);
    }
    
    public AssertFalseValidatorTest(String name) {
    	super(name);
    }
    
    /**
     * Test {@link AssertFalseValidator} with <code>null</code> context.
     */
    public void testAssertFalseValidator() {
    	AssertFalseValidator afv = new AssertFalseValidator();
    	assertFalse("True value validation must fail", afv.isValid(true, null));
    	assertTrue("False value validation must succeed", afv.isValid(false, null));
    	assertTrue("Null value validation must succeed", afv.isValid(null, null));
    }

}
