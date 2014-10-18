package edu.cmu.lti.f14.hw3.hw3_niloyg.casconsumers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.DocumentObject;
import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_niloyg.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();

	}

	/**
	 *  1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		HashMap<String,Integer> queryTokenMap;
		HashMap<String,Integer> docVector;
		if (it.hasNext()) {
			Document doc = (Document) it.next();
			
			if(doc.getRelevanceValue() == 99)
			{
			  FSList fsTokenList = doc.getTokenList();
	      ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
			  queryTokenMap = new HashMap<String,Integer>();
			  for(Token token:tokenList)
			    queryTokenMap.put(token.getText(), token.getFrequency());
			  DocumentVectorCache.getInstance().getQueryVector().put(doc.getQueryID(), queryTokenMap);
			}
			else
			{
			  if(!DocumentVectorCache.getInstance().getDocVectors().containsKey(doc.getQueryID()))
			    DocumentVectorCache.getInstance().getDocVectors().put(doc.getQueryID(), new ArrayList<DocumentObject>());
			  ArrayList<DocumentObject> docList = DocumentVectorCache.getInstance().getDocVectors().get(doc.getQueryID());
			  FSList fsTokenList = doc.getTokenList();
			  ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
        docVector = new HashMap<String,Integer>();
        for(Token token:tokenList)
          docVector.put(token.getText(), token.getFrequency());
        DocumentObject docObj = new DocumentObject( doc.getQueryID(), doc.getRelevanceValue(),0,doc.getCosineSimilarity(), doc.getText());
        docObj.setTokenList(docVector);
			  docList.add(docObj);
			  DocumentVectorCache.getInstance().getDocVectors().put(doc.getQueryID(), docList);
			}
			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());

		}

	}

	/**
	 *  1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		 PrintWriter writer = null;
	    try {
	      writer = new PrintWriter(new FileOutputStream(new File("src/main/resources/data/results.txt"), false));
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } 
		Map<Integer,HashMap<String,Integer>> queryVector = DocumentVectorCache.getInstance().getQueryVector();
	  Map<Integer,ArrayList<DocumentObject>> docVectors = DocumentVectorCache.getInstance().getDocVectors();
	  double mmrSum = 0.0;
	  
	  for(Integer qId:queryVector.keySet())
	  {
	    ArrayList<DocumentObject> documents = docVectors.get(qId);	
	    double avgdl = getAvgDocumentLength(documents);
	    Map<String,Integer> docFrequency = getDocFrequency(documents,queryVector.get(qId));
	    for(DocumentObject doc:documents)
	     doc.setCosineSimilarity(computeCosineSimilarity( queryVector.get(qId), doc.getTokenList()));
	    Collections.sort(documents, new Comparator<DocumentObject>() {
        @Override
        public int compare(DocumentObject  doc1, DocumentObject  doc2)
        {
            if(doc2.getCosineSimilarity()>doc1.getCosineSimilarity()) return 1;
            if(doc2.getCosineSimilarity()<doc1.getCosineSimilarity()) return -1;
            /* Incase of tie the document with the higher relevance value is returned*/
            if(doc2.getRelevanceValue()==1 && doc1.getRelevanceValue()!=1) return 1;
            if(doc1.getRelevanceValue()==1 && doc2.getRelevanceValue()!=1) return -1;
            return  0;
        }
    });
	    setRank(documents);
	    DocumentObject doc = getMMRDoc(documents);
	    mmrSum += 1/((double)doc.getRank());
	    printDocResults(doc,writer);
	  }
	  double metric_mrr = compute_mrr(mmrSum,queryVector.size());
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + String.format("%.4f",metric_mrr));
    writer.println("MRR="+String.format("%.4f",metric_mrr));
    writer.close();
	}

	/**
	 * @param documents: Sorted documents
	 * Iterate though the list of documents sorted by the similarity measure
	 */
	private void setRank(ArrayList<DocumentObject> documents) {
	  int rank = 0;
    for(DocumentObject doc:documents)
      doc.setRank(++rank);
  }

  /**
   * @param document
   * @param writer
   * Outputs in the required format to the file
   */
  private void printDocResults(DocumentObject document, PrintWriter writer) 
	{
	  writer.println("cosine="+String.format("%.4f",document.getCosineSimilarity())+"\trank="+document.getRank()
	          +"\tqid="+document.getQueryID()
	          +"\trel="+document.getRelevanceValue()
	          +"\t"+document.getText());
    
  }

  /**
   * @param documents
   * @return The highest ranking relevant document
   * 
   * Returns the highest ranking relevant document for a given QID
   */
  private DocumentObject getMMRDoc(ArrayList<DocumentObject> documents) {
	  for(DocumentObject doc:documents)
	  {
	    if(doc.getRelevanceValue()==1)
	      return doc;
	  }
	  return null;
	}
  
  /**
   * @param queryVector
   * @param docVector
   * 
   * Method written for analysing errors (false positive and false negatives)
   */
  private void errorAnalysis(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) 
  {
    System.out.println("\nMissed Query Terms: ");
    for(String query: queryVector.keySet())
    {
      if(!docVector.containsKey(query))
       System.out.print(query+"\t");
    }
    System.out.println("\nMissed Rel Doc Tokens: ");
    for(String token: docVector.keySet())
    {
      if(!queryVector.containsKey(token))
       System.out.print(token+"\t");
    }
  
  }

  /**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;

		double dotProduct = 0.0;
		double queryEucLen = 0.0;
		double docEucLen = 0.0;
		
		for(String query: queryVector.keySet())
		{
		  if(docVector.containsKey(query))
		    dotProduct += queryVector.get(query) * docVector.get(query);
		}
		for(Integer freq:queryVector.values())
		  queryEucLen += freq*freq;
    for(Integer freq:docVector.values())
      docEucLen += freq*freq;
    
    queryEucLen = Math.sqrt(queryEucLen);
    docEucLen = Math.sqrt(docEucLen);
		
    cosine_similarity = dotProduct/(queryEucLen*docEucLen);
    
		return cosine_similarity;
	}
	 /**
   * 
   * @return Jaccard Index
   */
  private double computeJaccardIndex(Map<String, Integer> queryVector,Map<String, Integer> docVector) 
  {
    double JaccardIndex=0.0;
    double Jmin = 0.0;
    double Jmax  = 0.0;
    
    for(String query: queryVector.keySet())
    {
      if(docVector.containsKey(query))
      {
        Jmin += Math.min(docVector.get(query),queryVector.get(query));
        Jmax += Math.max(docVector.get(query),queryVector.get(query));
      }
    }    
    JaccardIndex = Jmin/Jmax;
    
    return JaccardIndex;
  }
  
  /**
  * 
  * @return Sorrensen Dice Coefficient
  */
 private double computeSorrensenDiceCoefficient(Map<String, Integer> queryVector,Map<String, Integer> docVector) 
 {
   double SorrDiceCoef = 0.0;
   double tokenIntersection = 0.0;
   
   for(String query: queryVector.keySet())
   {
     if(docVector.containsKey(query))
       tokenIntersection++;
   }    
   SorrDiceCoef = (2*tokenIntersection)/(queryVector.size()+docVector.size());
   
   return SorrDiceCoef;
 }
 
 /**
  * 
  * @return Okapi BM 25
  */
 private double computeOkapiBm25(Map<String, Integer> queryVector,
         Map<String, Integer> docVector,double avgdl,Map<String,Integer> docFrequency,int docNum) 
 {
   double k1 = 1.5;
   double b = 0.75;
   double BM25 = 0.0;
   for(String queryToken:queryVector.keySet())
   {
     if(docVector.containsKey(queryToken))
     {
       double IDF = Math.log((docNum -docFrequency.get(queryToken)+0.5)/(docFrequency.get(queryToken)+0.5));
       BM25 +=  (IDF*docVector.get(queryToken)*(k1+1))/(docVector.get(queryToken) + k1*(1 - b + b*(docVector.size()/docNum)));
     }
   }
   return BM25;
 }
 
 /**
 * @param documents
 * @return Avg Length of the documents
 * Required for the BM25 similarity metric
 */
private double getAvgDocumentLength(ArrayList<DocumentObject> documents)
 {
   double avgdl = 0.0;
   for(DocumentObject doc:documents)
     avgdl += doc.getTokenList().size();
   return avgdl/documents.size();
 }
 
 /**
 * @param documents
 * @param queryVector
 * @return The number of documents that have the particular token from the query string
 * 
 * Required for the BM25 metric
 */
private Map<String,Integer> getDocFrequency(ArrayList<DocumentObject> documents,Map<String, Integer> queryVector)
 {
   Map<String,Integer> docFrequency = new HashMap<String,Integer>(); 
   int count = 0;
   for(String queryToken:queryVector.keySet())
   {
     count = 0;
     for(DocumentObject doc:documents)
     {
       if(doc.getTokenList().containsKey(queryToken))
         count++;
     }
     docFrequency.put(queryToken, count); 
   }
   return docFrequency;
 }

	/**
	 * 
	 * @param queryCount 
	 * @param findRank 
	 * @return mrr
	 */
	private double compute_mrr(double findRank, int queryCount) {
		double metric_mrr=0.0;

		metric_mrr = findRank/queryCount;
		return metric_mrr;
	}

}
