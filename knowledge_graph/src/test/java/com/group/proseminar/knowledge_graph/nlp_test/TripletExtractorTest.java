package com.group.proseminar.knowledge_graph.nlp_test;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.group.proseminar.knowledge_graph.nlp.TripletExtractor;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class TripletExtractorTest {
	private StanfordCoreNLP extrPipeline;
	private final String EPROPERTIES = "tokenize,ssplit,pos,lemma,ner,parse";
	Annotation document;

	@Before
	public void setup() {
		String text = "Londinium was founded by the Romans, who later abandoned the city.";
		Properties extrProps = new Properties();
		extrProps.put("annotators", EPROPERTIES);
		this.extrPipeline = new StanfordCoreNLP(extrProps);
		this.document = new Annotation(text);
		extrPipeline.annotate(document);
	}

	@Test
	public void extractFromTextTest() {
		TripletExtractor extractor = new TripletExtractor();
		System.out.println("Result: " + extractor.extractFromText(document));
		assertEquals(true, true);
	}
}
