package daverog.jsonld.tree;

import org.junit.*;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;


public class RdfTreeValidatorTest {
    
    @Rule public ExpectedException exception = ExpectedException.none(); 
    @Test
    public void disallow_two_uris_from_having_the_same_alias() throws IllegalArgumentException { 
	RdfTreeValidator rdfTreeValidator = new RdfTreeValidator();
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("An alias cannnot have multiple URI's. The values are: [http://purl.org/ns/b, http://purl.org/ns/a]");
        rdfTreeValidator.keysCannotHaveSameValue(ImmutableMap.of("http://purl.org/ns/a", "a", "http://purl.org/ns/b", "a", "http://purl.org/ns/c", "c")); 
    }
}
