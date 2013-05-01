/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.vo.sia;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SqlGeometryFactory {
  
  public static SqlGeometryConstraint create(final String geometryMode) {
    if (geometryMode.equals("OVERLAPS")) {
      return new OverlapsModeIntersection();
    } else {
      return new CenterModeIntersection();
    }
  }
}
