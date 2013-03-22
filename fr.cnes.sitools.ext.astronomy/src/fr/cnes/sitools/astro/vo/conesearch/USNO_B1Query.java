/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.vo.conesearch;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;

/**
 *
 * @author malapert
 */
public class USNO_B1Query implements ConeSearchQueryInterface{
    
    private ConeSearchQuery query;
    private double ra;
    private double dec;
    private double radius;
    private static final String url = "http://vizier.u-strasbg.fr/viz-bin/votable/-A?-source=I/284&";
    
    public USNO_B1Query(double ra, double dec, double radius) {
        this.query = new ConeSearchQuery(url);
        this.ra = ra;
        this.dec = dec;
        this.radius = radius;
    }        
    
    public USNO_B1Query(long healpix, int order) throws Exception {
        int nside = (int) Math.pow(2, order);
        double pixRes = HealpixIndex.getPixRes(nside);
        HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
        Pointing pointing = healpixIndex.pix2ang(healpix);
        this.ra = Math.toDegrees(pointing.phi);
        this.dec = 90.0 - Math.toDegrees(pointing.theta);
        this.radius = pixRes/2.0;        
    }
    
    @Override
    public GeoJsonRepresentation getResponse() throws Exception {
        Map dataModel = new HashMap();       
        int totalResults = 0;
        try {
            List features = new ArrayList();           
            List<Map<Field,String>> response = query.getResponseAt(ra,dec,radius);            
            for(Map<Field,String> record:response) {
                Map feature = new HashMap();
                Map geometry = new HashMap();
                Map properties = new HashMap();
                double ra_tmp=500;
                double dec_tmp=500;
                String identifier = null;
                Set<Field> fields = record.keySet();
                Iterator<Field> iter = fields.iterator();
                while(iter.hasNext()) {
                    Field field = iter.next();                
                    String ucd = field.getUcd();
                    if("POS_EQ_RA_MAIN".equals(ucd)) {
                        ra_tmp = Double.valueOf(record.get(field));
                    } else if("POS_EQ_DEC_MAIN".equals(ucd)) {
                        dec_tmp = Double.valueOf(record.get(field));
                    } else if("ID_MAIN".equals(ucd)) {
                        identifier = record.get(field);
                    } else {
                        properties.put(field.getName(), record.get(field));
                    }
                }
                properties.put("identifier", identifier);
                geometry.put("coordinates", String.format("[%s,%s]", ra_tmp, dec_tmp));
                geometry.put("type", "Point");   
                geometry.put("crs", "equatorial.ICRS");
                feature.put("geometry", geometry);
                feature.put("properties", properties);
                features.add(feature);          
                totalResults++;
            }
            dataModel.put("features", features);
            dataModel.put("totalResults", totalResults);
            return new GeoJsonRepresentation(dataModel);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
        
    }
    private static final Logger LOG = Logger.getLogger(USNO_B1Query.class.getName());
    
    
}
