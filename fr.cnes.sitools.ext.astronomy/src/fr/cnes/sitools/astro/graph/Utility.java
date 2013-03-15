/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     * Creatse a PNG representation of the graph.
     * @param graph graph component
     * @param file output filename
     * @param width number of pixels along X axis
     * @param height number of pixels along Y axis
     * @return True when the creation is a success otherwise false
     */
    public static boolean createPNG(final Graph graph, final File file, final int width, final int height) {
        boolean isCreated = false;
        graph.setPixelHeight(height);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        graph.paint(g);
        g.dispose();
        try {
            isCreated = ImageIO.write(bi, "png", file);
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isCreated;
    }
    
    /**
     * Creates a PNG representation of the graph.
     * @param graph graph component
     * @param out 
     * @param width number of pixels along X axis
     * @param height number of pixels along Y axis
     * @return True when the creation is a success otherwise false
     */
    public static boolean createPNG(final Graph graph, final OutputStream out, final int width, final int height) {
        boolean isCreated = false;
        graph.setPixelHeight(height);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        graph.paint(g);
        g.dispose();
        try {
            isCreated = ImageIO.write(bi, "png", out);
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isCreated;
    }    

    /**
     * Create a JFrame.
     * @param graph graph component
     * @param width number of pixels along X axis
     * @param height number of pixels along Y axis
     */
    public static void createJFrame(final Graph graph, final int width, final int height) {
        JFrame f = new JFrame("");
        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                System.exit(0);
            }
        });
        graph.setPixelHeight(height);
        graph.init();
        f.getContentPane().add("Center", graph);
        f.setSize(new Dimension(width, height));
        f.pack();
        f.setVisible(true);
    }

    /**
     * Create a Healpix Map from a CSV file. The number of the line in the CSV file is the Healpix pixel number.
     * The first column is the value of the Healpix pixel number.
     * @param input Input stream
     * @param nside nside
     * @param scheme RING ou NESTED
     * @return Returns a Healpix Map
     * @throws Exception 
     */
    public static HealpixMapDouble createHealpixMapFromCSVFile(final InputStream input, final int nside, final Scheme scheme) throws Exception {
        HealpixMapDouble healpixMapDouble = new HealpixMapDouble(nside, Scheme.NESTED);
        BufferedInputStream bis = new BufferedInputStream(input);
        DataInputStream dis = new DataInputStream(bis);
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
        HealpixMapDouble healpixMapDouble = new HealpixMapDouble(nside, Scheme.NESTED);
        //create BufferedReader to read csv file
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String strLine;
        StringTokenizer st;
        int lineNumber = 0, tokenNumber = 0;

        //read comma separated file line by line
        while ((strLine = br.readLine()) != null) {
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
        return healpixMapDouble;
    }

    /**
     * Empty constructor.
     */
    private Utility() {
    }
}
