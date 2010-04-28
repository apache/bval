package org.apache.bval.jsr303.example;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

/**
 * A continent has a name and a set of {@link Country}s.
 * 
 * @author Carlos Vara
 */
public class Continent {

    @NotNull
    public String name;
    
    @Valid
    public Set<Country> countries = new HashSet<Country>();
    
}
