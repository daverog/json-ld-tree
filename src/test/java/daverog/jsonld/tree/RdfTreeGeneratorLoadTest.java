package daverog.jsonld.tree;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
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
	
	@Ignore
	public void a_large_graph_of_250K_of_Turtle_takes_only_10_seconds_to_convert_to_JSON_100_times() throws RdfTreeException {
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

  @Test
  public void load_test_for_proven_slow_conversion() throws RdfTreeException {
    Model model = ModelUtils.createJenaModel(
        TestResourceLoader.loadClasspathResourceAsString("fixtures/creative-works-about-cardiff.ttl"));
    System.out.println("Number of statements: " + model.size());

    long before = System.currentTimeMillis();
    for (int i=0; i < 50; i++) {
      long innerBefore = System.currentTimeMillis();
      generator.generateRdfTree(model).asJson();
      long innerTimeTakenInMs = System.currentTimeMillis() - innerBefore;
//      System.out.println("\tInner Time taken (ms): " + innerTimeTakenInMs);
    }
    long timeTakenInMs = System.currentTimeMillis() - before;
    assertTrue("Took too long too perform TTL -> JSON-LD conversion", timeTakenInMs < 300 * 10);
    System.out.println("Time taken (ms): " + timeTakenInMs);
  }
}
