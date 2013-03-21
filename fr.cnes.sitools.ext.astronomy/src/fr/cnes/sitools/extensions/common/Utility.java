/************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *************************************************************************/
package fr.cnes.sitools.extensions.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;

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
   * Full Right ascension range in degree.
   */
  private static final double RANGE_RA_DEG = 360.0;
  
  /**
   * Full Right ascension range in hours.
   */
  private static final double RANGE_RA_HOURS = 24;  
  
  /**
   * Converts hours in degree.
   */
  private static final double HOURSTODEG = RANGE_RA_DEG / RANGE_RA_HOURS;
  
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
    String valueTrim = value.trim();
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
    String valRa = iterDoc.get(field);
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
    String valDec = iterDoc.get(field);
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
   * @param mjd modified julian date
   * @return ISO date
   */
  public static String modifiedJulianDateToISO(final double mjd) {
    double jd = mjd + 2400000.5;
    return julianDateToISO(jd);
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
}