//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class TransferableList
//
//--- Description -------------------------------------------------------------
//	An ArrayList that implements the Transferable interface.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	05/24/99	J. Jones / 588
//
//		Original implementation.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

//package GOV.nasa.gsfc.sea.util;

package jsky.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An ArrayList that implements the Transferable interface.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		05/24/99
 * @author		J. Jones / 588
 **/
public class TransferableList extends ArrayList implements Transferable {

    /**
     * Creates an empty list.
     **/
    public TransferableList() {
        super();
    }

    /**
     * Creates a list with the specified initial capacity.
     **/
    public TransferableList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Returns the set of supported DataFlavors for the Transferables in the list.
     * Iterates through the Transferables and adds each Transferable's DataFlavors
     * to the set.  The DataFlavors returned by this method are supported by at
     * least one item within the TransferableList, but not necessarily all items.
     *
     * @return	set of possible DataFlavors
     **/
    public DataFlavor[] getTransferDataFlavors() {
        Set<DataFlavor> flavors = new HashSet<DataFlavor>();

        // Construct set of all flavors available for all elements in list
        for (int i = 0; i < size(); ++i) {
            Transferable obj = (Transferable) get(i);
            DataFlavor[] objFlavs = obj.getTransferDataFlavors();
            flavors.addAll(Arrays.asList(objFlavs));
        }

        // Convert to DataFlavor array
        DataFlavor[] flavArray = new DataFlavor[flavors.size()];
        int i = 0;
        for (Object flavor : flavors) {
            flavArray[i++] = (DataFlavor) flavor;
        }

        return flavArray;
    }

    /**
     * Returns true if the DataFlavor is supported by at least one item in the
     * TransferableList.
     *
     * @param	flavor	DataFlavor to test
     * @return			true if DataFlavor is supported
     **/
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        // Compare flavor to set returned by getTransferDataFlavors()
        boolean supported = false;
        DataFlavor[] flavors = getTransferDataFlavors();
        for (DataFlavor f : flavors) {
            if (flavor.equals(f)) {
                supported = true;
                break;
            }
        }
        return supported;
    }

    /**
     * Returns an array of Objects that contains the transfer data for all
     * Transferables within the TransferableList that support the specified
     * DataFlavor.
     *
     * @param	flavor	extract data for this DataFlavor
     * @return			array of objects that match the DataFlavor
     * @exception	UnsupportedFlavorException	thrown if none of the elements
     *				in the list support the flavor
     **/
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        // Create list of TransferData objects
        List<Object> data = new ArrayList<Object>();

        // Add each element's TransferData where the element supports the flavor
        for (int i = 0; i < size(); ++i) {
            Transferable obj = (Transferable) get(i);
            if (obj.isDataFlavorSupported(flavor)) {
                data.add(obj.getTransferData(flavor));
            }
        }

        // If none of the elements support the flavor, throw exception
        if (data.size() == 0) {
            throw new UnsupportedFlavorException(flavor);
        }

        // Convert the list to an array of Objects
        return data.toArray();
    }
}
