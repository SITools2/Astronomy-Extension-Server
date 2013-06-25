/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.cache;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ehcache.CacheManager;
import org.restlet.data.LocalReference;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Singleton for server cache based on Healpix.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SingletonCacheHealpixDataAccess {
  
  /**
   * Cache configuration.
   */
  private final transient Representation cacheConf = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/ehcache.xml").get();

  /**
   * Constructs a cache.
   */
  protected SingletonCacheHealpixDataAccess() {    
    try {
      CacheManager.create(cacheConf.getStream());
    } catch (IOException ex) {
      Logger.getLogger(SingletonCacheHealpixDataAccess.class.getName()).log(Level.SEVERE, null, ex);
    }    
  }
  
  /**
   * Creates the cache.
   */
  public static void create() {
    final SingletonCacheHealpixDataAccess singletonCacheHealpixDataAccess = new SingletonCacheHealpixDataAccess();
  }
  
  /**
   * Returns the cache instance.
   * @return the cache instance
   */
  public static CacheManager getInstance() {
    return CacheManager.getInstance();
  }
  
  /**
   * Creates unique identifier for Healpix application.
   * @param applicationId application ID
   * @param order order
   * @param healpixNumber healpix number
   * @return the unique identifier
   */
  public static String generateId(final String applicationId, final String order, final String healpixNumber) {
    return String.format("%s#%s#%s", applicationId, order, healpixNumber);
  }
  
}
