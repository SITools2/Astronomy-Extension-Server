/*************************************************************************
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

/**
 * The VO dictionary is automatically build from the VO response.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class VoDictionary {
  /**
   * Description.
   */
  private String description;
  /**
   * Unit.
   */
  private String unit;
  
  /**
   * Constructor.
   */
  public VoDictionary() {
    this(null, null);
  }
  
  /**
   * Constructor.
   * @param descriptionVal description
   */
  public VoDictionary(final String descriptionVal) {
    this(descriptionVal, null);
  }
  
  /**
   * Constructor.
   * @param descriptionVal description
   * @param unitVal unit
   */
  public VoDictionary(final String descriptionVal, final String unitVal) {
    this.description = descriptionVal;
    this.unit = unitVal;
  }

  /**
   * Returns the description.
   * @return the description
   */
  public final String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   * @param descriptionVal the description to set
   */
  public final void setDescription(final String descriptionVal) {
    this.description = descriptionVal;
  }

  /**
   * Returns the unit.
   * @return the unit
   */
  public final String getUnit() {
    return unit;
  }

  /**
   * Sets the unit.
   * @param unitVal  the unit to set
   */
  public final void setUnit(final String unitVal) {
    this.unit = unitVal;
  }
}