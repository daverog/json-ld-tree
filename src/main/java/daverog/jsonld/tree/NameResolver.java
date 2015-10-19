package daverog.jsonld.tree;

import com.google.common.collect.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class NameResolver {

	private final Model model;
	private final SortedMap<String, TypedResource> mappedResources;
	private final List<String> prioritisedNamespaces;
	private final BiMap<String, String> nameOverrides;
	private final Map<String, String> inverseNameOverrides;
	private final String rdfResultOntologyPrefix;

	public NameResolver(Model model, List<String> prioritisedNamespaces, Map<String,String> nameOverrides, String rdfResultOntologyPrefix) {
		checkDuplicateNameOverrides(nameOverrides);

		this.model = model;
		this.nameOverrides = HashBiMap.create(nameOverrides);
		this.inverseNameOverrides = this.nameOverrides.inverse();
		this.rdfResultOntologyPrefix = rdfResultOntologyPrefix;
		this.prioritisedNamespaces = Lists.newArrayList(RdfTree.RDF_PREFIX, RdfTree.OWL_PREFIX);
		this.prioritisedNamespaces.addAll(prioritisedNamespaces);

		mappedResources = Maps.newTreeMap();

		registerResources(model);
	}

	private void checkDuplicateNameOverrides(Map<String, String> nameOverrides) {
		Map<String, Collection<String>> inverse = Multimaps.invertFrom(Multimaps.forMap(nameOverrides), HashMultimap.<String, String>create()).asMap();
		for (Collection collection : inverse.values()) {
			if (collection.size() > 1)
				throw new IllegalArgumentException("A name override cannot map to multiple URIs: " + collection);
		}
	}

	private void registerResources(Model model) {
		StmtIterator statements = model.listStatements();
		while(statements.hasNext()) {
			Statement statement = statements.next();

			registerResource(new TypedResource(statement.getSubject(), ResourceType.NONE));

			ResourceType type = ResourceType.NONE;
			if (statement.getObject().isResource())	{
				Resource objectResource = statement.getObject().asResource();
				String nameSpace = objectResource.getNameSpace();
				if (nameSpace != null && model.getNsURIPrefix(nameSpace) != null) {
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
		String nameSpace = resource.getResource().getNameSpace();

		if (shouldRegisterNameSpaceResources(nameSpace) && isPrefixed(resource)) {
			String localName = resource.getResource().getLocalName();
			TypedResource existingResource = mappedResources.get(localName);

			if (existingResource == null) {
				registerNewResource(resource);
			} else if (!resource.getResource().equals(existingResource.getResource())) {
				registerConflictingResources(resource, existingResource);
			}
		}
	}

	private void registerNewResource(TypedResource resource) {
		String localName = resource.getResource().getLocalName();
		String nameOverrideUri = inverseNameOverrides.get(localName);

		if (nameOverrideUri == null || resource.getResource().getURI().equals(nameOverrideUri)) {
            mappedResources.put(localName, resource);
        } else {
            String currentPrefix = getPrefixFromModel(resource);
            mappedResources.put(currentPrefix + "_" + localName, resource);
        }
	}

	private boolean shouldRegisterNameSpaceResources(String nameSpace) {
		return nameSpace != null && !nameSpace.equals(rdfResultOntologyPrefix);
	}

	private boolean isPrefixed(TypedResource resource) {
		return null != getPrefixFromModel(resource);
	}

	private String getPrefixFromModel(TypedResource resource) {
		Resource wrappedResource = resource.getResource();
		return getPrefixFromModel(wrappedResource);
	}

	private String getPrefixFromModel(Resource resource) {
		String nameSpace = resource.getNameSpace();
		return model.getNsURIPrefix(nameSpace);
	}

	private void registerConflictingResources(TypedResource resource, TypedResource existingResource) {
		String nameSpace = resource.getResource().getNameSpace();
		String existingNamespace = existingResource.getResource().getNameSpace();

		if(isFirstHigherPriorityNameSpace(existingNamespace, nameSpace)) {
			String prefix = getPrefixFromModel(resource);
			mappedResources.put(prefix + "_" + resource.getResource().getLocalName(), resource);
        } else {
            String currentPrefix = getPrefixFromModel(existingResource);
            mappedResources.put(currentPrefix + "_" + existingResource.getResource().getLocalName(), existingResource);
            mappedResources.put(resource.getResource().getLocalName(), resource);
        }
	}

	private boolean isFirstHigherPriorityNameSpace(String firstNameSpace, String secondNameSpace) {
		boolean firstIsHigherPriority;
		int priorityOfFirstNameSpace = prioritisedNamespaces.indexOf(firstNameSpace);
		int priorityOfSecondNameSpace = prioritisedNamespaces.indexOf(secondNameSpace);

		if (priorityOfFirstNameSpace == -1 && priorityOfSecondNameSpace == -1) {
            firstIsHigherPriority = firstNameSpace.compareTo(secondNameSpace) < 0;
        } else if (priorityOfFirstNameSpace == -1) {
            firstIsHigherPriority = false;
        } else if (priorityOfSecondNameSpace == -1) {
            firstIsHigherPriority = true;
        } else {
            firstIsHigherPriority = priorityOfFirstNameSpace < priorityOfSecondNameSpace;
        }
		return firstIsHigherPriority;
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

	public String getPrefixedName(Resource resource) {
		if (resource.isAnon()) return "@blank";
		if (resource.getURI().equals(RdfTree.RDF_TYPE)) return "type";
		if (nameOverrides.containsKey(resource.getURI())) return nameOverrides.get(resource.getURI());

		String prefix = getPrefixForResourceUri(resource);
		if (prefix != null) {
			return prefix + ":" + resource.getLocalName();
		} else {
			return resource.getURI();
		}
	}

	public String getPrefixForResourceUri(Resource resource) {
		String nameSpace = resource.getNameSpace();

		if (nameSpace.equals(RdfTree.RDF_PREFIX)) return "rdf";
		if (nameSpace.equals(RdfTree.OWL_PREFIX)) return "owl";
		if (nameOverrides.containsKey(resource.getURI())) return null;

		return getPrefixFromModel(resource);
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
