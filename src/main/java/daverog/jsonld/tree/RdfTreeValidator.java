package daverog.jsonld.tree;

import com.google.common.base.Function;
import com.google.common.collect.*;

import java.util.*;

public class RdfTreeValidator {

    public void keysCannotHaveSameValue(Map<String, String> map) throws IllegalArgumentException {
        SetMultimap<String, String> multimap = Multimaps.forMap(map);
        Multimap<String, String> inverse = Multimaps.invertFrom(multimap, HashMultimap.<String, String> create()); 
	for (Collection collection : inverse.asMap().values()) {
	    if (collection.size() > 1) throw new IllegalArgumentException("An alias cannnot have multiple URI's. The values are: " + collection); 
        }  
    } 
}
