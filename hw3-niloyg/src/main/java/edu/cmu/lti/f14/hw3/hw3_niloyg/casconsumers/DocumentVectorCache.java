package edu.cmu.lti.f14.hw3.hw3_niloyg.casconsumers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems.DocumentObject;

/**
 * @author niloygupta
 * 
 * This is a singleton class which stores the vector and document tokens.
 * 
 *
 */
public class DocumentVectorCache {
  
  private static DocumentVectorCache cache;

  /**
   * Query Vector maps QID to list of tokens part of the query.
   */
  private Map<Integer,HashMap<String,Integer>> queryVector;
  /**
   * docVector groups documents by QID. Maintains a map of tokens per document.
   */
  private Map<Integer,ArrayList<DocumentObject>> docVectors;
  private DocumentVectorCache()
  {
    queryVector = new HashMap<Integer,HashMap<String,Integer>>();
    docVectors = new HashMap<Integer,ArrayList<DocumentObject>>();
  }

  static DocumentVectorCache getInstance()
  {
    if(cache==null)
      cache = new DocumentVectorCache();
    return cache;
  }

  public Map<Integer, HashMap<String, Integer>> getQueryVector() {
    return queryVector;
  }

  public void setQueryVector(Map<Integer, HashMap<String, Integer>> queryVector) {
    this.queryVector = queryVector;
  }

  public Map<Integer, ArrayList<DocumentObject>> getDocVectors() {
    return docVectors;
  }

  public void setDocVectors(Map<Integer, ArrayList<DocumentObject>> docVectors) {
    this.docVectors = docVectors;
  }
  

}
