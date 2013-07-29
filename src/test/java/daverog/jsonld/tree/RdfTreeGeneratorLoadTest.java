package daverog.jsonld.tree;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import daverog.jsonld.tree.ModelUtils;
import daverog.jsonld.tree.TestResourceLoader;

import com.hp.hpl.jena.rdf.model.Model;

import daverog.jsonld.tree.RdfTreeException;
import daverog.jsonld.tree.RdfTreeGenerator;

public class RdfTreeGeneratorLoadTest {

	private RdfTreeGenerator generator;
	
	@Before
	public void setUp() {
		generator = new RdfTreeGenerator();
	}
	
	@Test
	public void a_large_graph_of_250K_of_Turtle_takes_only_2_mins_to_convert_to_JSON_1000_times() throws RdfTreeException {
		Model model = ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/large.ttl"));
		System.out.println("Number of statements: " + model.size());
				
		long before = System.currentTimeMillis();
		for (int i=0; i < 100; i++) {
			generator.generateRdfTree(ModelUtils.createJenaModel(
				TestResourceLoader.loadClasspathResourceAsString("fixtures/large.ttl")))
				.asJson();
		}
		long timeTakenInMs = System.currentTimeMillis() - before;
		assertTrue("Took too long too perform TTL -> JSON-LD conversion", timeTakenInMs < 1000 * 10);
		System.out.println("Time taken (ms): " + timeTakenInMs);
	}	

}
