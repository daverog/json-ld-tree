package daverog.jsonld.tree;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ModelUtils {

	public static String toN3String(Model model) {
		return toString(model, "N3");
	}

	public static String toString(Model model, String lang) {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		Writer writer = new PrintWriter(boas);
		model.write(writer, lang);
		try {
			boas.flush();
		} catch (IOException e) {
			throw new RuntimeException("Graph serialisation error");
		}
		return boas.toString();
	}

	public static Model createJenaModel(String rdfContent){
		return createJenaModel(rdfContent, "TTL");
	}

	public static Model createJenaModel(String rdfContent, String lang) {
		StringReader reader = new StringReader(rdfContent);
		Model model = ModelFactory.createDefaultModel();
		model.read(reader, null, lang);
		return model;
	}

	public static List<Resource> getSubjectsByPredicateAndObject(Model model, String predicatePrefix, String predicateValue,String objectPrefix, String objectValue) {
		Property predicate = model.createProperty(predicatePrefix,predicateValue);
		Property object= model.createProperty(objectPrefix, objectValue);

		return getSubjectsByPredicateAndObject(model, predicate, object);
	}

	public static Resource getSubjectByPredicateAndObject(Model model, String predicatePrefix, String predicateValue, String objectValue) {
		Property predicate = model.createProperty(predicatePrefix,predicateValue);
		Property object= model.createProperty( objectValue );
		List<Resource> subjects = getSubjectsByPredicateAndObject(model, predicate, object);
		if(subjects.size()>0){
			return subjects.get(0);
		}
		return null;
	}

	private static List<Resource> getSubjectsByPredicateAndObject(final Model model, final Property predicate, final Property object) {
		List<Resource> subjects = new ArrayList<Resource>();

		Selector selector = new SimpleSelector(null, predicate, object);
		StmtIterator it = model.listStatements(selector);
		Resource resource = null;
		while (it.hasNext()) {
			Statement statement = it.nextStatement();
			resource = statement.getSubject();
			subjects.add(resource);
		}
		return subjects;
	}

	public static String getObjectValueFromSubjectByPredicate(Model model, Resource subject, String predicatePrefix, String predicateName) {
		String value = "";
		Property p = model.createProperty(predicatePrefix, predicateName);
		Statement statement = subject.getProperty(p);
		if (statement != null) {
			RDFNode resource = statement.getObject();
			value = resource.isLiteral() ? statement.getString() : resource.toString();
		}
		else {
			return null;
		}
		return value;
	}

}
