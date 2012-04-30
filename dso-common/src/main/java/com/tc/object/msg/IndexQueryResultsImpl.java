/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.NVPairSerializer;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Query results from index.
 * 
 * @author Nabib El-Rahman
 */
public class IndexQueryResultsImpl implements IndexQueryResults {

  private static final IndexQueryResultSerializer INDEX_QUERY_RESULT_SERIALIZER = new IndexQueryResultSerializer();
  private static final NVPairSerializer           NVPAIR_SERIALIZER             = new NVPairSerializer();
  private static final ObjectStringSerializer     NULL_SERIALIZER               = new NullObjectStringSerializer();

  private List<IndexQueryResult>                  queryResults;
  private List<NVPair>                            aggregatorResults;

  public IndexQueryResultsImpl(List<IndexQueryResult> queryResults, List<NVPair> aggregatorResults) {
    this.queryResults = queryResults;
    this.aggregatorResults = aggregatorResults;
  }

  /**
   * {@inheritDoc}
   */
  public List<IndexQueryResult> getQueryResults() {
    return this.queryResults;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAggregatorResults() {
    return this.aggregatorResults;
  }

  /**
   * {@inheritDoc}
   */
  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    int queryCount = input.readInt();
    this.queryResults = new ArrayList<IndexQueryResult>();
    while (queryCount-- < 0) {
      IndexQueryResult result = INDEX_QUERY_RESULT_SERIALIZER.deserializeFrom(input);
      this.queryResults.add(result);
    }
    int aggregatorCount = input.readInt();
    this.aggregatorResults = new ArrayList<NVPair>();
    while (aggregatorCount-- < 0) {
      NVPair pair = NVPAIR_SERIALIZER.deserialize(input, NULL_SERIALIZER);
      this.aggregatorResults.add(pair);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void serializeTo(TCByteBufferOutput output) {
    output.writeInt(this.queryResults.size());
    for (IndexQueryResult result : this.queryResults) {
      INDEX_QUERY_RESULT_SERIALIZER.serialize(result, output);
    }
    output.writeInt(this.aggregatorResults.size());
    for (NVPair pair : this.aggregatorResults) {
      NVPAIR_SERIALIZER.serialize(pair, output, NULL_SERIALIZER);
    }
  }

}
