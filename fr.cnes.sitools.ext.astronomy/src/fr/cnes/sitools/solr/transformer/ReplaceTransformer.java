/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2.
 * 
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.solr.transformer;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

/**
 * Replaces a string by another one using the DIH transformer.
 *
 * <p>The DIH extracts data and it organizes data by the use of attributes called "field". Each field is considered as a column. Each column
 * can be processed by this transformer. Thus, to use this transformer, set to a field the following attributes: <ul> <li>column, name of
 * the column that has been previouly extracted by the DIH</li> <li>name, name of the field</li> <li>replace, string to replace<li> <li>by,
 * new value of the string to replace</li> </ul> </p>
 *
 * <p> Here is an example how to configure the DIH configuration file
 * <pre><code><field column="..." name="..." replace="..." by="..."/></code></pre> </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ReplaceTransformer extends Transformer {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ReplaceTransformer.class.getName());

  /**
   * Returns the transformation that is applied in this method.
   *
   * @param row row to convert
   * @param context Data Handler context
   * @return new converted row
   */
  @Override
  public final Object transformRow(final Map<String, Object> row, final Context context) {
    List<Map<String, String>> fields = context.getAllEntityFields();
    for (Map<String, String> field : fields) {
      //TODO : Tmp is not clear, To be checked
      String columnName = (field.containsKey("tmp")) ? field.get("tmp") : field.get(DataImporter.COLUMN);
      @SuppressWarnings("UnusedAssignment")
      String value = String.valueOf(row.get(columnName));

      // check of this field has prefix=".." attribute in the data-config.xml
      if (field.containsKey("replace") && field.containsKey("by")) {
        String replace = field.get("replace");
        String by = field.get("by");
        value = String.valueOf(row.get(columnName));
        if (value != null && !columnName.isEmpty()) {
          String result = value.replace(replace, by);
          LOG.log(Level.FINEST, "{0} has been replaced by {1}", new Object[]{value, result});
          row.put(columnName, result);
        }
      }
    }
    return row;
  }
}
