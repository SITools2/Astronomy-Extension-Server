 /*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application.opensearch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Index set from Solr index. The index contains the following fields: - index name - data type - map of top terms - boolean showing when
 * the index is stored in Solr - boolean showing when top terms can be categorized
 *
 * @author Jean-Christophe Malapert
 */
public class Index {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Index.class.getName());
  /**
   * This constant defines the maximal number of disctinct values in a category.
   * When a parameter is a category, then distinct values are computed for this parameter.
   */
  public static final int MAX_TOP_TERMS = 30;
  /**
   * Index name.
   */
  private transient String name;
  /**
   * Stored in the index.
   */
  private transient boolean stored;
  /**
   * data type.
   */
  private transient DataType datatype;
  /**
   * dictinct values for a parameter considered as a parameter.
   */
  private transient Map<String, Long> topTerms;
  /**
   * defines if a parameter is a category. The definition of a category depends on MAX_TOP_TERMS.
   */
  private transient boolean canBeCategorized;

  /**
   * Returns the Solr index name.
   *
   * @return the Solr index name
   */
  public final String getName() {
    return name;
  }

  /**
   * Returns <code>true</code> if the index name is stored otherwise <code>false</code>.
   *
   * @return <code>true</code> if the index name is stored otherwise <code>false</code>
   */
  public final boolean isStored() {
    return stored;
  }

  /**
   * Returns the dataType.
   *
   * @return the datatype
   */
  public final DataType getDatatype() {
    return datatype;
  }

  /**
   * Returns the top terms.
   *
   * @return the topTerms
   */
  public final Map<String, Long> getTopTerms() {
    return Collections.unmodifiableMap(topTerms);
  }

  /**
   * Shows if the Solr index name is categorized.
   *
   * @return the canBeCategorized
   */
  public final boolean isCanBeCategorized() {
    return canBeCategorized;
  }

  /**
   * Datatype.
   */
  public enum DataType {

    /**
     * SOLR datatype for a number.
     */
    NUMBER(Arrays.asList("sinteger", "slong", "sfloat", "sdouble")),
    /**
     * SOLR datatype for a date.
     */
    DATE(Arrays.asList("date")),
    /**
     * SOLR datatype for a text.
     */
    TEXT(Arrays.asList("text", "text_ws", "string", "integer", "long", "float", "double", "boolean"));
    /**
     * List of solr data types.
     */
    private final List<String> solrTypes;

    /**
     * Constructor.
     *
     * @param solrTypesVal solr data type
     */
    DataType(final List<String> solrTypesVal) {
      this.solrTypes = solrTypesVal;
    }

    /**
     * Returns the SOLR data types.
     *
     * @return the SOLR data types
     */
    public List<String> getSolrTypes() {
      return Collections.unmodifiableList(this.solrTypes);
    }

    /**
     * Registers a new Solr data type.
     *
     * @param solrDataTypeName solr data type name
     * @param dataType data type
     */
    public static void registerNewSolrDataType(final String solrDataTypeName, final DataType dataType) {
      dataType.getSolrTypes().add(solrDataTypeName);
    }

    /**
     * Return the enum from its data type.
     *
     * @param solrDataTypeName solr data type name
     * @return the enum from its data type
     */
    public static DataType getDataTypeFromSolrDataTypeName(final String solrDataTypeName) {
      final DataType[] dataTypeArray = DataType.values();
      for (int i = 0; i < dataTypeArray.length; i++) {
        final DataType dataType = dataTypeArray[i];
        final List<String> solrIndexesName = dataType.getSolrTypes();
        for (String solrIndex : solrIndexesName) {
          if (solrIndex.equals(solrDataTypeName)) {
            return dataType;
          }
        }
      }
      throw new IllegalArgumentException(solrDataTypeName + " is not supported as solr datatype");
    }
  }

  /**
   * Constructor.
   *
   * @param nameVal index name
   * @param storedVal stored
   * @param datatypeVal datatype
   * @param topTermsVal top terms
   */
  public Index(final String nameVal, final boolean storedVal, final DataType datatypeVal, final Map<String, Long> topTermsVal) {
    this.name = nameVal;
    this.stored = storedVal;
    this.datatype = datatypeVal;
    this.topTerms = topTermsVal;
    this.canBeCategorized = computeCanBeCategorized();
  }

  /**
   * An index is categorized when the size of topTerms < MAX_TOP_TERMS.
   *
   * @return Returns true when topTerms < MAX_TOP_TERMS otherwise false
   */
  private boolean computeCanBeCategorized() {
    return (!getTopTerms().isEmpty() && getTopTerms().size() < MAX_TOP_TERMS) ? true : false;
  }

  /**
   * Returns the population based on topTerms computation.
   *
   * @return Returns the population of an index
   */
  public final long getPopulation() {
    assert computeCanBeCategorized();
    long population = 0;
    for (String term : getTopTerms().keySet()) {
      population += getTopTerms().get(term);
    }
    return population;
  }
}
