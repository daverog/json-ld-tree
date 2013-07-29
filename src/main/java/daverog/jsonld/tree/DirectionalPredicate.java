package daverog.jsonld.tree;

import com.hp.hpl.jena.rdf.model.Property;

public class DirectionalPredicate {
	
	private Property predicate;
	private boolean inverse;

	public DirectionalPredicate(Property predicate, boolean inverse) {
		this.predicate = predicate;
		this.inverse = inverse;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (inverse ? 1231 : 1237);
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DirectionalPredicate other = (DirectionalPredicate) obj;
		if (inverse != other.inverse)
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		return true;
	}

}
