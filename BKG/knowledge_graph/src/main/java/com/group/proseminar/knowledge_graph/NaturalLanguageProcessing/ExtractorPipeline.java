package com.group.proseminar.knowledge_graph.NaturalLanguageProcessing;

import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;

public class ExtractorPipeline {

	private StanfordCoreNLP corefPipeline;
	private StanfordCoreNLP extrPipeline;
	private final String CPROPERTIES = "tokenize,ssplit,pos,lemma,ner,parse,dcoref";
	private final String EPROPERTIES = "tokenize,ssplit,pos,lemma,ner,parse";
	CoreferenceResoluter resoluter;
	TripletExtractor extractor;

	ExtractorPipeline() {
		this.resoluter = new CoreferenceResoluter();
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
	}
	
	public void processArticle (String article) {
		Annotation doc = new Annotation(article);
		corefPipeline.annotate(doc);
		String resoluted = resoluter.coreferenceResolution(doc);
		if (resoluted == null) {
			resoluted = article;
		}
		Annotation res = new Annotation(resoluted);
		extrPipeline.annotate(res);
		Set<Triplet<Tree, Tree, Tree>> triplets = extractor.extractFromText(res);
		// TODO: Write triplets to file
		System.out.println("TRIPLETS: " + triplets);
	}
}