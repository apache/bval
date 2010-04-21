package org.apache.bval.jsr303;

import junit.framework.TestCase;
import org.apache.bval.util.PropertyAccess;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Pattern;
import java.util.Set;

/**
 * Description: <br>
 * User: roman.stumm<br>
 * Date: 21.04.2010<br>
 * Time: 14:21:45<br>
 * viaboxx GmbH, 2010
 */
public class TckReproducerTest extends TestCase {

  public static <T> void assertCorrectNumberOfViolations(Set<ConstraintViolation<T>> violations,
                                                         int expectedViolations) {
    assertEquals(
        "Wrong number of constraint violations. Expected: " + expectedViolations + " Actual: " + violations.size(),
        expectedViolations, violations.size());
  }

  private Validator getValidator() {
    return ApacheValidatorFactory.getDefault().getValidator();
  }

  public void testPropertyAccessOnNonPublicClass()
      throws Exception {
    Validator validator = getValidator();
    Car car = new Car("USd-298");
    assertEquals(car.getLicensePlateNumber(), PropertyAccess.getProperty(car, "licensePlateNumber"));

    Set<ConstraintViolation<Car>> violations = validator.validateProperty(
        car, "licensePlateNumber", First.class, org.apache.bval.jsr303.example.Second.class
    );
    assertCorrectNumberOfViolations(violations, 1);

    car.setLicensePlateNumber("USD-298");
    violations = validator.validateProperty(
        car, "licensePlateNumber", First.class, org.apache.bval.jsr303.example.Second.class
    );
    assertCorrectNumberOfViolations(violations, 0);
  }

  class Car {
    @Pattern(regexp = "[A-Z][A-Z][A-Z]-[0-9][0-9][0-9]", groups = {First.class, Second.class})
    private String licensePlateNumber;

    Car(String licensePlateNumber) {
      this.licensePlateNumber = licensePlateNumber;
    }

    public String getLicensePlateNumber() {
      return licensePlateNumber;
    }

    public void setLicensePlateNumber(String licensePlateNumber) {
      this.licensePlateNumber = licensePlateNumber;
    }
  }

  interface First {
  }

  interface Second {
  }
}
