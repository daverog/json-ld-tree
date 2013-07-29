package daverog.jsonld.tree;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

import com.google.common.collect.Sets;

public class RdfTreeUtils {
	
	public static <T> int compareTwoListsOfValues(
			List<T> firstValues,
			List<T> secondValues,
			Comparator<T> comparator) {
		SortedSet<T> all = Sets.newTreeSet(comparator);
		all.addAll(firstValues);
		all.addAll(secondValues);
		
		for (Object value : all) {
			if (firstValues.contains(value) && !secondValues.contains(value))
				return -1;
			if (!firstValues.contains(value) && secondValues.contains(value))
				return 1;
		}
		
		return 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int compareObjects(Object object1, Object object2) {
		if (object1 instanceof Comparable<?> && object2 instanceof Comparable<?> 
			&& object1.getClass().isAssignableFrom(object2.getClass())) {
			return ((Comparable)object1).compareTo((Comparable)object2);
		}
		return object1.toString().compareTo(object2.toString());
	}

}
