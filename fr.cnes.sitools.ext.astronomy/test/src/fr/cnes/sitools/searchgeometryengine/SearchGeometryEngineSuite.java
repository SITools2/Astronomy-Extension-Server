/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
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
 */
package fr.cnes.sitools.searchgeometryengine;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author malapert
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({fr.cnes.sitools.searchgeometryengine.CoordSystemTest.class, fr.cnes.sitools.searchgeometryengine.RingIndexTest.class, fr.cnes.sitools.searchgeometryengine.PointTest.class, fr.cnes.sitools.searchgeometryengine.NestedIndexTest.class, fr.cnes.sitools.searchgeometryengine.PolygonTest.class})
public class SearchGeometryEngineSuite {
    
}
