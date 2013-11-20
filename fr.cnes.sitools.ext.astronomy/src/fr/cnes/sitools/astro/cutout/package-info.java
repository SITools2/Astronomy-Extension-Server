/**
This package contains classes related to images cutOut.<br/>

fr.cnes.sitools.astro.cutOut is a Java package aiming to extract some parts from a FITS file.
<H2>List of available cutout</H2>
Three different cutouts are implemented:
<ul>
  <li>CutOutCDS: Provides a cutOut from CDS as PNG</li>
  <li>CutOutSITools2 : Provides a FITS and JPEG cutout from a FITS file</li>
  <li>HealpixMap : Provides a FITS cutout from a FITS binary table</li>
</ul>
<img src="../../../../../images/Cutout.png"/>
<br/><br/><br/>
<h3>CutOutCDS</h3>
This class uses a web service, which is provided by CDS.
<pre>
CutOutInterface cutout = new CutOutCDS(199.8766625, -12.7409111);
cutout.createCutoutFits(new FileOutputStream(new File("/tmp/test.fits")));
</pre>

<h3>CutOutSITools2</h3>
This class can provide a FITS or a JPEG file as output. For the JPEG file,
the zscale algorithm is applied to choose the best dynamic of the image.
<pre>
double rightAscension = 199.8766625;
double declination = -12.7409111;
double radius = 5;
Fits fits = new Fits("/tmp/test.fits");
CutOutInterface cutout = new CutOutSITools2(fits, rightAscension, declination, radius);
cutout.createCutoutFits(new FileOutputStream(new File("/tmp/test.fits")));
</pre>

<h3>HealpixMap</h3>
<pre>
File file = new File("/tmp/myData.fits");
double arsecPerPixelAlongX = 30;
double arsecPerPixelAlongY = 30;
double[] shape = new double[]{265.6, -28.43, 257.11, -23.45, 262.96, -15.23, 271.3, -20.10};
double rotation = 0;
CutOutInterface cutout = new HealpixMap(arsecPerPixelAlongX, arsecPerPixelAlongX, shape, rotation, file, AstroCoordinate.CoordinateSystem.EQUATORIAL);
cutout.createCutoutFits(new FileOutputStream(new File("/tmp/test.fits")));
</pre>
@copyright 2010-2013 CNES
@author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
@startuml Cutout.png
interface CutOutInterface
CutOutInterface <|-- CutOutCDS
CutOutInterface <|-- CutOutSITools2
CutOutInterface <|-- HealpixMap
HealpixMap *-- WcsComputation
CutOutSITools2 *-- FitsHeader
CutOutSITools2 *-- FitsBufferedImage
AnimatedGifEncoder *-- LZWEncoder
AnimatedGifEncoder *-- NeuQuant
class CutOutException
@enduml
*/
package fr.cnes.sitools.astro.cutout;
