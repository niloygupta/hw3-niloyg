package edu.cmu.lti.f14.hw3.hw3_niloyg.typesystems;

import java.util.HashMap;

/**
 * @author niloygupta
 *
 * Wrapper class which stores relevant data from the Document JCAS object.
 * Ensures that document contents are not deleted when CAS is destroyed.
 */
public class DocumentObject {

  private Integer queryID;
  private Integer relevanceValue;
  private Integer rank;
  private Double  cosineSimilarity;
  private String text;
  private HashMap<String,Integer> tokenList;
  
  public DocumentObject(Integer queryID,Integer relevanceValue,Integer rank,Double  cosineSimilarity, String text)
  {
    this.queryID = queryID;
    this.relevanceValue = relevanceValue;
    this.rank = rank;
    this.cosineSimilarity = cosineSimilarity;
    this.text = text;
    tokenList = new HashMap<String,Integer>();
  }
  
  public Integer getQueryID() {
    return queryID;
  }
  public void setQueryID(Integer queryID) {
    this.queryID = queryID;
  }
  public Integer getRelevanceValue() {
    return relevanceValue;
  }
  public void setRelevanceValue(Integer relevanceValue) {
    this.relevanceValue = relevanceValue;
  }
  public Integer getRank() {
    return rank;
  }
  public void setRank(Integer rank) {
    this.rank = rank;
  }
  public Double getCosineSimilarity() {
    return cosineSimilarity;
  }
  public void setCosineSimilarity(Double cosineSimilarity) {
    this.cosineSimilarity = cosineSimilarity;
  }
  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }
  public HashMap<String, Integer> getTokenList() {
    return tokenList;
  }
  public void setTokenList(HashMap<String, Integer> tokenList) {
    this.tokenList = tokenList;
  }

  
  
  
}
