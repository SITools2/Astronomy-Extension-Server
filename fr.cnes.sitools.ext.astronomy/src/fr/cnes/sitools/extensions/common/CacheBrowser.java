/******************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.extensions.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.restlet.data.CacheDirective;
import org.restlet.representation.Representation;

/**
 * Creates a CacheDirective for browsers.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CacheBrowser {

  /**
   * Representation to cache.
   */
  private Representation rep;

  /**
   * Cache directives.
   */
  private List<CacheDirective> cacheDirectivesBrowser;
  
  /**
   * Constructor.
   * @param repVal representation to cache  
   */
  protected CacheBrowser(final Representation repVal) {
    this.rep = repVal;
    this.cacheDirectivesBrowser = null;
  }

  /**
   * Returns the right cache directive according to the choice of the cache.
   * @param cacheDirective choice of the cache
   * @param repVal representation to cache
   * @return the right cache directive according to the choice of the cache
   */
  public static CacheBrowser createCache(final CacheDirectiveBrowser cacheDirective, final Representation repVal) {
    CacheBrowser cache = new CacheBrowser(repVal);
    List<CacheDirective> cacheDirectives;
    switch(cacheDirective) {
      case FOREVER:
        cache.createCacheForEver();
        break;
      case NO_CACHE:
        cache.createNoCache();
        break;
      case DAILY:
        cache.createCacheDay();
        break;
      default:
        throw new IllegalArgumentException("Cache directive not supported");
    }
    return cache;
  }

  /**
   * Returns the representation.
   * @return the representation
   */
  public final Representation getRepresentation() {
    return this.rep;
  }
  
  /**
   * Returns the cache directives.
   * @return the cache directives
   */
  public final List<CacheDirective> getCacheDirectives() {
    return this.cacheDirectivesBrowser;
  }

  /**
   * Possible choices of the cache.
   */
  public enum CacheDirectiveBrowser {
    /**
     * Cache is enabled forever.
     */
    FOREVER,
    /**
     * No cache.
     */
    NO_CACHE,
    /**
     * Refresh the cache each day.
     */
    DAILY
  }

  /**
   * Creates the cache directive for the "forever" choice.
   */
  private void createCacheForEver() {
    List<CacheDirective> cacheDirectives = new ArrayList<CacheDirective>();
    cacheDirectives.add(CacheDirective.publicInfo());
    this.cacheDirectivesBrowser = cacheDirectives;
  }

  /**
   * Creates the cache directive for the "nocache" choice.
   */  
  private void createNoCache() {
    List<CacheDirective> cacheDirectives = new ArrayList<CacheDirective>();
    cacheDirectives.add(CacheDirective.noCache());
    this.cacheDirectivesBrowser = cacheDirectives;
  }

  /**
   * Creates the cache directive for the "cacheday" choice.
   */  
  private void createCacheDay() {
    final int numberOfHoursInDay = 24;
    Calendar expiresOn = Calendar.getInstance();
    long age = expiresOn.getTimeInMillis();
    expiresOn.add(Calendar.HOUR_OF_DAY, numberOfHoursInDay);
    rep.setExpirationDate(expiresOn.getTime());
    CacheDirective maxAge = CacheDirective.maxAge((int) (expiresOn.getTimeInMillis() - age));
    List<CacheDirective> cacheDirectives = new ArrayList<CacheDirective>();
    cacheDirectives.add(maxAge);
    this.cacheDirectivesBrowser =  cacheDirectives;
  }  
}
