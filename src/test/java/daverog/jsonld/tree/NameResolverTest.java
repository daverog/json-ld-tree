package daverog.jsonld.tree;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.rdf.model.Model;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NameResolverTest {
	
	private List<String> prioritisedNamespaces = Lists.newArrayList("http://prefix.com/", "http://prefix2.com/");
    private Map<String, String> nameOverrides = Maps.newHashMap();

	@Test
	public void a_name_for_a_resource_is_the_full_uri_if_a_prefix_is_not_provided() {
		Model model = ModelUtils.createJenaModel(
				"<uri:a> <uri:b> <uri:c> .");
		NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");
		
		assertEquals("uri:full", nameResolver.getName(
				model.createResource("uri:full")));
		assertNull(nameResolver.getMappedResources().get("uri:full"));
	}

	@Test
	public void a_name_for_a_resource_is_the_local_name_if_a_prefix_is_provided() {
		Model model = ModelUtils.createJenaModel(
				"@prefix prfx: <http://prefix.com/> .\n" +
				"<uri:a> prfx:localName <uri:c> .");
		NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");
		
		assertEquals("localName", nameResolver.getName(
				model.getResource("http://prefix.com/localName")));
		assertEquals("http://prefix.com/localName", nameResolver.getMappedResources().get("localName").getResource().getURI());
	}

    @Test
    public void a_prefixed_name_for_a_resource_is_the_prefixed_local_name_if_a_prefix_is_provided() {
        Model model = ModelUtils.createJenaModel(
                "@prefix prfx: <http://prefix.com/> .\n" +
                        "<uri:a> prfx:localName <uri:c> .");
        NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");

        assertEquals("prfx:localName", nameResolver.getPrefixedName(model.getResource("http://prefix.com/localName")));
    }
	
	@Test
	public void the_namespace_URI_map_is_the_same_size_as_the_number_of_prefixed_URIs() {
		Model model = ModelUtils.createJenaModel(
				"@prefix prfx: <http://prefix.com/> .\n" +
				"<uri:a> prfx:localName <uri:c> .\n" +
				"<uri:a> prfx:localName <uri:d> .");
		NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");
		
		assertEquals(1, nameResolver.getMappedResources().size());
	}
	
	@Test
	public void a_name_for_a_resource_is_the_local_name_with_prefix_if_prefix_is_provided_and_is_lower_priority_namespace() {
		Model model = ModelUtils.createJenaModel(
				"@prefix prfx: <http://prefix.com/> .\n" +
				"@prefix prfx2: <http://prefix2.com/> .\n" +
				"<uri:a> prfx:localName <uri:c> .\n" +
				"<uri:a> prfx2:localName <uri:c> .");
		NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");
		
		assertEquals("prfx2_localName", nameResolver.getName(
				model.getResource("http://prefix2.com/localName")));
		assertEquals("http://prefix2.com/localName", nameResolver.getMappedResources().get("prfx2_localName").getResource().getURI());
	}
	
	@Test
	public void a_name_for_a_resource_is_the_local_name_with_prefix_if_prefix_is_provided_and_is_unprioritised_namespace() {
		Model model = ModelUtils.createJenaModel(
				"@prefix prfx: <http://prefix.com/> .\n" +
				"@prefix prfx3: <http://prefix3.com/> .\n" +
				"<uri:a> prfx:localName <uri:c> .\n" +
				"<uri:a> prfx3:localName <uri:c> .");
		NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, nameOverrides, "");
		
		assertEquals("prfx3_localName", nameResolver.getName(
				model.getResource("http://prefix3.com/localName")));
		assertEquals("http://prefix3.com/localName", nameResolver.getMappedResources().get("prfx3_localName").getResource().getURI());
	}

    @Test
    public void a_name_for_a_resource_is_the_overridden_name_if_one_is_provided() {
        Model model = ModelUtils.createJenaModel(
                "@prefix prfx: <http://badgers.com/> .\n" +
                        "<uri:a> prfx:localName <uri:c> .");

        Map<String, String> overrides = ImmutableMap.of("http://badgers.com/localName", "badgers");
        NameResolver nameResolver = new NameResolver(model, prioritisedNamespaces, overrides, "");

        assertEquals("badgers", nameResolver.getPrefixedName(model.getResource("http://badgers.com/localName")));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Test
    public void disallow_two_uris_from_having_the_same_alias() throws RdfTreeException {
        Model model = ModelUtils.createJenaModel("<uri:a> <uri:b> <uri:c> .");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("A name override cannot map to multiple URIs: [http://purl.org/ns/b, http://purl.org/ns/a]");
        Map<String, String> overrides = ImmutableMap.of("http://purl.org/ns/a", "a", "http://purl.org/ns/b", "a", "http://purl.org/ns/c", "c");
        new NameResolver(model, prioritisedNamespaces, overrides, "");
    }
}
