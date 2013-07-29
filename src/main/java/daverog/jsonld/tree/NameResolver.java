package daverog.jsonld.tree;

import java.util.List;
import java.util.SortedMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class NameResolver {
	
	private final Model model;
	private final SortedMap<String, TypedResource> mappedResources;
	private final List<String> prioritisedNamespaces;

	public NameResolver(Model model, List<String> prioritisedNamespaces) {
		this.model = model;
		this.prioritisedNamespaces = Lists.newArrayList(RdfTree.RDF_PREFIX, RdfTree.OWL_PREFIX);
		this.prioritisedNamespaces.addAll(prioritisedNamespaces);
		mappedResources = Maps.newTreeMap();
		
		StmtIterator statements = model.listStatements();
		while(statements.hasNext()) {
			Statement statement = statements.next();
			
			registerResource(new TypedResource(statement.getSubject(), ResourceType.NONE));

			ResourceType type = ResourceType.NONE;
			if (statement.getObject().isResource())	{
				Resource objectResource = statement.getObject().asResource();
				if (objectResource.getNameSpace() != null && model.getNsURIPrefix(objectResource.getNameSpace()) != null) {
					type = ResourceType.VOCAB;
				} else {
					type = ResourceType.ID;
				}
				registerResource(new TypedResource(objectResource, ResourceType.NONE));
			}
			
			registerResource(new TypedResource(statement.getPredicate(), type));
		}
	}

	private void registerResource(TypedResource resource) {
		if (resource.getResource().getNameSpace() != null && !resource.getResource().getNameSpace().equals(RdfTree.RESULT_ONTOLOGY_URI_PREFIX)) {
			String namespace = resource.getResource().getNameSpace();
			String prefix = model.getNsURIPrefix(namespace);
			if (prefix != null && namespace != null) {
				TypedResource currentResource = mappedResources.get(resource.getResource().getLocalName());
				if (currentResource == null) {
					mappedResources.put(resource.getResource().getLocalName(), resource);
				} else {
					if (!resource.getResource().equals(currentResource.getResource())) {
						if(prioritisedNamespaces.indexOf(currentResource.getResource().getNameSpace()) == -1 ||
							prioritisedNamespaces.indexOf(namespace) <
						    prioritisedNamespaces.indexOf(currentResource.getResource().getNameSpace())) {
							String currentPrefix = model.getNsURIPrefix(currentResource.getResource().getNameSpace());
							mappedResources.put(currentPrefix + "_" + currentResource.getResource().getLocalName(), currentResource);
							mappedResources.put(resource.getResource().getLocalName(), resource);
						} else {
							mappedResources.put(prefix + "_" + resource.getResource().getLocalName(), resource);
						}
					}
				}
			}
		}
	}

	public String getName(Resource resource) {
		if (resource.isAnon()) return "@blank";
		if (resource.getURI().equals(RdfTree.RDF_TYPE)) return "type";
		
		TypedResource mappedResource = mappedResources.get(resource.getLocalName());
		if (mappedResource != null && mappedResource.getResource().equals(resource)) {
			return resource.getLocalName();
		}
		
		String prefix = getPrefixForResourceUri(resource);
		if (prefix != null) {
			return prefix + "_" + resource.getLocalName();
		} else {
			return resource.getURI();
		}
	}

	private String getPrefixForResourceUri(Resource resource) {
		if (resource.getNameSpace().equals(RdfTree.RDF_PREFIX)) return "rdf";
		if (resource.getNameSpace().equals(RdfTree.OWL_PREFIX)) return "owl";
		
		return model.getNsURIPrefix(resource.getNameSpace());
	}

	public int compareNames(Resource resource, Resource otherResource) {
		TypedResource mappedResource = mappedResources.get(resource.getLocalName());
		TypedResource otherMappedResource = mappedResources.get(otherResource.getLocalName());
		
		if (mappedResource == null && otherMappedResource != null) return -1;
		if (mappedResource != null && otherMappedResource == null) return 1;
		
		return getName(resource).compareTo(getName(otherResource));
	}

	public SortedMap<String, TypedResource> getMappedResources() {
		return mappedResources;
	}

	protected class TypedResource {
		private final Resource resource;
		private final ResourceType type;

		private TypedResource(Resource resource, ResourceType type) {
			this.resource = resource;
			this.type = type;
		}

		public Resource getResource() {
			return resource;
		}

		public ResourceType getType() {
			return type;
		}
	}
	
	protected enum ResourceType {
		NONE,
		VOCAB,
		ID
	}
	
}
