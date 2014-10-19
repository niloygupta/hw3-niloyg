package edu.cmu.lti.f14.hw3.hw3_niloyg.annotators;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_niloyg.casconsumers.DocumentVectorCache;
import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_niloyg.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_niloyg.utils.Utils;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
	  
		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}
	
	 /**
   * Uses Stanford Tokensizer and removes punctuations. Also stems the tokens 
   * and converts to lower case.
   *
   * @param doc input text
   * @return    a list of tokens.
   */

  List<String> tokenize1(String doc) 
  {
    List<String> res = new ArrayList<String>();
    Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
    TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
    Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(doc));
    List<Word> words = tokenizer.tokenize();
    for(Word word:words)
    {
      if(DocumentVectorCache.getInstance().getStopWords().contains(word.word()))
        continue;
      if((p.matcher(word.word()).find()))
        continue;
      res.add(StanfordLemmatizer.stemWord(word.word().toLowerCase()));
    }
    return res;
  }
	

	/**
	 * 
	 * @param jcas
	 * @param doc Document of tokens
	 * Parses the document and extracts the token and it's frequency.
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		List<String> tokenList = tokenize0(docText);
		//List<String> tokenList = tokenize1(docText);
		List<Token> docTokenList = new ArrayList<Token>();
		Map<String,Integer> tokenMap = new HashMap<String,Integer>();
		for(String tokenElem:tokenList)
		{
		  if(tokenMap.containsKey(tokenElem))
		  {
		    int currentFreq = tokenMap.get(tokenElem);
		    tokenMap.put(tokenElem, currentFreq + 1);
		  }
		  else
		    tokenMap.put(tokenElem, 1);
		}
		
		for(String tokenElem:tokenMap.keySet())
		{
		  Token token = new Token(jcas);
		  token.setText(tokenElem);
		  token.setFrequency(tokenMap.get(tokenElem));
		  docTokenList.add(token);
		}
		doc.setTokenList(Utils.fromCollectionToFSList(jcas, docTokenList));
		
	}

}
