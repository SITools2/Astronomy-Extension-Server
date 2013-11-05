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
package fr.cnes.sitools.extensions.common;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import net.ivoa.xml.votable.v1.Data;
import net.ivoa.xml.votable.v1.Field;
import net.ivoa.xml.votable.v1.Info;
import net.ivoa.xml.votable.v1.Param;
import net.ivoa.xml.votable.v1.Resource;
import net.ivoa.xml.votable.v1.Table;
import net.ivoa.xml.votable.v1.TableData;
import net.ivoa.xml.votable.v1.Td;
import net.ivoa.xml.votable.v1.Tr;

/**
 * Utility class.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class Utility {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Utility.class.getName());
  

  /**
   * Checks if an object is not null.
   * @param obj object to test
   * @return True when obj is not null otherwise false
   */
  public static boolean isSet(final Object obj) {
    return (obj == null) ? false : true;
  }
  
  /**
   * Returns the right data type for a specific value.
   *
   * @param dataType data type
   * @param value value to convert
   * @return the right data type for a specific value
   */
  public static Object getDataType(final net.ivoa.xml.votable.v1.DataType dataType, final String value) { 
    Object response;
    final String valueTrim = value.trim();
    switch (dataType) {
      case DOUBLE:
        response = Double.valueOf(valueTrim);
        break;
      case DOUBLE_COMPLEX:
        response = Double.valueOf(valueTrim);
        break;
      case FLOAT:
        response = Float.valueOf(valueTrim);
        break;
      case FLOAT_COMPLEX:
        response = Float.valueOf(valueTrim);
        break;
      case INT:
        response = Integer.valueOf(valueTrim);
        break;
      case LONG:
        response = Long.valueOf(valueTrim);
        break;
      default:
        response = valueTrim;
        break;
    }
    return response;
  }

  /**
   * Returns the right ascension in degree.
   * 
   * This parser handles right ascension in degree or in h:m:s
   * @param iterDoc Map that contains the data
   * @param field field to retrieve from the iterdoc
   * @return the right ascension in degree.
   */
  public static double parseRaVO(final Map<Field, String> iterDoc, final Field field) {
    final String valRa = iterDoc.get(field);
    double raValue;
    if (Utility.isSet(field.getUnit()) && field.getUnit().contains("h:m:s")) {
      raValue = AstroCoordinate.parseRa(valRa);
    } else {
      raValue = Double.valueOf(valRa);
    }
    return raValue;
  }
  
  /**
   * Returns the declination in degree.
   *
   * This parser handles declination in degree or in h:m:s
   * @param iterDoc Map that contains the data
   * @param field field to retrieve from the iterdoc
   * @return the right ascension in degree.
   */
  public static double parseDecVO(final Map<Field, String> iterDoc, final Field field) {
    final String valDec = iterDoc.get(field);
    double decValue;
    if (Utility.isSet(field.getUnit()) && field.getUnit().contains("d:m:s")) {
      decValue = AstroCoordinate.parseDec(valDec);
    } else {
      decValue = Double.valueOf(valDec);
    }
    return decValue;
  }

  /**
   * Converts a MJD to ISO date.
   *
   * @param modifiedJulianDate modified julian date
   * @return ISO date
   */
  public static String modifiedJulianDateToISO(final double modifiedJulianDate) {
    final double julianDate = modifiedJulianDate + 2400000.5;
    return julianDateToISO(julianDate);
  }

  /**
   * Converts a JD to ISO date.
   *
   * @param julianDate julian date
   * @return ISO date
   */
  public static String julianDateToISO(final double julianDate) {
    try {
      // Calcul date calendrier Grégorien à partir du jour Julien éphéméride
      // Tous les calculs sont issus du livre de Jean MEEUS "Calcul astronomique"
      // Chapitre 3 de la société astronomique de France 3 rue Beethoven 75016 Paris
      // Tel 01 42 24 13 74
      // Valable pour les années négatives et positives mais pas pour les jours Juliens négatifs
      double jd = julianDate;
      double z, f, a, b, c, d, e, m, aux;
      Date date = new Date();
      jd += 0.5;
      z = Math.floor(jd);
      f = jd - z;

      if (z >= 2299161.0) {
        a = Math.floor((z - 1867216.25) / 36524.25);
        a = z + 1 + a - Math.floor(a / 4);
      } else {
        a = z;
      }

      b = a + 1524;
      c = Math.floor((b - 122.1) / 365.25);
      d = Math.floor(365.25 * c);
      e = Math.floor((b - d) / 30.6001);
      aux = b - d - Math.floor(30.6001 * e) + f;
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(date);
      calendar.set(Calendar.DAY_OF_MONTH, (int) aux);

      double hhd = aux - calendar.get(Calendar.DAY_OF_MONTH);
      aux = ((aux - calendar.get(Calendar.DAY_OF_MONTH)) * 24);

      calendar.set(Calendar.HOUR_OF_DAY, (int) aux);
      calendar.set(Calendar.MINUTE, (int) ((aux - calendar.get(Calendar.HOUR_OF_DAY)) * 60));

      // Calcul secondes
      double mnd = (24 * hhd) - calendar.get(Calendar.HOUR_OF_DAY);
      double ssd = (60 * mnd) - calendar.get(Calendar.MINUTE);
      int ss = (int) (60 * ssd);
      calendar.set(Calendar.SECOND, ss);

      if (e < 13.5) {
        m = e - 1;
      } else {
        m = e - 13;
      }
      // Se le resta uno al mes por el manejo de JAVA, donde los meses empiezan en 0.
      calendar.set(Calendar.MONTH, (int) m - 1);
      if (m > 2.5) {
        calendar.set(Calendar.YEAR, (int) (c - 4716));
      } else {
        calendar.set(Calendar.YEAR, (int) (c - 4715));
      }

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss");
      //System.out.println("Appnumber= "+appNumber+" TimeStamp="+timeStamp+" Julian Date="+julianDateStr+" Converted Date="+sdf.format(calendar.getTime()));
      return sdf.format(calendar.getTime());

    } catch (Exception ex) {
      LOG.log(Level.FINEST, ex.getMessage());
    }
    return null;
  }
  
  public static Object array1DTo2D(Object data, int bitpix, int width, int height) {
      Object obj = null;      
      switch (bitpix) {
          case 8:
              byte[] dataB = (byte[])data;
              byte[][] resultB = new byte[height][width];
              for (int i = 0; i<height; i++) {
                for (int j=0 ; j<width; j++) {
                    resultB[i][j] = dataB[i*width+j];
                }
              }
              obj = resultB;
              break;
          case 16:
              short[] dataS = (short[])data;
              short[][] resultS = new short[height][width];
              for (int i = 0; i<height; i++) {
                for (int j=0 ; j<width; j++) {
                    resultS[i][j] = dataS[i*width+j];
                }
              }
              obj = resultS;
              break;
          case 32:
              int[] dataI = (int[])data;
              int[][] resultI = new int[height][width];
              for (int i = 0; i<height; i++) {
                for (int j=0 ; j<width; j++) {
                    resultI[i][j] = dataI[i*width+j];
                }
              }
              obj = resultI;
              break;       
          case -32:
              float[] dataF = (float[])data;
              float[][] resultF = new float[height][width];
              for (int i = 0; i<height; i++) {
                for (int j=0 ; j<width; j++) {
                    resultF[i][j] = dataF[i*width+j];
                }
              }
              obj = resultF;
              break;              
          case -64:
              double[] dataD = (double[])data;
              double[][] resultD = new double[height][width];
              for (int i = 0; i<height; i++) {
                for (int j=0 ; j<width; j++) {
                    resultD[i][j] = dataD[i*width+j];
                }
              }
              obj = resultD;
              break;
          default:
              break;           
      }
      return obj;              
  }
  
    /**
     * Parse Resource from VOTable.
     * @param resourceIter Resource
     * @return records
     */
    public static List<Map<Field, String>> parseResource(final Resource resourceIter) {
        final List<Info> infos = resourceIter.getINFO();
        for (Info info : infos) {
            final String status = info.getValueAttribute();
            if ("ERROR".equals(status)) {
                throw new IllegalArgumentException(info.getValue());
            }
        }
        List<Map<Field, String>> responses = new ArrayList<Map<Field, String>>();
        final List<Object> objects = resourceIter.getLINKAndTABLEOrRESOURCE();
        for (Object objectIter : objects) {
            if (objectIter instanceof Table) {
                final Table table = (Table) objectIter;
                responses = parseTable(table);
            }
        }
        return responses;
    }
    /**
     * Parse table from VO.
     * @param table table
     * @return records
     */
    private static List<Map<Field, String>> parseTable(final Table table) {
        int nbFields = 0;
        List<Map<Field, String>> responses = new ArrayList<Map<Field, String>>();
        final Map<Integer, Field> responseFields = new HashMap<Integer, Field>();
        final List<JAXBElement<?>> currentTable = table.getContent();
        for (JAXBElement<?> currentTableIter : currentTable) {
            // metadata case
            if (currentTableIter.getValue() instanceof Param) {
              // Need this condition. It seems For a Param tag
              // is an instance of Field. And we do not want
              // to parse a Param as a Field.
            } else if (currentTableIter.getValue() instanceof Field) {
                final JAXBElement<Field> fields = (JAXBElement<Field>) currentTableIter;
                final Field field = fields.getValue();
                responseFields.put(nbFields, field);
                nbFields++;
                // data
            } else if (currentTableIter.getValue() instanceof Data) {
                final JAXBElement<Data> datas = (JAXBElement<Data>) currentTableIter;
                final Data data = datas.getValue();
                final TableData tableData = data.getTABLEDATA();
                final List<Tr> trs = tableData.getTR();
                for (Tr trsIter : trs) {
                    final Map<Field, String> response = new HashMap<Field, String>();
                    final List<Td> tds = trsIter.getTD();
                    int nbTd = 0;
                    for (Td tdIter : tds) {
                        final String value = tdIter.getValue();
                        response.put(responseFields.get(nbTd), value);
                        nbTd++;
                    }
                    responses.add(response);
                }
            }
        }
        return responses;
    }  
}
