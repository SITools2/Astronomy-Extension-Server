/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.cache;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.restlet.data.LocalReference;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SingletonCacheHealpixDataAccess {
  
  private Representation cacheConf = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/ehcache.xml").get();

  protected SingletonCacheHealpixDataAccess() {    
    try {
      CacheManager.create(cacheConf.getStream());
    } catch (IOException ex) {
      Logger.getLogger(SingletonCacheHealpixDataAccess.class.getName()).log(Level.SEVERE, null, ex);
    }    
  }
  
  public static void create() {
    SingletonCacheHealpixDataAccess singletonCacheHealpixDataAccess = new SingletonCacheHealpixDataAccess();
  }
  
  public static CacheManager getInstance() {
    return CacheManager.getInstance();
  }
  
  public static String generateId(String applicationId, String order, String healpixNumber) {
    return String.format("%s#%s#%s", applicationId, order, healpixNumber);
  }
  
}
