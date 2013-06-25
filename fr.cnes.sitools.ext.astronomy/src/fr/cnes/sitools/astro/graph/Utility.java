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
package fr.cnes.sitools.astro.graph;

import healpix.essentials.HealpixMapDouble;
import healpix.essentials.Scheme;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * Utility class to create PNG or JFrame representation.
 * @author Jean-Christophe Malapert
 */
public final class Utility {

    /**
     * Creatse a PNG representation of the graph by giving the graph to plot,
     * the output filename and the height of the plot in pixels.
     * <p>
     * The width is automatically computed to keep the ratio of the projection.
     * </p>
     * @param graph graph component to plot
     * @param file output filename    
     * @param height number of pixels along Y axis
     * @return True when the creation is a success otherwise false
     */
    public static boolean createPNG(final Graph graph, final File file, final int height) {
        boolean isCreated;
        graph.setPixelHeight(height);        
        graph.setupRatioImageSize();
        final BufferedImage bufferImage = new BufferedImage(graph.getPixelWidth(), graph.getPixelHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics graphic = bufferImage.createGraphics();
        graph.paint(graphic);
        graphic.dispose();
        try {
            isCreated = ImageIO.write(bufferImage, "png", file);
        } catch (IOException ex) {
            isCreated = false;
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isCreated;
    }
    
    /**
     * Creates a PNG representation of the graph by giving the graph to plot, 
     * the output stream and the height in pixels.
     * <p>
     * The width is automatically computed to keep the ratio of the projection.
     * </p>     
     * @param graph graph component to plot
     * @param out output stream
     * @param height number of pixels along Y axis
     * @return True when the creation is a success otherwise false
     */
    public static boolean createPNG(final Graph graph, final OutputStream out, final int height) {
        boolean isCreated;
        graph.setPixelHeight(height);      
        graph.setupRatioImageSize();
        final BufferedImage bufferedImage = new BufferedImage(graph.getPixelWidth(), graph.getPixelHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics graphic = bufferedImage.createGraphics();
        graph.paint(graphic);
        graphic.dispose();
        try {
            isCreated = ImageIO.write(bufferedImage, "png", out);
        } catch (IOException ex) {
            isCreated = false;
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isCreated;
    }    

    /**
     * Create a JFrame bu giving the graph to plot and the height in pixels.
     * @param graph graph component    
     * @param height number of pixels along Y axis
     */
    public static void createJFrame(final Graph graph, final int height) {
        final JFrame frame = new JFrame("");
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent event) {
                System.exit(0);
            }
        });
        graph.setPixelHeight(height);
        graph.setupRatioImageSize();
        graph.init();
        frame.getContentPane().add("Center", graph);
        frame.setSize(new Dimension(graph.getPixelWidth(), graph.getPixelHeight()));
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Create a Healpix Map from a CSV file. The number of the line in the CSV file is the Healpix pixel number.
     * The first column is the value of the Healpix pixel number.
     * @param input Input stream
     * @param nside nside
     * @param scheme RING ou NESTED
     * @return Returns a Healpix Map
     * @throws GraphException Exception
     */
    public static HealpixMapDouble createHealpixMapFromCSVFile(final InputStream input, final int nside, final Scheme scheme) throws GraphException {
        try {
            final HealpixMapDouble healpixMapDouble = new HealpixMapDouble(nside, Scheme.NESTED);
            final BufferedInputStream bis = new BufferedInputStream(input);
            final DataInputStream dis = new DataInputStream(bis);
            int lineNumber = 0, tokenNumber = 0;
            String strLine;
            StringTokenizer st;
            while (dis.available() != 0) {
                strLine = dis.readLine();
                lineNumber++;
                st = new StringTokenizer(strLine, ",");
                while (st.hasMoreTokens()) {
                    //display csv values
                    tokenNumber++;
                    if (tokenNumber == 1) {
                        String text = st.nextToken();
                        double density = Double.valueOf(text);
                        healpixMapDouble.setPixel(lineNumber - 1, density);
                    } else {
                        st.nextToken();
                    }
                }
                //reset token number
                tokenNumber = 0;
            }
            dis.close();
            bis.close();
            return healpixMapDouble;
        } catch (Exception ex) {
            throw new GraphException(ex);
        }
    }

    /**
     * Create a Healpix Map from a CSV file. The number of the line in the CSV file is the Healpix pixel number.
     * The first column is the value of the Healpix pixel number.
     * @param filename file name
     * @param nside nside
     * @param scheme RING ou NESTED
     * @return Returns a Healpix Map
     * @throws Exception 
     */    
    public static HealpixMapDouble createHealpixMapFromCSVFile(final String filename, final int nside, final Scheme scheme) throws Exception {
        final HealpixMapDouble healpixMapDouble = new HealpixMapDouble(nside, Scheme.NESTED);
        //create BufferedReader to read csv file
        final BufferedReader bufferReader = new BufferedReader(new FileReader(filename));
        String strLine;
        StringTokenizer st;
        int lineNumber = 0, tokenNumber = 0;

        //read comma separated file line by line
        while ((strLine = bufferReader.readLine()) != null) {
            lineNumber++;

            //break comma separated line using ","
            st = new StringTokenizer(strLine, ",");

            while (st.hasMoreTokens()) {
                //display csv values
                tokenNumber++;
                if (tokenNumber == 1) {
                    String text = st.nextToken();
                    double density = Double.valueOf(text);
                    healpixMapDouble.setPixel(lineNumber - 1, density);
                } else {
                    st.nextToken();
                }
            }

            //reset token number
            tokenNumber = 0;
        }
        bufferReader.close();
        return healpixMapDouble;
    }

    /**
     * Empty constructor.
     */
    private Utility() {
    }
}
