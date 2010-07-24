package org.apache.bval.constraints;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Checks correct behaviour of {@link NotNullValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must not be null.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.2"
 * 
 * @author Carlos Vara
 */
public class NotNullValidatorTest extends TestCase {

    public static Test suite() {
        return new TestSuite(NotNullValidatorTest.class);
    }
    
    public NotNullValidatorTest(String name) {
    	super(name);
    }
    
    /**
     * Test {@link NotNullValidator} with null context.
     */
    public void testNotNullValidator() {
    	NotNullValidator nnv = new NotNullValidator();
    	assertTrue("Non null value validation must succeed", nnv.isValid("hello", null));
    	assertFalse("Null value validation must fail", nnv.isValid(null, null));    	
    }
	
}
