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
package fr.cnes.sitools.astro.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

/**
 * Test of NameResolver object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NameResolverTest {

  /**
   * Test of getCoordinates method, of class AbstractNameResolver.
   */
  @Test
  public final void testGetCoordinates() throws Exception {
    System.out.println("getCoordinates");
    String objectName = "m31";
    AbstractNameResolver cds = new CDSNameResolver(objectName, CDSNameResolver.NameResolverService.all);
    AbstractNameResolver imcce = new IMCCESsoResolver(objectName, "now");
    AbstractNameResolver corot = new CorotIdResolver(objectName);
    cds.setNext(imcce);
    imcce.setNext(corot);
    NameResolverResponse response = cds.getResponse();
    List<fr.cnes.sitools.extensions.common.AstroCoordinate> coords = response.getAstroCoordinates();
    assertNotNull(coords);
    fr.cnes.sitools.extensions.common.AstroCoordinate coordinate = coords.get(0);
    assertEquals("RA is not correct for m31", coordinate.getRaAsSexagesimal(), "00:42:44.330");
    assertEquals("DEC is not correct for m31", coordinate.getDecAsSexagesimal(), "+41:16:07.50");
    assertEquals("RA is not correct for m31", 10.6847083, coordinate.getRaAsDecimal(), 0.0001);
    assertEquals("DEC is not correct for m31", 41.26875, coordinate.getDecAsDecimal(), 0.0001);
  }

  /**
   * Test of getCreditsName method, of class AbstractNameResolver.
   */
  @Test
  public void testGetCreditsName() {
    System.out.println("getCreditsName");
    String objectName = "m31";
    AbstractNameResolver cds = new CDSNameResolver(objectName, CDSNameResolver.NameResolverService.all);
    AbstractNameResolver imcce = new IMCCESsoResolver(objectName, "now");
    AbstractNameResolver corot = new CorotIdResolver(objectName);
    cds.setNext(imcce);
    imcce.setNext(corot);
    NameResolverResponse response = cds.getResponse();
    assertEquals("CDS", response.getCredits());
  }
}
