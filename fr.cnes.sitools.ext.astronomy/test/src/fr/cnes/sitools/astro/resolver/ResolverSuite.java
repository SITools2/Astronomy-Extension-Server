/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.resolver;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({fr.cnes.sitools.astro.resolver.AstroCoordinateTest.class, fr.cnes.sitools.astro.resolver.CorotIdResolverTest.class, fr.cnes.sitools.astro.resolver.CDSResolverTest.class, fr.cnes.sitools.astro.resolver.IMCCEResolverTest.class, fr.cnes.sitools.astro.resolver.NameResolverTest.class, fr.cnes.sitools.astro.resolver.ReverseNameResolverTest.class})

public class ResolverSuite {
    
}
