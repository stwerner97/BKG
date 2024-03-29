package com.group.proseminar.knowledge_graph.nlp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import com.group.proseminar.knowledge_graph.ontology.PredicateResolver;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Controls pipeline features of the natural language processing package.
 * 
 * @author Stefan Werner
 *
 */
public class ExtractorPipeline {

	private StanfordCoreNLP corefPipeline;
	private StanfordCoreNLP extrPipeline;
	private final String CPROPERTIES = "tokenize,ssplit,pos,lemma,ner,parse,dcoref";
	private final String EPROPERTIES = "tokenize,ssplit,pos,lemma,ner,parse";
	private CoreferenceResolver corefResolver;
	private TripletExtractor extractor;
	private PredicateResolver predResolver;
	private BufferedWriter writer;
	private Model resultModel;

	/**
	 * Initialize dependent classes and setup annotators.
	 * 
	 * @throws IOException
	 */
	public ExtractorPipeline() throws IOException {
		this.corefResolver = new CoreferenceResolver();
		this.extractor = new TripletExtractor();
		// Initialize corefPipeline
		Properties corefProps = new Properties();
		corefProps.put("annotators", CPROPERTIES);
		corefProps.put("dcoref.score", true);
		this.corefPipeline = new StanfordCoreNLP(corefProps);
		// Initialize extrPipeline
		Properties extrProps = new Properties();
		extrProps.put("annotators", EPROPERTIES);
		this.extrPipeline = new StanfordCoreNLP(extrProps);
		this.predResolver = new PredicateResolver();
		// Initialize writer and model for output
		Path path = Paths.get("src/main/resources/nlp_result.ttl");
		this.writer = new BufferedWriter(new FileWriter(path.toUri().getPath()));
		this.resultModel = ModelFactory.createDefaultModel();
		this.resultModel.setNsPrefix("dbr", "http://dbpedia.org/resource/");
		this.resultModel.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
	}

	/**
	 * Executes pipeline approach on a given article.
	 * 
	 * @param article
	 * @throws Exception
	 */
	public void processArticle(String article) throws Exception {
		CoreDocument doc = new CoreDocument(article);
		corefPipeline.annotate(doc);
		String resolved = corefResolver.resolveCoreferences(doc);
		if (resolved == null) {
			resolved = article;
		}
		Set<Entity> entities = corefResolver.linkEntitiesToMentions(doc);

		System.out.println(entities);

		Annotation res = new Annotation(resolved);
		extrPipeline.annotate(res);
		Set<Triplet<String, String, String>> triplets = extractor.extractFromText(res);
		EntityLinker linker = new EntityLinker();

		Collection<Triplet<String, String, String>> result = new HashSet<>();

		System.out.println("Triplets: " + triplets);

		for (Triplet<String, String, String> triplet : triplets) {
			// handle subject and object
			String subject = triplet.getFirst();
			String predicate = triplet.getSecond();
			String object = triplet.getThird();
			Entity sEntity = linker.getLargestEntity(entities, subject);
			Entity oEntity = linker.getLargestEntity(entities, object);
			if (sEntity != null && oEntity != null) {
				Set<Entity> set = Stream.of(sEntity, oEntity).collect(Collectors.toSet());
				// link subject and object to URIs
				linker.resolveURIs(set);
				// write to triplet
				String subjURI = sEntity.getUri();
				String predURI = predResolver.resolveToURI(predicate);
				String objURI = oEntity.getUri();

				if (predURI == null) {
					String edge = predResolver.getVerbDependend(res, predicate);
					predURI = predResolver.resolveToURI(edge);
				}

				Triplet<String, String, String> uriTriplet = null;
				if (subjURI != null && predURI != null && objURI != null) {
					uriTriplet = new Triplet<>(subjURI, predURI, objURI);
				}
				if (uriTriplet != null) {
					result.add(uriTriplet);
				}

				System.out.println("Subject: " + subject + ", Predicate: " + predicate + ", Object: " + object);
				// Print out progress
				System.out.println("SubjectURI: " + subjURI);
				System.out.println("PredicateURI: " + predURI);
				System.out.println("ObjectURI: " + objURI);
				System.out.println("-----------------------------------------------------------------------------");
			}
		}
		
		// Insert results to model
		result.stream().forEach(x->insertTripletToModel(x.getFirst(), x.getSecond(), x.getThird()));
	}

	/**
	 * Inserts triplets of format subject, predicate, object to a jena-model.
	 * @param sURI - uri of subject
	 * @param pURI - uri of predicate
	 * @param oURI - uri of object
	 */
	private void insertTripletToModel(String sURI, String pURI, String oURI) {
		Resource sResource = this.resultModel.createResource(sURI);
		Property property = this.resultModel.createProperty(pURI);
		Resource oResource = this.resultModel.createResource(oURI);
		this.resultModel.add(sResource, property, oResource);
	}
	
	/**
	 * Write model to file.
	 */
	public void writeResultToFile() {
		this.resultModel.write(writer,"Turtle");
	}
}
