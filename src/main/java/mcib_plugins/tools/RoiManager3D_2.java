/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mcib_plugins.tools;

import customnode.CustomTriangleMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.YesNoCancelDialog;
import ij.io.OpenDialog;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.Recorder;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Scrollbar;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.media.j3d.View;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DSurface;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Point3D;
import mcib3d.geom.Vector3D;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.Segment3DSpots;
import mcib3d.utils.AboutMCIB;
import mcib3d.utils.CheckInstall;

/**
 *
 * @author thomas
 */
public class RoiManager3D_2 extends JFrame implements PlugIn, MouseWheelListener, AdjustmentListener, MacroExtension, UniverseListener, DropTargetListener, WindowListener {

    protected Objects3DPopulation objects3D;
    protected DefaultListModel model = new DefaultListModel();
    private HashMap<String, Integer> hashNames;
    private ImagePlus currentImage;
    private Roi[] arrayRois = null;
    boolean canceled;
    boolean live = true;

    private ResultsFrame tableResultsMeasure = null;
    private ResultsFrame tableResultsQuantif = null;
    private ResultsFrame tableResultsColoc = null;
    private ResultsFrame tableResultsDistance = null;
    private ResultsFrame tableResultsVoxels = null;

    private double version = mcib3d.utils.AboutMCIB.getVERSION();
    private boolean multi = false;
    private Image3DUniverse universe = null;
    private boolean showUniverse = true;
    private int currentZmin = 0;
    private int currentZmax = 0;

    /**
     * Creates new form RoiManager3D_2
     */
    public RoiManager3D_2() {
        if (!CheckInstall.installComplete()) {
            IJ.log("Not starting RoiManager3D");
            return;
        }
        IJ.log("Starting RoiManager3D");
        initComponents();
        this.setTitle("RoiManager3D " + version);
        list.setModel(model);
        setVisible(true);
        objects3D = new Objects3DPopulation();
        hashNames = new HashMap();
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // window
        addWindowListener(this);

        if (IJ.macroRunning()) {
            Functions.registerExtensions(this);
            live = false;
            buttonLiveRoi.setSelected(false);
        }

        if (Recorder.record) {
            //Recorder.record("Ext.install", "RoiManager3D_");
        }

        this.getUniverse();

        universe.addUniverseListener(this);

        new DropTarget(list, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /**
     * Gets the extensionFunctions attribute of the RoiManager3D_ object
     *
     * @return The extensionFunctions value
     */
    @Override
    public ExtensionDescriptor[] getExtensionFunctions() {
        int[] argSeg = {ARG_NUMBER, ARG_NUMBER};
        int[] argCol = {ARG_NUMBER, ARG_NUMBER, ARG_NUMBER};
        int[] argViewer = {ARG_NUMBER, ARG_NUMBER, ARG_NUMBER};
        int[] argSel = {ARG_NUMBER};
        int[] argName = {ARG_NUMBER, ARG_OUTPUT + ARG_STRING};
        int[] argRename = {ARG_STRING};
        int[] argSaveResult = {ARG_STRING, ARG_STRING};
        int[] argCount = {ARG_OUTPUT + ARG_NUMBER};
        //int[] argDist = {ARG_NUMBER, ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER};
        int[] argDist2 = {ARG_NUMBER, ARG_NUMBER, ARG_STRING, ARG_OUTPUT + ARG_NUMBER};
        int[] argColoc = {ARG_NUMBER, ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER};
        int[] argMeasure3D = {ARG_NUMBER, ARG_STRING, ARG_OUTPUT + ARG_NUMBER};
        int[] argCentroid3D = {ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER};
        int[] argBorder3D = {ARG_NUMBER, ARG_NUMBER, ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER};
        int[] argBounding3D = {ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER, ARG_OUTPUT + ARG_NUMBER};
        int[] argClosestK = {ARG_NUMBER, ARG_NUMBER, ARG_STRING, ARG_OUTPUT + ARG_NUMBER};

        ExtensionDescriptor[] extensions = {
            ExtensionDescriptor.newDescriptor("Manager3D_Close", this),
            ExtensionDescriptor.newDescriptor("Manager3D_AddImage", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Delete", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Reset", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Erase", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Rename", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_Merge", this),
            ExtensionDescriptor.newDescriptor("Manager3D_FillStack", this, argCol),
            ExtensionDescriptor.newDescriptor("Manager3D_Fill3DViewer", this, argViewer),
            ExtensionDescriptor.newDescriptor("Manager3D_Split", this, argCount),
            ExtensionDescriptor.newDescriptor("Manager3D_Measure", this),
            ExtensionDescriptor.newDescriptor("Manager3D_List", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Quantif", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Distance", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Coloc", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Angle", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Select", this, argSel),
            ExtensionDescriptor.newDescriptor("Manager3D_SelectAll", this),
            ExtensionDescriptor.newDescriptor("Manager3D_GetName", this, argName),
            ExtensionDescriptor.newDescriptor("Manager3D_SelectFor", this, argCol),
            ExtensionDescriptor.newDescriptor("Manager3D_MultiSelect", this),
            ExtensionDescriptor.newDescriptor("Manager3D_MonoSelect", this),
            //ExtensionDescriptor.newDescriptor("Manager3D_3DViewerSelect", this),
            ExtensionDescriptor.newDescriptor("Manager3D_DeselectAll", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Count", this, argCount),
            ExtensionDescriptor.newDescriptor("Manager3D_Segment", this, argSeg),
            ExtensionDescriptor.newDescriptor("Manager3D_ShowRoi", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Dist2", this, argDist2),
            ExtensionDescriptor.newDescriptor("Manager3D_Coloc2", this, argColoc),
            ExtensionDescriptor.newDescriptor("Manager3D_Measure3D", this, argMeasure3D),
            ExtensionDescriptor.newDescriptor("Manager3D_Quantif3D", this, argMeasure3D),
            ExtensionDescriptor.newDescriptor("Manager3D_RadiusBorderVoxel", this, argBorder3D),
            ExtensionDescriptor.newDescriptor("Manager3D_Centroid3D", this, argCentroid3D),
            ExtensionDescriptor.newDescriptor("Manager3D_MassCenter3D", this, argCentroid3D),
            ExtensionDescriptor.newDescriptor("Manager3D_Label", this),
            ExtensionDescriptor.newDescriptor("Manager3D_Load", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_Save", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_SaveMeasure", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_SaveQuantif", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_Bounding3D", this, argBounding3D),
            ExtensionDescriptor.newDescriptor("Manager3D_Closest", this, argMeasure3D),
            ExtensionDescriptor.newDescriptor("Manager3D_ClosestK", this, argClosestK),
            // new 19/06/2013
            ExtensionDescriptor.newDescriptor("Manager3D_Feret1", this, argCentroid3D),
            ExtensionDescriptor.newDescriptor("Manager3D_Feret2", this, argCentroid3D),
            ExtensionDescriptor.newDescriptor("Manager3D_BorderVoxel", this, argColoc),
            // close results windows
            ExtensionDescriptor.newDescriptor("Manager3D_CloseResult", this, argRename),
            ExtensionDescriptor.newDescriptor("Manager3D_SaveResult", this, argSaveResult),
            // test transform universe
            ExtensionDescriptor.newDescriptor("Manager3D_Rotate", this, argCol),
            ExtensionDescriptor.newDescriptor("Manager3D_LoadView3D", this, argRename),};
        return extensions;
    }

    /**
     * Description of the Method
     *
     * @param name Description of the Parameter
     * @param args Description of the Parameter
     * @return Description of the Return Value
     */
    @Override
    public String handleExtension(String name, Object[] args) {
        if (name.equals("Manager3D_Close")) {
            dispose();
        } else if (name.equals("Manager3D_AddImage")) {
            addImage();
        } else if (name.equals("Manager3D_Delete")) {
            delete(false);
        } else if (name.equals("Manager3D_LiveRoi")) {
            live = !live;
            if (live) {
                this.computeRois();
                this.updateRois();
            }
        } else if (name.equals("Manager3D_Reset")) {
            reset();
        } else if (name.equals("Manager3D_Erase")) {
            delete(true);
        } else if (name.equals("Manager3D_Rename")) {
            String S = (String) args[0];
            rename(S);
        } else if (name.equals("Manager3D_FillStack")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int b = D.intValue();
            D = (Double) args[2];
            int c = D.intValue();
            fill3D(a, b, c);
        } else if (name.equals("Manager3D_Fill3DViewer")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int b = D.intValue();
            D = (Double) args[2];
            int c = D.intValue();
            //D = (Double) args[3];
            //double s = D;
            fill3DViewer(a, b, c);
        } else if (name.equals("Manager3D_Split")) {
            if (split()) {
                ((Double[]) args[0])[0] = new Double(1);
            } else {
                ((Double[]) args[0])[0] = new Double(0);
            }
        } else if (name.equals("Manager3D_Merge")) {
            merge();
        } else if (name.equals("Manager3D_Measure")) {
            measure3D();
        } else if (name.equals("Manager3D_Quantif")) {
            quantif3D();
        } else if (name.equals("Manager3D_List")) {
            listVoxels();
        } else if (name.equals("Manager3D_Label")) {
            label();
        } else if (name.equals("Manager3D_Distance")) {
            distance();
        } else if (name.equals("Manager3D_Coloc")) {
            coloc();
        } else if (name.equals("Manager3D_Angle")) {
            angle();
        } else if (name.equals("Manager3D_Segment")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int b = D.intValue();
            segmentation3D(a, b);
        } else if (name.equals("Manager3D_Select")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            selectMacroMode(a);
            //select(a, false);
        } else if (name.equals("Manager3D_SelectAll")) {
            selectAll();
        } else if (name.equals("Manager3D_GetName")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            ((String[]) args[1])[0] = (String) model.get(a);
        } else if (name.equals("Manager3D_SelectFor")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int b = D.intValue();
            D = (Double) args[2];
            int c = D.intValue();
            selectFor(a, b, c);
        } else if (name.equals("Manager3D_DeselectAll")) {
            deselect();
        } else if (name.equals("Manager3D_MultiSelect")) {
            multi = true;
            //multiSelect();
        } else if (name.equals("Manager3D_MonoSelect")) {
            multi = false;
            //multiSelect();
        } else if (name.equals("Manager3D_3DViewerSelect")) {
            //selectFrom3DViewer();
        } else if (name.equals("Manager3D_Count")) {
            ((Double[]) args[0])[0] = new Double(model.getSize());
        } else if (name.equals("Manager3D_Dist2")) {
            handleDistance(args);
        } else if (name.equals("Manager3D_Coloc2")) {
            handleColoc(args);
        } else if (name.equals("Manager3D_ShowRoi")) {
            if (arrayRois == null) {
                computeRois();
            }
            updateRois();
        } else if (name.equals("Manager3D_Measure3D")) {
            handleMeasure3D(args);
        } else if (name.equals("Manager3D_Quantif3D")) {
            handleQuantif3D(args);
        } else if (name.equals("Manager3D_Centroid3D")) {
            handleCentroid3D(args);
        } else if (name.equals("Manager3D_MassCenter3D")) {
            handleMassCentroid3D(args);
        } else if (name.equals("Manager3D_RadiusBorderVoxel")) {
            handleRadiusBorderVoxel(args);
        } else if (name.equals("Manager3D_Bounding3D")) {
            handleBounding3D(args);
        } else if (name.equals("Manager3D_Load")) {
            String S = (String) args[0];
            loadObjects(S);
        } else if (name.equals("Manager3D_Save")) {
            String S = (String) args[0];
            saveObjects(S);
        } else if (name.equals("Manager3D_SaveMeasure")) {
            String S = (String) args[0];
            saveResult("M", S);
        } else if (name.equals("Manager3D_SaveQuantif")) {
            String S = (String) args[0];
            saveResult("Q", S);
//            if (tableResultsQuantif != null) {
//                tableResultsQuantif.getModel().writeData(S);
//            }
        } else if (name.equals("Manager3D_Closest")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            String s = (String) args[1];
            double res = (double) closestObject(a, s);
            ((Double[]) args[2])[0] = res;
        } else if (name.equals("Manager3D_ClosestK")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int k = D.intValue();
            String s = (String) args[2];
            double res = (double) kClosestObject(a, k, s);
            ((Double[]) args[3])[0] = res;
        } else if (name.equals("Manager3D_Feret1")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            Object3D ob = objects3D.getObject(a);
            Voxel3D vox = ob.getFeretVoxel1();
            ((Double[]) args[1])[0] = vox.getX();
            ((Double[]) args[2])[0] = vox.getY();
            ((Double[]) args[3])[0] = vox.getZ();
        } else if (name.equals("Manager3D_Feret2")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            Object3D ob = objects3D.getObject(a);
            Voxel3D vox = ob.getFeretVoxel2();
            ((Double[]) args[1])[0] = vox.getX();
            ((Double[]) args[2])[0] = vox.getY();
            ((Double[]) args[3])[0] = vox.getZ();
        } else if (name.equals("Manager3D_BorderVoxel")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            Object3D ob1 = objects3D.getObject(a);
            D = (Double) args[1];
            a = D.intValue();
            Object3D ob2 = objects3D.getObject(a);
            Voxel3D vox = ob1.VoxelsBorderBorder(ob2)[0];
            ((Double[]) args[2])[0] = vox.getX();
            ((Double[]) args[3])[0] = vox.getY();
            ((Double[]) args[4])[0] = vox.getZ();
        } else if (name.equals("Manager3D_Rotate")) {
            Double D = (Double) args[0];
            int a = D.intValue();
            D = (Double) args[1];
            int b = D.intValue();
            D = (Double) args[2];
            int c = D.intValue();
            rotateUniverse(a, b, c);
        } else if (name.equals("Manager3D_LoadView3D")) {
            String S = (String) args[0];
            loadView3D(S);
        } else if (name.equals("Manager3D_CloseResult")) {
            String S = (String) args[0];
            closeResult(S);
        } else if (name.equals("Manager3D_SaveResult")) {
            String S = (String) args[0];
            String file = (String) args[1];
            saveResult(S, file);
        }

        return null;
    }

    public void closing() {
        if (currentImage != null) {
            removeScrollListener(currentImage, this, this);
            currentImage.killRoi();
            currentImage.updateAndDraw();
            currentImage = null;
        }
        dispose();
    }

    ////////////// HANDLE MACROS EXTENSIONS
    void reset() {
        int count = model.getSize();
        if (count > 0) {
            model.removeAllElements();
            objects3D = new Objects3DPopulation();
        }
        list.updateUI();
        if (universe != null) {
            universe.removeAllContents();
        }
    }

    private void selectMacroMode(int index) {
        //IJ.log("select " + multi + " " + index);
        if (multi) {
            if (list.isSelectedIndex(index)) {
                list.removeSelectionInterval(index, index);
            } else {
                list.addSelectionInterval(index, index);
            }
        } else {
            list.clearSelection();
            list.setSelectedIndex(index);
        }
        //IJ.log("nb selected "+list.getSelectedIndices().length);
        list.updateUI();
    }

    public void selectFor(int start, int end, int step) {
        list.clearSelection();
        for (int i = start; i < end; i += step) {
            list.addSelectionInterval(i, i);
        }
        list.updateUI();
    }

    private void handleDistance(Object[] args) {
        Double D = (Double) args[0];
        int a = D.intValue();
        D = (Double) args[1];
        int b = D.intValue();
        String par = (String) args[2];

        double[] res = new double[1];
        distance(a, b, par, res);
        ((Double[]) args[3])[0] = res[0];
    }

    private void distance(int i, int j, String par, double[] dist) {
        Object3D ob1;
        Object3D ob2;

        dist[0] = -1;

        if ((i >= 0) && (i < objects3D.getNbObjects())) {
            ob1 = objects3D.getObject(i);
        } else {
            return;
        }
        if ((j >= 0) && (j < objects3D.getNbObjects())) {
            ob2 = objects3D.getObject(j);
        } else {
            return;
        }

        if (par.equalsIgnoreCase("cc")) {
            dist[0] = ob1.distCenterUnit(ob2);
        } else if (par.equalsIgnoreCase("bb")) {
            dist[0] = ob1.distBorderUnit(ob2);
        } else if (par.equalsIgnoreCase("c1b2")) {
            dist[0] = ob1.distCenterBorderUnit(ob2);
        } else if (par.equalsIgnoreCase("c2b1")) {
            dist[0] = ob2.distCenterBorderUnit(ob1);
        } else if (par.equalsIgnoreCase("r1c2")) {
            dist[0] = ob1.radiusCenter(ob2);
        } else if (par.equalsIgnoreCase("r2c1")) {
            dist[0] = ob2.radiusCenter(ob1);
        } else if (par.equalsIgnoreCase("ex2c1")) {
            dist[0] = ob1.distCenterUnit(ob2) / ob1.radiusCenter(ob2);
        } else if (par.equalsIgnoreCase("ex1c2")) {
            dist[0] = ob2.distCenterUnit(ob1) / ob2.radiusCenter(ob1);
        } // opposite radiuscenter
        else if (par.equalsIgnoreCase("-r1c2")) {
            dist[0] = ob1.radiusCenter(ob2, true);
        } else if (par.equalsIgnoreCase("-r2c1")) {
            dist[0] = ob2.radiusCenter(ob1, true);
        }
    }

    private void handleColoc(Object[] args) {
        Double D = (Double) args[0];
        int a = D.intValue();
        D = (Double) args[1];
        int b = D.intValue();
        double[] res = new double[3];
        coloc(a, b, res);
        ((Double[]) args[2])[0] = res[0];
        ((Double[]) args[3])[0] = res[1];
        ((Double[]) args[4])[0] = res[2];
    }

    private void coloc(int i, int j, double[] coloc) {
        Object3D ob1;
        Object3D ob2;
        //IJ.log("debug "+i+" "+j);
        coloc[0] = -1;
        coloc[1] = -1;
        coloc[2] = -1;

        if ((i >= 0) && (i < objects3D.getNbObjects())) {
            ob1 = objects3D.getObject(i);
        } else {
            return;
        }
        if ((j >= 0) && (j < objects3D.getNbObjects())) {
            ob2 = objects3D.getObject(j);
        } else {
            return;
        }
        //IJ.log("debug "+ob1+" "+ob2);
        coloc[0] = ob1.pcColoc(ob2);
        coloc[1] = ob2.pcColoc(ob1);
        int[] surfc = ob1.surfaceContact(ob2, Prefs.get("RoiManager3D-Options_surfDist.double", 1.0));
        coloc[2] = surfc[0] + surfc[1];
        // debug
        //IJ.log("debug "+coloc[0]+" "+coloc[1]+" "+coloc[2]);
    }

    private void handleMeasure3D(Object[] args) {
        Double D = (Double) args[0];
        int a = D.intValue();
        String s = (String) args[1];
        double res = measure3D(a, s);
        ((Double[]) args[2])[0] = res;
    }

    private double measure3D(int i, String par) {
        double res = Double.NaN;

        Object3D obj;

        if ((i >= 0) && (i < objects3D.getNbObjects())) {
            obj = objects3D.getObject(i);
        } else {
            return res;
        }

        if (par.equalsIgnoreCase("Vol")) {
            res = obj.getVolumeUnit();
        } else if (par.equalsIgnoreCase("NbVox")) {
            res = obj.getVolumePixels();
        } else if (par.equalsIgnoreCase("Surf")) {
            res = obj.getAreaUnit();
        } else if (par.equalsIgnoreCase("Comp")) {
            res = obj.getCompactness();
        } else if (par.equalsIgnoreCase("Spher")) {
            res = obj.getSphericity();
        } else if (par.equalsIgnoreCase("Feret")) {
            res = obj.getFeret();
        } else if (par.equalsIgnoreCase("Elon1")) {
            res = obj.getMainElongation();
        } else if (par.equalsIgnoreCase("Elon2")) {
            res = obj.getMedianElongation();
        } else if (par.equalsIgnoreCase("DCMin")) {
            res = obj.getDistCenterMin();
        } else if (par.equalsIgnoreCase("DCMax")) {
            res = obj.getDistCenterMax();
        } else if (par.equalsIgnoreCase("DCMean")) {
            res = obj.getDistCenterMean();
        } else if (par.equalsIgnoreCase("DCSD")) {
            res = obj.getDistCenterSigma();
        }

        return res;
    }

    private void handleQuantif3D(Object[] args) {
        Double D = (Double) args[0];
        int a = D.intValue();
        String s = (String) args[1];
        double res = quantif3D(a, s);
        ((Double[]) args[2])[0] = res;
    }

    private double quantif3D(int i, String par) {

        ImageHandler ima = getImage3D();

        double res = Double.NaN;
        Object3D obj;

        if ((i >= 0) && (i < objects3D.getNbObjects())) {
            obj = objects3D.getObject(i);
        } else {
            return res;
        }

        if (par.equalsIgnoreCase("IntDen")) {
            res = obj.getIntegratedDensity(ima);
        } else if (par.equalsIgnoreCase("Mean")) {
            res = obj.getPixMeanValue(ima);
        } else if (par.equalsIgnoreCase("Min")) {
            res = obj.getPixMinValue(ima);
        } else if (par.equalsIgnoreCase("Max")) {
            res = obj.getPixMaxValue(ima);
        } else if (par.equalsIgnoreCase("Sigma")) {
            res = obj.getPixStdDevValue(ima);
        }

        return res;
    }

    private void handleCentroid3D(Object[] args) {
        Double D = (Double) args[0];
        int i = D.intValue();
        Object3D obj = objects3D.getObject(i);
        ((Double[]) args[1])[0] = obj.getCenterX();
        ((Double[]) args[2])[0] = obj.getCenterY();
        ((Double[]) args[3])[0] = obj.getCenterZ();
    }

    private void handleMassCentroid3D(Object[] args) {
        ImageHandler ima = getImage3D();

        Double D = (Double) args[0];
        int i = D.intValue();
        Object3D obj = objects3D.getObject(i);
        ((Double[]) args[1])[0] = obj.getMassCenterX(ima);
        ((Double[]) args[2])[0] = obj.getMassCenterY(ima);
        ((Double[]) args[3])[0] = obj.getMassCenterZ(ima);
    }

    private void handleRadiusBorderVoxel(Object[] args) {
        // object1 
        Double D = (Double) args[0];
        int i = D.intValue();
        // object 2
        D = (Double) args[1];
        int j = D.intValue();
        // direction
        D = (Double) args[2];
        int s = D.intValue();

        Object3D obj1 = objects3D.getObject(i);
        Point3D cen1 = obj1.getCenterAsPoint();
        Object3D obj2 = objects3D.getObject(j);
        Point3D cen2 = obj2.getCenterAsPoint();
        // direction
        Vector3D dir;
        if (s == 1) {
            dir = new Vector3D(cen1, cen2);
        } else {
            dir = new Vector3D(cen2, cen1);
        }
        Vector3D Vborder = obj1.vectorPixelBorder(cen1.getX(), cen1.getY(), cen1.getZ(), dir);
        Vector3D border = cen1.getVector3D().add(Vborder);
        ((Double[]) args[3])[0] = border.getX();
        ((Double[]) args[4])[0] = border.getY();
        ((Double[]) args[5])[0] = border.getZ();
    }

    private int closestObject(int a, String Dist) {
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0) {
            indices = getAllIndexes();
        }
        // closest object in selected indices
        Object3D ob1 = objects3D.getObject(a);
        Object3D ob2 = null;
        if (Dist.equalsIgnoreCase("bb")) {
            ob2 = objects3D.closestBorder(ob1, indices);
        } else if (Dist.equalsIgnoreCase("cc")) {
            ob2 = objects3D.closestCenter(ob1, indices, true);
        }

        return objects3D.getIndexOf(ob2);
    }

    private int kClosestObject(int a, int k, String Dist) {
//        int[] indexes = list.getSelectedIndices();
//        if (indexes.length == 0) {
//            indexes = getAllIndexes();
//        }
        // TODO closest object in  selected indices ??
        Object3D ob1 = objects3D.getObject(a);
        Object3D ob2 = null;
        if (Dist.equalsIgnoreCase("bb")) {
            ob2 = objects3D.kClosestBorder(ob1, k);
        } else if (Dist.equalsIgnoreCase("cc")) {
            ob2 = objects3D.kClosestCenter(ob1, k, true);
        }

        return objects3D.getIndexOf(ob2);
    }

    private void handleBounding3D(Object[] args) {
        // object1 
        Double D = (Double) args[0];
        int i = D.intValue();
        Object3D obj = objects3D.getObject(i);
        ((Double[]) args[1])[0] = new Double(obj.getXmin());
        ((Double[]) args[2])[0] = new Double(obj.getXmax());
        ((Double[]) args[3])[0] = new Double(obj.getYmin());
        ((Double[]) args[4])[0] = new Double(obj.getYmax());
        ((Double[]) args[5])[0] = new Double(obj.getZmin());
        ((Double[]) args[6])[0] = new Double(obj.getZmax());
    }

    ////////////// HANDLE MACROS EXTENSIONS (end)
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel = new javax.swing.JPanel();
        jScrollPane = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        buttonSegmentation3D = new javax.swing.JButton();
        buttonAddImage = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonErase = new javax.swing.JButton();
        buttonRename = new javax.swing.JButton();
        buttonSplit = new javax.swing.JButton();
        buttonMerge = new javax.swing.JButton();
        buttonQuantif = new javax.swing.JButton();
        buttonMeasure = new javax.swing.JButton();
        buttonLoad = new javax.swing.JButton();
        buttonColoc = new javax.swing.JButton();
        button3Dviewer = new javax.swing.JButton();
        buttonFillStack = new javax.swing.JButton();
        buttonAngles = new javax.swing.JButton();
        buttonListVoxels = new javax.swing.JButton();
        buttonLabel = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonDistances = new javax.swing.JButton();
        buttonAbout = new javax.swing.JButton();
        buttonLiveRoi = new javax.swing.JToggleButton();
        buttonSelectAll = new javax.swing.JButton();
        buttonDeselect = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JSeparator();
        buttonConfig = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        list.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                listMouseReleased(evt);
            }
        });
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listValueChanged(evt);
            }
        });
        jScrollPane.setViewportView(list);

        buttonSegmentation3D.setText("3D Segmentation");
        buttonSegmentation3D.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSegmentation3DActionPerformed(evt);
            }
        });

        buttonAddImage.setText("Add Image");
        buttonAddImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddImageActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonErase.setText("Erase");
        buttonErase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEraseActionPerformed(evt);
            }
        });

        buttonRename.setText("Rename");
        buttonRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRenameActionPerformed(evt);
            }
        });

        buttonSplit.setText("Split in two");
        buttonSplit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSplitActionPerformed(evt);
            }
        });

        buttonMerge.setText("Merge");
        buttonMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMergeActionPerformed(evt);
            }
        });

        buttonQuantif.setText("Quantif 3D");
        buttonQuantif.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonQuantifActionPerformed(evt);
            }
        });

        buttonMeasure.setText("Measure 3D");
        buttonMeasure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMeasureActionPerformed(evt);
            }
        });

        buttonLoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/fileopen.png")));
        buttonLoad.setToolTipText("Load objects");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        buttonColoc.setText("Colocalisation");
        buttonColoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonColocActionPerformed(evt);
            }
        });

        button3Dviewer.setText("3D Viewer");
        button3Dviewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button3DviewerActionPerformed(evt);
            }
        });

        buttonFillStack.setText("Fill Stack");
        buttonFillStack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFillStackActionPerformed(evt);
            }
        });

        buttonAngles.setText("Angles");
        buttonAngles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAnglesActionPerformed(evt);
            }
        });

        buttonListVoxels.setText("List Voxels");
        buttonListVoxels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonListVoxelsActionPerformed(evt);
            }
        });

        buttonLabel.setText("Label");
        buttonLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLabelActionPerformed(evt);
            }
        });

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/filesave.png")));
        buttonSave.setToolTipText("Save objects");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonDistances.setText("Distances");
        buttonDistances.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDistancesActionPerformed(evt);
            }
        });

        buttonAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_about.png"))); // NOI18N
        buttonAbout.setToolTipText("About");
        buttonAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAboutActionPerformed(evt);
            }
        });

        buttonLiveRoi.setText("Live Roi");
        buttonLiveRoi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLiveRoiActionPerformed(evt);
            }
        });

        buttonSelectAll.setText("Select All");
        buttonSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectAllActionPerformed(evt);
            }
        });

        buttonDeselect.setText("Deselect");
        buttonDeselect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeselectActionPerformed(evt);
            }
        });

        buttonConfig.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/config.png"))); // NOI18N
        buttonConfig.setToolTipText("Settings");
        buttonConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelLayout = new javax.swing.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(jPanelLayout.createSequentialGroup()
                                .addComponent(buttonMerge, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonSplit))
                            .addGroup(jPanelLayout.createSequentialGroup()
                                .addComponent(buttonRename)
                                .addGap(0, 0, 0)
                                .addComponent(buttonDelete)
                                .addGap(0, 0, 0)
                                .addComponent(buttonErase, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jSeparator3)
                            .addGroup(jPanelLayout.createSequentialGroup()
                                .addComponent(buttonSegmentation3D)
                                .addGap(0, 0, 0)
                                .addComponent(buttonAddImage)))
                        .addContainerGap(24, Short.MAX_VALUE))
                    .addGroup(jPanelLayout.createSequentialGroup()
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanelLayout.createSequentialGroup()
                                    .addComponent(buttonDistances)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(buttonAngles))
                                .addComponent(jSeparator4)
                                .addComponent(jSeparator5)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelLayout.createSequentialGroup()
                                    .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(buttonColoc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(buttonMeasure, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(0, 0, 0)
                                    .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(buttonListVoxels, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                                        .addComponent(buttonQuantif, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGroup(jPanelLayout.createSequentialGroup()
                                    .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(buttonSelectAll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(button3Dviewer, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(0, 0, Short.MAX_VALUE)
                                    .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(buttonFillStack)
                                        .addComponent(buttonDeselect)))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelLayout.createSequentialGroup()
                                    .addComponent(buttonLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(buttonSave, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanelLayout.createSequentialGroup()
                                    .addComponent(buttonConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(buttonAbout, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelLayout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addComponent(buttonLiveRoi)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonLabel)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelLayout.createSequentialGroup()
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonSegmentation3D)
                            .addComponent(buttonAddImage))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonRename)
                            .addComponent(buttonDelete)
                            .addComponent(buttonErase))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonMerge)
                            .addComponent(buttonSplit))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonMeasure)
                            .addComponent(buttonQuantif))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonDistances)
                            .addComponent(buttonAngles))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonColoc)
                            .addComponent(buttonListVoxels))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(button3Dviewer)
                            .addComponent(buttonFillStack))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonSelectAll)
                            .addComponent(buttonDeselect))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonLiveRoi)
                            .addComponent(buttonLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonAbout, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonLoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(9, 9, 9))
                    .addComponent(jScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 390, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 402, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonEraseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEraseActionPerformed
        delete(true);
    }//GEN-LAST:event_buttonEraseActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        delete(false);
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        if (!saveObjects()) {
            IJ.log("Could not write RoiSet3D ");
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonSegmentation3DActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSegmentation3DActionPerformed
        segmentation3D();
    }//GEN-LAST:event_buttonSegmentation3DActionPerformed

    private void buttonAddImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddImageActionPerformed
        addImage();
    }//GEN-LAST:event_buttonAddImageActionPerformed

    private void listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listValueChanged
        if (live) {
            this.computeRois();
        }
    }//GEN-LAST:event_listValueChanged

    private void buttonRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRenameActionPerformed
        rename(null);
    }//GEN-LAST:event_buttonRenameActionPerformed

    private void buttonMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMergeActionPerformed
        if (list.getSelectedIndices().length < 2) {
            IJ.showMessage("Needs at least 2 selected objects");
        } else {
            merge();
        }
    }//GEN-LAST:event_buttonMergeActionPerformed

    private void buttonSplitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSplitActionPerformed
        split();
    }//GEN-LAST:event_buttonSplitActionPerformed

    private void buttonMeasureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMeasureActionPerformed
        measure3D();
    }//GEN-LAST:event_buttonMeasureActionPerformed

    private void buttonQuantifActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonQuantifActionPerformed
        quantif3D();
    }//GEN-LAST:event_buttonQuantifActionPerformed

    private void buttonDistancesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDistancesActionPerformed
        if (list.getSelectedIndices().length == 1) {
            IJ.showMessage("Needs at least 2 selected objects");
        } else {
            distance();
        }
    }//GEN-LAST:event_buttonDistancesActionPerformed

    private void buttonAnglesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAnglesActionPerformed
        if (list.getSelectedIndices().length != 3) {
            IJ.showMessage("Needs 3 objects selected");
        } else {
            angle();
        }
    }//GEN-LAST:event_buttonAnglesActionPerformed

    private void buttonColocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonColocActionPerformed
        if (list.getSelectedIndices().length == 1) {
            IJ.showMessage("Needs at least 2 selected objects");
        } else {
            coloc();
        }
    }//GEN-LAST:event_buttonColocActionPerformed

    private void buttonListVoxelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonListVoxelsActionPerformed
        listVoxels();
    }//GEN-LAST:event_buttonListVoxelsActionPerformed

    private void buttonLiveRoiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLiveRoiActionPerformed
        live = !live;
        if (live) {
            this.computeRois();
            this.updateRois();
        }
    }//GEN-LAST:event_buttonLiveRoiActionPerformed

    private void listMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listMouseReleased
        int index = list.locationToIndex(evt.getPoint());
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Select", index);
        }
    }//GEN-LAST:event_listMouseReleased

    private void buttonAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAboutActionPerformed
        AboutMCIB about = new AboutMCIB("RoiManager3D V" + version);
        about.drawAbout();
    }//GEN-LAST:event_buttonAboutActionPerformed

    private void button3DviewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button3DviewerActionPerformed
        fill3DViewer();
    }//GEN-LAST:event_button3DviewerActionPerformed

    private void buttonDeselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeselectActionPerformed
        deselect();
    }//GEN-LAST:event_buttonDeselectActionPerformed

    private void buttonSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectAllActionPerformed
        selectAll();
    }//GEN-LAST:event_buttonSelectAllActionPerformed

    private void buttonLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLabelActionPerformed
        label();
    }//GEN-LAST:event_buttonLabelActionPerformed

    private void buttonFillStackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFillStackActionPerformed
        fill3D();
    }//GEN-LAST:event_buttonFillStackActionPerformed

    private void buttonConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigActionPerformed
        IJ.run("3D Manager Options");
    }//GEN-LAST:event_buttonConfigActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        loadObjects();
    }//GEN-LAST:event_buttonLoadActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RoiManager3D_2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RoiManager3D_2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RoiManager3D_2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RoiManager3D_2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new RoiManager3D_2().setVisible(true);
            }
        });
    }

    /**
     * Adds a feature to the Image attribute of the RoiManager3D_ object
     */
    void addImage() {
        ImagePlus plus = getImage();
        ImagePlus contours;
        String title = plus.getTitle();
        objects3D.setCalibration(plus.getCalibration());

        // cannot add RGB 
        if (plus.getBitDepth() == 24) {
            IJ.showMessage("Cannot import RGB image");
        }

        // image is coming from OC3D
        if (title.contains("Objects map of")) {
            title = title.replaceFirst("Objects map of", "Surface map of");
            contours = WindowManager.getImage(title);
            if (contours != null) {
                IJ.log("Contours found : " + title);
            }
        } // image is coming from 3D segment
        else if (title.contains("-3Dseg")) {
            title = title.replace("-3Dseg", "-3Dsurf");
            contours = WindowManager.getImage(title);
            if (contours != null) {
                IJ.log("Contours found : " + title);
            }
        }
        // TODO , should work with 4D hyperstacks        
        ImageHandler seg = ImageHandler.wrap(plus);

        int minX = 0;
        int maxX = seg.sizeX;
        int minY = 0;
        int maxY = seg.sizeY;
        int minZ = 0;
        int maxZ = seg.sizeZ;

        int nb = objects3D.getNbObjects();

        int min = (int) seg.getMinAboveValue(0);
        int max = (int) seg.getMax();
        IJ.log("Adding image : min-max " + min + " " + max);
        // iterate in image  and constructs objects
        ArrayList<Voxel3D>[] objects = new ArrayList[max - min + 1];
        for (int i = 0; i < max - min + 1; i++) {
            objects[i] = new ArrayList<Voxel3D>();
        }
        float pix;
        for (int k = minZ; k < maxZ; k++) {
            for (int j = minY; j < maxY; j++) {
                for (int i = minX; i < maxX; i++) {
                    pix = seg.getPixel(i, j, k);
                    if (pix > 0) {
                        objects[(int) (pix) - min].add(new Voxel3D(i, j, k, pix));
                    }
                }
            }
        }
        // ARRAYLIST
        ArrayList listObjects = new ArrayList();
        for (int i = 0; i < max - min + 1; i++) {
            if (!objects[i].isEmpty()) {
                // test roi
                listObjects.add(objects[i]);
            }
        }
        // add objects
        addListVoxels(listObjects, plus);

        // HASH
        buildHash();

        IJ.log(objects3D.getNbObjects() - nb + " objects added. Total of " + objects3D.getNbObjects() + " objects");

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_AddImage");
        }
        list.updateUI();
    }

    public void addListVoxels(ArrayList list, ImagePlus plus) {
        ArrayList<Voxel3D> objList;
        Iterator it = list.iterator();
        Object3D obj;
        Calibration cal = plus.getCalibration();
        Roi roi = plus.getRoi();
        boolean excludeXY = Prefs.get("RoiManager3D-Options_ExcludeXY.boolean", false);
        boolean excludeZ = Prefs.get("RoiManager3D-Options_ExcludeZ.boolean", false);
        ImageInt seg;
        if (plus.getBitDepth() < 32) {
            seg = ImageInt.wrap(plus);
        } else {
            seg = null;
        }
        while (it.hasNext()) {
            // Object3D
            objList = (ArrayList<Voxel3D>) it.next();
            if (!objList.isEmpty()) {
                boolean roiok = false;
                boolean edgeok = false;
                obj = new Object3DVoxels(objList);
                obj.setCalibration(cal);
                obj.setLabelImage(seg);
                obj.computeContours();
                // seg image is only used to compute contours, after remove, in case user closes image
                obj.setLabelImage(null);
                // check if center inside roi
                if (((roi != null) && (roi.contains((int) Math.round(obj.getCenterX()), (int) Math.round(obj.getCenterY())))) || (roi == null)) {
                    roiok = true;
                }
                // check touch edges
                if (excludeXY || excludeZ) {
                    if (!obj.edgeImage(seg, excludeXY, excludeZ)) {
                        edgeok = true;
                    }
                } else {
                    edgeok = true;
                }
                if (roiok && edgeok) {
                    addObject3D(obj);
                }
            }
        }
        // HASH
        //buildHash();
    }

    public void addObjects3DPopulation(Objects3DPopulation pop) {
        for (int i = 0; i < pop.getNbObjects(); i++) {
            addObject3D(pop.getObject(i));
        }
        buildHash();
    }

    public void addObjects3D(Object3D[] objs) {
        for (Object3D obj : objs) {
            addObject3D(obj);
        }
    }

    public void addObjects3D(ArrayList<Object3D> objs) {
        for (Object3D obj : objs) {
            addObject3D(obj);
        }
    }

    public void addObject3D(Object3D obj) {
        objects3D.addObject(obj);
        String name = getModelNameFromObject(objects3D.getNbObjects() - 1);
        model.addElement(name);
        list.updateUI();
        this.list.repaint();
        this.list.revalidate();
        hashNames.put(name, model.size() - 1);
    }

    private String getModelNameFromObject(int i) {
        Object3D obj = objects3D.getObject(i);
        String name;
        //IJ.log("obj:"+obj+" "+obj.getName());
        if (obj.getName().length() == 0) {
            name = "obj" + objects3D.getNbObjects() + "-val" + obj.getValue();
            obj.setName(name);
        } else {
            name = obj.getName();
            if (!obj.getComment().isEmpty()) {
                name = name.concat(" " + obj.getComment());
            }
            if (obj.getType() > 0) {
                name = name.concat(" (" + obj.getType() + ")");
            }
        }
        return name;
    }

    void buildHash() {
        hashNames.clear();
        for (int i = 0; i < model.getSize(); i++) {
            hashNames.put((String) (model.get(i)), i);
        }
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    private boolean merge() {
        int[] indexes = list.getSelectedIndices();

        Object3DVoxels obj0 = (Object3DVoxels) objects3D.getObject(indexes[0]);

        Object3DVoxels obj;
        int le = indexes.length;
        for (int i = 1; i < le; i++) {
            IJ.showStatus("Merging Object " + model.get(indexes[i]));
            obj = (Object3DVoxels) objects3D.getObject(indexes[i]);
            obj0.addVoxels(obj.getVoxels());
        }

        for (int i = le - 1; i > 0; i--) {
            delete(indexes[i]);
        }
        buildHash();
        list.updateUI();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Merge");
        }

        return true;
    }

    private boolean split() {
        ImagePlus plus = getImage();
        //IntImage3D ima = new IntImage3D(plus.getStack());
        int[] indexes = list.getSelectedIndices();
        int i0 = indexes[0];

        Object3D obj0 = objects3D.getObject(i0);
        Calibration cal = obj0.getCalibration();
        int val0 = obj0.getValue();
        //IntImage3D ima0 = obj0.getSegImage();
        Object3D obj1;
        Object3D obj2;
        int nb = model.size();

        if (!(obj0 instanceof Object3DVoxels)) {
            IJ.log("Cannot split object");
            return false;
        }

        boolean res = false;
        // distance between centers (in pixels)
        int dist = (int) Prefs.get("RoiManager3D-Options_splitDist.double", 5);
        IJ.log("Splitting");
        Object3DVoxels[] objslist = Segment3DSpots.splitSpotWatershed(obj0, 2, dist);
        IJ.log("split complete");
        //IJ.log(objslist+" "+objslist.length+" "+objslist[0]+" "+objslist[0].getVolumePixels()+" "+objslist[1]+" "+objslist[1].getVolumePixels());
        if ((objslist != null) && (objslist[0].getVolumePixels() > 0) && (objslist[1].getVolumePixels() > 0)) {
            res = true;
            // with arraylist
            obj1 = objslist[0];
            obj1.setCalibration(cal);
            obj1.setValue(val0);
            obj1.setName(obj0.getName() + "-split1");
            obj2 = objslist[1];
            obj2.setCalibration(cal);
            obj2.setValue(val0);
            obj2.setName(obj0.getName() + "-split2");
            delete(i0);
            addObject3D(obj1);
            addObject3D(obj2);
            buildHash();
            list.updateUI();
        } else {
            IJ.log("Object was not splitted");
        }

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Split");
        }

        return res;
    }

    public boolean updateName(int index) {
        hashNames.remove(getModelNameFromObject(index));
        String name2 = getModelNameFromObject(index);
        model.set(index, name2);
        list.updateUI();
        hashNames.put(name2, index);
        return true;
    }

    /**
     * Description of the Method
     *
     * @param name2 Description of the Parameter
     * @return Description of the Return Value
     */
    boolean rename(String name2) {
        int index = list.getSelectedIndex();
        if (index < 0) {
            return error("Exactly one item in the list must be selected.");
        }
        if (name2 == null) {
            name2 = IJ.getString("New name", "Name");
        }
        objects3D.getObject(index).setName(name2);
        model.set(index, name2);
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Rename", name2);
        }
        list.updateUI();

        // test write all names
//        for (int i = 0; i < model.getSize(); i++) {
//            System.out.println("" + model.get(i));
//        }
        return true;
    }

    /**
     * Delete and erase the object
     *
     * @param replacing parameter for roimanager
     * @param erase true if pixels must be set to zero else only suppress from
     * the list
     * @return Description of the Return Value
     */
    boolean delete(boolean erase) {
        int count = model.getSize();
        if (count == 0) {
            return false;
        }
        int index[] = list.getSelectedIndices();

        if ((index.length == 0) || (index.length == list.getModel().getSize())) {
            String msg = "Delete all items on the list?";

            canceled = false;

            if (!IJ.macroRunning() && !IJ.isMacro()) {
                YesNoCancelDialog d = new YesNoCancelDialog(this, "ROIManager3D", msg);
                if (d.cancelPressed()) {
                    canceled = true;
                    return false;
                }
                if (!d.yesPressed()) {
                    return false;
                }
            }
            index = getAllIndexes();
        }
        // prevent from updating live Rois
        live = false;
        buttonLiveRoi.setSelected(false);

        boolean delete;

        // check if really want to erase
        if ((erase) && !IJ.macroRunning() && !IJ.isMacro()) {
            if (!IJ.showMessageWithCancel("Erase ?", "Erase will delete the objects in the current image, are you sure ?")) {
                return false;
            }
        }
        for (int i = count - 1; i >= 0; i--) {
            delete = false;
            for (int j = 0; j < index.length; j++) {
                if (index[j] == i) {
                    delete = true;
                }
            }
            if (delete) {
                if (erase) {
                    fill3D(0, 0, 0);
                }
                delete(i);
            }
        }

        buildHash();

        //updateShowAll();
        if (Recorder.record) {
            if (!erase) {
                Recorder.record("Ext.Manager3D_Delete");
            } else {
                Recorder.record("Ext.Manager3D_Erase");
            }
        }

        // rebuild hash
        buildHash();

        // update rois to nothing
        if (!IJ.macroRunning() && !IJ.isMacro()) {
            //computeRois();
            // updateRois();
        }

        return true;
    }

    void delete(int i) {
        //roimanager.getROIs().remove(name);
        //list.remove(i);
        model.remove(i);
        objects3D.removeObject(i);
        // rebuild hash
        //buildHash();
        // 3D
        //delete3DViewer(name);
    }

    /**
     * Description of the Method
     */
    private void segmentation3D() {
        GenericDialog gd = new GenericDialog("Threshold 3D");
        gd.addNumericField("Low_Threshold", 128, 0);
        gd.addNumericField("High_Threshold", 255, 0);
        gd.showDialog();
        int low = (int) gd.getNextNumber();
        int high = (int) gd.getNextNumber();

        segmentation3D(low, high);
    }

    // draw the object in a color in a image
    private void fill3D(int r, int g, int b) {
        // selected objects
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        // current image to fill in
        ImagePlus ima = getImage();
        if (ima == null) {
            return;
        }
        ImageStack stack = ima.getStack();
        ImageProcessor processor = stack.getProcessor(1);
        double rr = r;
        double gg = g;
        double bb = b;
        boolean color = (processor instanceof ColorProcessor);
        boolean gray = ((processor instanceof ByteProcessor) || (processor instanceof ShortProcessor));
        int intensity = 0;
        if (gray) {
            intensity = (int) Math.round(rr * 0.3 + gg * 0.6 + bb * 0.1);
        }
        for (int i = 0; i < indexes.length; i++) {
            Object3D obj = objects3D.getObject(indexes[i]);
            // if gray draw luminosity gray level
            if (gray) {
                obj.draw(stack, intensity);
                ima.updateAndDraw();
            } else if (color) {
                obj.draw(stack, r, g, b);
                ima.updateAndDraw();
            } else {
                IJ.log("Image Type not supported for fill 3D");
            }
        }
        if (Recorder.record) {
            if (gray) {
                Recorder.record("Ext.Manager3D_FillStack", intensity, intensity, intensity);
            } else if (color) {
                Recorder.record("Ext.Manager3D_FillStack", r, g, b);
            }
        }
    }

    private void fill3D() {
        Color col = Toolbar.getForegroundColor();
        fill3D(col.getRed(), col.getGreen(), col.getBlue());
    }

    private void fill3DViewer() {
        Color col = Toolbar.getForegroundColor();
        fill3DViewer(col.getRed(), col.getGreen(), col.getBlue());
    }

    // draw the object in a color in a image
    private void fill3DViewer(int r, int g, int b) {
        // selected objects
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        for (int i = 0; i < indexes.length; i++) {
            Object3D obj = (Object3D) objects3D.getObject(indexes[i]);
            add3DViewer(obj, (String) model.get(indexes[i]), new Color3f(r / 255.0f, g / 255.0f, b / 255.0f));
        }
        if (showUniverse) {
            universe.show();
            showUniverse = false;
            //universe.getCanvas().addNotify();
        }

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Fill3DViewer", r, g, b, 0);
        }
    }

    synchronized void add3DViewer(Object3D obj, String name, Color3f col) {
        java.util.List l = null;
        Object3DSurface surf = obj.getObject3DSurface();
        l = surf.getSurfaceTrianglesUnit(false);

        if (l != null) {
            if (!universe.contains(name)) {
                System.out.println("Adding obj " + name);
                CustomTriangleMesh tm = new CustomTriangleMesh(l);
                tm.setColor(col);
                Content surface = universe.addCustomMesh(tm, name);
                surface.toggleLock();
                System.out.println("Added obj " + surface.toString());
            } // already exists
            else {
                System.out.println("Recoloring obj " + name);
                Content surface = universe.getContent(name);
                surface.setColor(col);
            }
        }
    }

    private void getUniverse() {
        System.out.println("Getting universes");
        java.util.List Viewers3D = Image3DUniverse.universes;
        System.out.println("Universes opened " + Viewers3D.size());
        if (!Viewers3D.isEmpty()) {
            universe = (Image3DUniverse) Viewers3D.get(0);
            //universe.show();
        } else {
            universe = new Image3DUniverse(512, 512);
            IJ.wait(100);
            //universe.show();
        }

        System.out.println("Universe " + universe + " " + universe.allContentsString());
    }

    /**
     * Description of the Method
     *
     * @param low Description of the Parameter
     * @param high Description of the Parameter
     */
    private void segmentation3D(int low, int high) {
        ImagePlus plus = getImage();
        plus.killRoi();
        String title = plus.getTitle();
        Calibration cal = plus.getCalibration();
        // Use ImageLabeller
        ImageHandler ima = ImageHandler.wrap(plus);
        ima = ima.threshold(low, false, false);
        ImageLabeller labels = new ImageLabeller();
        ImageHandler seg;
        if (Prefs.get("RoiManager3D-Options_Seg32.boolean", false)) {
            seg = labels.getLabelsFloat(ima);
        } else {
            seg = labels.getLabels(ima);
        }
        if (cal != null) {
            seg.setScale(cal.pixelWidth, cal.pixelDepth, cal.getUnits());
        }
        seg.show(title + "-3Dseg");
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Segment", low, high);
        }
    }

    /**
     * Description of the Method
     */
    private void angle() {
        int[] indexes = list.getSelectedIndices();
        int i1 = indexes[0];
        int i2 = indexes[1];
        int i3 = indexes[2];
        Object3D ob1 = objects3D.getObject(i1);
        Object3D ob2 = objects3D.getObject(i2);
        Object3D ob3 = objects3D.getObject(i3);

        IJ.log("\nObjects : " + model.get(i1) + " " + model.get(i2) + " " + model.get(i3));
        IJ.log("Angle 1 (213) : " + ob1.angle(ob2, ob3));
        IJ.log("Angle 2 (123) : " + ob2.angle(ob1, ob3));
        IJ.log("Angle 3 (132) : " + ob3.angle(ob1, ob2));

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Angle");
        }
    }

    /**
     * distance between two objects
     */
    private void coloc() {
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int nb = indexes.length;
        Object3D ob1;

        Object3D ob2;
        double distmax = Prefs.get("RoiManager3D-Options_surfDist.double", 1.0);
        boolean sc = Prefs.get("RoiManager3D-Options_SurfContact.boolean", true);

        ArrayList<String> headings = new ArrayList<String>();
        headings.add("Nb");
        headings.add("Obj1");
        headings.add("Obj2");
        headings.add("Label1");
        headings.add("Label2");
        headings.add("coloc");
        headings.add("PcColoc");
        if (sc) {
            headings.add("SurfCont pos");
            headings.add("SurfCont neg");
            headings.add("SurfCont t");
        }

        Object[][] data = new Object[(indexes.length * (indexes.length - 1))][headings.size()];

        int count = 0;
        for (int i1 = 0; i1 < nb; i1++) {
            ob1 = objects3D.getObject(indexes[i1]);
            for (int i2 = i1 + 1; i2 < nb; i2++) {
                IJ.showStatus("Coloc " + (indexes[i1] + 1) + "-" + (indexes[i2] + 1));
                ob2 = objects3D.getObject(indexes[i2]);
                int h1 = 0;
                int h2 = 0;
                data[count][h1++] = count;
                data[count][h1++] = (indexes[i1] + 1);
                data[count][h1++] = (indexes[i2] + 1);
                data[count][h1++] = model.get(indexes[i1]);
                data[count][h1++] = model.get(indexes[i2]);
                data[count + 1][h2++] = count + 1;
                data[count + 1][h2++] = (indexes[i2] + 1);
                data[count + 1][h2++] = (indexes[i1] + 1);
                data[count + 1][h2++] = model.get(indexes[i2]);
                data[count + 1][h2++] = model.get(indexes[i1]);

                data[count][h1++] = ob1.getColoc(ob2);
                data[count + 1][h2++] = ob2.getColoc(ob1);
                data[count][h1++] = ob1.pcColoc(ob2);
                data[count + 1][h2++] = ob2.pcColoc(ob1);

                if (sc) {
                    int[] surfc = ob1.surfaceContact(ob2, distmax);
                    data[count][h1++] = surfc[0];
                    data[count][h1++] = surfc[1];
                    data[count][h1++] = surfc[0] + surfc[1];
                    surfc = ob2.surfaceContact(ob1, distmax);
                    data[count + 1][h2++] = surfc[0];
                    data[count + 1][h2++] = surfc[1];
                    data[count + 1][h2++] = surfc[0] + surfc[1];
                }

                count += 2;
            }
        }
        // JTABLE
        //Create and set up the window.
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        tableResultsColoc = new ResultsFrame("3D Coloc", heads, data, this, ResultsFrame.OBJECTS_2);
        tableResultsColoc.showFrame();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Coloc");
        }

    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    private boolean measure3D() {
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        if (indexes.length == 0) {
            return false;
        }

        ArrayList<String> headings = new ArrayList<String>();
        headings.add("Nb");
        headings.add("Obj");
        headings.add("Label");
        if (Prefs.get("RoiManager3D-Options_centroid-pix.boolean", true)) {
            headings.add("CX (pix)");
            headings.add("CY (pix)");
            headings.add("CZ (pix)");
        }
        if (Prefs.get("RoiManager3D-Options_centroid-unit.boolean", true)) {
            headings.add("CX (unit)");
            headings.add("CY (unit)");
            headings.add("CZ (unit)");
        }
        if (Prefs.get("RoiManager3D-Options_BB.boolean", true)) {
            headings.add("Xmin (pix)");
            headings.add("Ymin (pix)");
            headings.add("Zmin (pix)");
            headings.add("Xmax (pix)");
            headings.add("Ymax (pix)");
            headings.add("Zmax (pix)");
            headings.add("VolBounding (pix)");
            headings.add("RatioVolbox");
        }
        if (Prefs.get("RoiManager3D-Options_volume.boolean", true)) {
            headings.add("Vol (unit)");
            headings.add("Vol (pix)");
        }
        if (Prefs.get("RoiManager3D-Options_surface.boolean", true)) {
            headings.add("Surf (unit)");
            headings.add("Surf (pix)");
            headings.add("SurfCorr (pix)");
        }
        if (Prefs.get("RoiManager3D-Options_compacity.boolean", true)) {
            headings.add("Comp (pix)");
            headings.add("Spher (pix)");
            headings.add("CompCorr (pix)");
            headings.add("SpherCorr (pix)");
            headings.add("Comp (unit)");
            headings.add("Spher (unit)");
            headings.add("CompDiscrete");
        }
        if (Prefs.get("RoiManager3D-Options_feret.boolean", false)) {
            headings.add("Feret (unit)");
        }
        if (Prefs.get("RoiManager3D-Options_ellipse.boolean", true)) {
            headings.add("Ell_MajRad");
            headings.add("Ell_Elon");
            headings.add("Ell_Flatness");
            headings.add("volEllipsoid (unit)");
            headings.add("RatioVolEllipsoid");
        }
        if (Prefs.get("RoiManager3D-Options_invariants.boolean", false)) {
            for (int g = 0; g < Object3D.getNbMoments3D(); g++) {
                headings.add("Moment" + (g + 1));
            }
        }
        // CONVEX HULL
        if (Prefs.get("RoiManager3D-Options_convexhull.boolean", false)) {
            headings.add("SurfMesh (unit)");
            headings.add("SurfMeshsmooth (unit)");
            headings.add("SurfMeshHull (unit)");
            headings.add("VolHull (unit)");
        }
        if (Prefs.get("RoiManager3D-Options_dist2Surf.boolean", true)) {
            headings.add("DCMin (unit)");
            headings.add("DCMax (unit)");
            headings.add("DCMean (unit)");
            headings.add("DCSD (unit)");
        }

        Object[][] data = new Object[indexes.length][headings.size()];

        Object3D obj;
        double resXY;
        double resZ;
        //int count = rtMeasure.getCounter();
        for (int i = 0; i < indexes.length; i++) {
            int h = 0;
            data[i][h++] = i;
            data[i][h++] = indexes[i] + 1;
            data[i][h++] = model.get(indexes[i]);
            obj = objects3D.getObject(indexes[i]);
            resXY = obj.getResXY();
            resZ = obj.getResZ();
            if (Prefs.get("RoiManager3D-Options_centroid-pix.boolean", true)) {
                data[i][h++] = obj.getCenterX();
                data[i][h++] = obj.getCenterY();
                data[i][h++] = obj.getCenterZ();
            }
            if (Prefs.get("RoiManager3D-Options_centroid-unit.boolean", true)) {
                data[i][h++] = obj.getCenterX() * resXY;
                data[i][h++] = obj.getCenterY() * resXY;
                data[i][h++] = obj.getCenterZ() * resZ;
            }
            if (Prefs.get("RoiManager3D-Options_BB.boolean", true)) {
                data[i][h++] = obj.getXmin();
                data[i][h++] = obj.getYmin();
                data[i][h++] = obj.getZmin();
                data[i][h++] = obj.getXmax();
                data[i][h++] = obj.getYmax();
                data[i][h++] = obj.getZmax();
                data[i][h++] = obj.getVolumeBoundingBoxPixel();
                data[i][h++] = obj.getRatioBox();
            }
            if (Prefs.get("RoiManager3D-Options_volume.boolean", true)) {
                data[i][h++] = obj.getVolumeUnit();
                data[i][h++] = obj.getVolumePixels();
            }
            if (Prefs.get("RoiManager3D-Options_surface.boolean", true)) {
                data[i][h++] = obj.getAreaUnit();
                data[i][h++] = obj.getAreaPixels();
                if (obj instanceof Object3DVoxels) {
                    data[i][h++] = ((Object3DVoxels) obj).getAreaPixelsCorrected();
                } else {
                    data[i][h++] = -1;
                }
            }
            if (Prefs.get("RoiManager3D-Options_compacity.boolean", true)) {
                data[i][h++] = obj.getCompactness(false);
                data[i][h++] = obj.getSphericity(false);
                // TEST LAURENT GOLE
                if (obj instanceof Object3DVoxels) {
                    data[i][h++] = ((Object3DVoxels) obj).getCompactnessCorrected();
                    data[i][h++] = ((Object3DVoxels) obj).getSphericityCorrected();
                } else {
                    data[i][h++] = -1;
                    data[i][h++] = -1;
                }
                data[i][h++] = obj.getCompactness(true);
                data[i][h++] = obj.getSphericity(true);

                // TEST DISCRETE COMPACITE
                if (obj instanceof Object3DVoxels) {
                    data[i][h++] = ((Object3DVoxels) obj).getDiscreteCompactness();
                } else {
                    data[i][h++] = -1;
                }
            }
            if (Prefs.get("RoiManager3D-Options_feret.boolean", false)) {
                data[i][h++] = obj.getFeret();
            }
            if (Prefs.get("RoiManager3D-Options_ellipse.boolean", true)) {
                data[i][h++] = obj.getRadiusMoments(2);
                data[i][h++] = obj.getMainElongation();
                data[i][h++] = obj.getMedianElongation();
                data[i][h++] = obj.getVolumeEllipseUnit();
                data[i][h++] = obj.getRatioEllipsoid();
            }
            if (Prefs.get("RoiManager3D-Options_invariants.boolean", false)) {
                double[] geoinv = obj.getMoments3D();
                for (int g = 0; g < geoinv.length; g++) {
                    data[i][h++] = geoinv[g];
                }
            }
            // CONVEX HULL
            if (Prefs.get("RoiManager3D-Options_convexhull.boolean", false)) {
                Object3DSurface surf = new Object3DSurface(obj.computeMeshSurface(false));
                surf.setCalibration(obj.getCalibration());
                surf.setSmoothingFactor(0.1f);
                Object3DSurface convexsurf = surf.getConvexSurface();
                convexsurf.multiThread = true;
                double volHull = convexsurf.getVolumeUnit();
                data[i][h++] = surf.getSurfaceMeshUnit();
                data[i][h++] = surf.getSmoothSurfaceAreaUnit();
                data[i][h++] = convexsurf.getSurfaceMeshUnit();
                data[i][h++] = volHull;
            }
            if (Prefs.get("RoiManager3D-Options_dist2Surf.boolean", true)) {
                data[i][h++] = obj.getDistCenterMin();
                data[i][h++] = obj.getDistCenterMax();
                data[i][h++] = obj.getDistCenterMean();
                data[i][h++] = obj.getDistCenterSigma();
            }
        }

        // JTABLE
        //Create and set up the window.
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        tableResultsMeasure = new ResultsFrame("3D Measure", heads, data, this, ResultsFrame.OBJECT_1);
        tableResultsMeasure.showFrame();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Measure");
        }

        return true;
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    private boolean quantif3D() {
        ImagePlus imp = getImage();
        if (imp == null) {
            IJ.error("Error : No window opened.");
            return false;
        }

        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        if (indexes.length == 0) {
            return false;
        }

        ArrayList<String> headings = new ArrayList<String>();
        headings.add("Nb");
        headings.add("Obj");
        headings.add("Label");
        headings.add("AtCenter");
        if (Prefs.get("RoiManager3D-Options_COM-pix.boolean", true)) {
            headings.add("CMx (pix)");
            headings.add("CMy (pix)");
            headings.add("CMy (pix)");
        }
        if (Prefs.get("RoiManager3D-Options_COM-unit.boolean", true)) {
            headings.add("CMx (unit)");
            headings.add("CMy (unit)");
            headings.add("CMy (unit)");
        }
        if (Prefs.get("RoiManager3D-Options_intDens.boolean", true)) {
            headings.add("IntDen");
        }
        if (Prefs.get("RoiManager3D-Options_min.boolean", true)) {
            headings.add("Min");
        }
        if (Prefs.get("RoiManager3D-Options_max.boolean", true)) {
            headings.add("Max");
        }
        if (Prefs.get("RoiManager3D-Options_mean.boolean", true)) {
            headings.add("Mean");
        }
        if (Prefs.get("RoiManager3D-Options_stdDev.boolean", true)) {
            headings.add("Sigma");
        }
        if (Prefs.get("RoiManager3D-Options_Mode.boolean", true)) {
            headings.add("Mode");
            headings.add("Mode NonZero");
        }

        Object[][] data = new Object[indexes.length][headings.size()];
        double resXY, resZ;
        Object3D obj;
        ImageHandler ima = this.getImage3D();
        for (int i = 0; i < indexes.length; i++) {
            int h = 0;
            data[i][h++] = i;
            data[i][h++] = indexes[i] + 1;
            data[i][h++] = model.get(indexes[i]);
            obj = objects3D.getObject(indexes[i]);
            resXY = obj.getResXY();
            resZ = obj.getResZ();
            data[i][h++] = obj.getPixCenterValue(ima);
            if (Prefs.get("RoiManager3D-Options_COM-pix.boolean", true)) {
                data[i][h++] = obj.getMassCenterX(ima);
                data[i][h++] = obj.getMassCenterY(ima);
                data[i][h++] = obj.getMassCenterZ(ima);
            }
            if (Prefs.get("RoiManager3D-Options_COM-unit.boolean", true)) {
                data[i][h++] = obj.getMassCenterX(ima) * resXY;
                data[i][h++] = obj.getMassCenterY(ima) * resXY;
                data[i][h++] = obj.getMassCenterZ(ima) * resZ;
            }
            if (Prefs.get("RoiManager3D-Options_intDens.boolean", true)) {
                data[i][h++] = obj.getIntegratedDensity(ima);
            }
            if (Prefs.get("RoiManager3D-Options_min.boolean", true)) {
                data[i][h++] = obj.getPixMinValue(ima);
            }
            if (Prefs.get("RoiManager3D-Options_max.boolean", true)) {
                data[i][h++] = obj.getPixMaxValue(ima);
            }
            if (Prefs.get("RoiManager3D-Options_mean.boolean", true)) {
                data[i][h++] = obj.getPixMeanValue(ima);
            }
            if (Prefs.get("RoiManager3D-Options_stdDev.boolean", true)) {
                data[i][h++] = obj.getPixStdDevValue(ima);
            }
            if (Prefs.get("RoiManager3D-Options_Mode.boolean", true)) {
                data[i][h++] = obj.getPixModeValue(ima);
                data[i][h++] = obj.getPixModeNonZero(ima);
            }
        }

        // JTABLE
        //Create and set up the window.
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        tableResultsQuantif = new ResultsFrame("3D Quantif", heads, data, this, ResultsFrame.OBJECT_1);
        //Create and set up the content pane.        

        tableResultsQuantif.showFrame();
//

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Quantif");
        }

        return true;
    }

    private boolean listVoxels() {
        Object3D obj;
        ArrayList v;

        ImagePlus imp = getImage();
        if (imp == null) {
            IJ.error("Error : No window opened ?");
            return false;
        }

        // only one object selected 
        int[] indexes = list.getSelectedIndices();
//        if (indexes.length != 1) {
//            return false;
//        }

//        if (rtVoxels == null) {
//            rtVoxels = new ResultsTable();
//        }
        String title = imp.getShortTitle();
        ImageHandler image = this.getImage3D();
        if (image == null) {
            IJ.log("Error, cannot list voxels for this image : " + imp.getTitle());
            return false;
        }

        ArrayList<String> headings = new ArrayList<String>();
        headings.add("Nb");
        headings.add("Obj");
        headings.add("Label");
        headings.add("X");
        headings.add("Y");
        headings.add("Z");
        headings.add("Value");

        // nb totals de voxels
        int vol = 0;
        for (int idx : indexes) {
            vol += objects3D.getObject(idx).getVolumePixels();
        }

        Object[][] data = new Object[vol][headings.size()];

        Voxel3D voxel;
        int count = 0;

        for (int ob = 0; ob < indexes.length; ob++) {
            int nbObj = indexes[ob] + 1;
            Object nameObj = model.get(indexes[ob]);
            obj = objects3D.getObject(indexes[ob]);
            //IJ.log("image to list: "+image);
            v = obj.listVoxels(image);
            if (v == null) {
                IJ.log("No voxels to display for " + model.get(indexes[ob]));
                return false;
            }
            for (int i = 0; i < v.size(); i++) {
                int h = 0;
                data[count][h++] = count;
                data[count][h++] = nbObj;
                data[count][h++] = nameObj;
                voxel = (Voxel3D) v.get(i);
                data[count][h++] = voxel.getX();
                data[count][h++] = voxel.getY();
                data[count][h++] = voxel.getZ();
                data[count][h++] = voxel.getValue();
                count++;
            }
        }

        // JTABLE
        //Create and set up the window.
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        tableResultsVoxels = new ResultsFrame("3D Voxels", heads, data, this, ResultsFrame.OBJECT_NO);
        //Create and set up the content pane.        
        tableResultsVoxels.showFrame();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_List");
        }

        return true;
    }

    private void loadObjects() {
        OpenDialog op = new OpenDialog("Open RoiSet3D", "");
        loadObjects(op.getDirectory() + op.getFileName());
    }

    private void loadObjects(String path) {
        Objects3DPopulation popTmp = new Objects3DPopulation();
        popTmp.loadObjects(path);
        addObjects3DPopulation(popTmp);
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Load", path);
        }
    }

    private boolean saveObjects() {
        OpenDialog op = new OpenDialog("Save RoiSet3D", "");
        return saveObjects(op.getDirectory() + op.getFileName());
    }

    private boolean saveObjects(String path) {
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        return objects3D.saveObjects(path, indexes);
    }

    /**
     * distance between objects
     */
    private void distance() {
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int nb = indexes.length;
        Object3D ob1;
        Object3D ob2;

//        if (rtDistance == null) {
//            rtDistance = new ResultsTable();
//            int h = 0;
//        }
        ArrayList<String> headings = new ArrayList<String>();
        headings.add("Nb");
        headings.add("Obj1");
        headings.add("Obj2");
        headings.add("Label1");
        headings.add("Label2");
        headings.add("cen-cen");
        headings.add("cen-bor");
        headings.add("bor-bor");
        if (Prefs.get("RoiManager3D-Options_RadDist.boolean", true)) {
            headings.add("radiusCen");
            headings.add("excen");
            headings.add("bor-rad");
            headings.add("periph");
        }
        if (Prefs.get("RoiManager3D-Options_Closest.boolean", true)) {
            headings.add("closest_cen_i");
            headings.add("closest_bor_i");
            headings.add("closest_cen_n");
            headings.add("closest_bor_n");
        }

        Object[][] data = new Object[(indexes.length * (indexes.length - 1))][headings.size()];

        int count = 0;
        double d1, d2;
        double dist;
        for (int i1 = 0; i1 < nb; i1++) {
            ob1 = objects3D.getObject(indexes[i1]);
            for (int i2 = i1 + 1; i2 < nb; i2++) {
                IJ.showStatus("Distance " + (indexes[i1] + 1) + "-" + (indexes[i2] + 1));
                int h1 = 0;
                int h2 = 0;
                data[count][h1++] = count;
                data[count][h1++] = (indexes[i1] + 1);
                data[count][h1++] = (indexes[i2] + 1);
                data[count][h1++] = model.get(indexes[i1]);
                data[count][h1++] = model.get(indexes[i2]);
                data[count + 1][h2++] = count + 1;
                data[count + 1][h2++] = (indexes[i2] + 1);
                data[count + 1][h2++] = (indexes[i1] + 1);
                data[count + 1][h2++] = model.get(indexes[i2]);
                data[count + 1][h2++] = model.get(indexes[i1]);

                ob2 = objects3D.getObject(indexes[i2]);
                // From object 1
                d1 = ob1.distCenterUnit(ob2);
                data[count][h1++] = d1;
                data[count][h1++] = ob1.distCenterBorderUnit(ob2);
                data[count][h1++] = ob1.distBorderUnit(ob2);
                // From object 2
                data[count + 1][h2++] = d1;
                data[count + 1][h2++] = ob2.distCenterBorderUnit(ob1);
                data[count + 1][h2++] = ob2.distBorderUnit(ob1);
                if (Prefs.get("RoiManager3D-Options_RadDist.boolean", true)) {
                    d2 = ob1.radiusCenter(ob2);
                    data[count][h1++] = d2;
                    data[count][h1++] = d1 / d2;
                    // border-radius
                    double dist1 = ob1.distBorderUnit(ob1.getCenterAsPoint(), ob2, ob2.getCenterAsPoint(), false);
                    double dist2 = ob1.distBorderUnit(ob1.getCenterAsPoint(), ob2, ob2.getCenterAsPoint(), true);
                    dist = Math.min(dist1, dist2);
                    data[count][h1++] = dist;
                    data[count][h1++] = dist / d2;
                    d2 = ob2.radiusCenter(ob1);
                    data[count + 1][h2++] = d2;
                    data[count + 1][h2++] = d1 / d2;
                    // border-radius 
                    data[count + 1][h2++] = dist;
                    data[count + 1][h2++] = dist / d2;
                }
                if (Prefs.get("RoiManager3D-Options_Closest.boolean", true)) {
                    int closest = objects3D.getIndexOf(objects3D.closestCenter(ob1, true));
                    String name1 = (String) model.get(closest);
                    data[count][h1++] = closest + 1;
                    closest = objects3D.getIndexOf(objects3D.closestBorder(ob1));
                    String name2 = (String) model.get(closest);
                    data[count][h1++] = closest + 1;
                    data[count][h1++] = name1;
                    data[count][h1++] = name2;
                    closest = objects3D.getIndexOf(objects3D.closestCenter(ob2, true));
                    name1 = (String) model.get(closest);
                    data[count + 1][h2++] = closest + 1;
                    closest = objects3D.getIndexOf(objects3D.closestBorder(ob2));
                    name2 = (String) model.get(closest);
                    data[count + 1][h2++] = closest + 1;
                    data[count + 1][h2++] = name1;
                    data[count + 1][h2++] = name2;
                }
                count += 2;
            }
        }

        // JTABLE
        //Create and set up the window.
        String[] heads = new String[headings.size()];
        heads = headings.toArray(heads);
        tableResultsDistance = new ResultsFrame("3D Distance", heads, data, this, ResultsFrame.OBJECTS_2);
        tableResultsDistance.showFrame();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Distance");
        }
    }

    /**
     * Gets the image attribute of the RoiManager object
     *
     * @return The image value
     */
    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        //ImagePlus imp = IJ.getImage();
        //IJ.log("Current image  : "+imp);
        if (imp == null) {
            //IJ.log("There are no images open.");
            return null;
        } else {
            return imp;
        }
    }

    ImageHandler getImage3D() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return null;
        }
        return ImageHandler.wrap(imp);
    }

    private void removeScrollListener(ImagePlus img, AdjustmentListener al, MouseWheelListener ml) {
        //from Fiji code
        // TODO Find author...
        if ((img.getWindow() != null) && (img.getWindow().getComponents() != null)) {
            for (Component c : img.getWindow().getComponents()) {
                if (c instanceof Scrollbar) {
                    ((Scrollbar) c).removeAdjustmentListener(al);
                } else if (c instanceof Container) {
                    for (Component c2 : ((Container) c).getComponents()) {
                        if (c2 instanceof Scrollbar) {
                            ((Scrollbar) c2).removeAdjustmentListener(al);
                        }
                    }
                }
            }
            img.getWindow().removeMouseWheelListener(ml);
        }
    }

    private void addScrollListener(ImagePlus img, AdjustmentListener al, MouseWheelListener ml) {
        if (!live) {
            return;
        }
        //from Fiji code
        // TODO Find author...
        for (Component c : img.getWindow().getComponents()) {
            if (c instanceof Scrollbar) {
                ((Scrollbar) c).addAdjustmentListener(al);
            } else if (c instanceof Container) {
                for (Component c2 : ((Container) c).getComponents()) {
                    if (c2 instanceof Scrollbar) {
                        ((Scrollbar) c2).addAdjustmentListener(al);
                    }
                }
            }
        }
        img.getWindow().addMouseWheelListener(ml);
    }

    private void registerActiveImage() {
        ImagePlus activeImage = this.getImage();
        //System.out.println("Register " + activeImage + " " + currentImage);
        if (activeImage != null && activeImage.getProcessor() != null && activeImage.getImageStackSize() > 1) {
            if (currentImage != null && currentImage.getWindow() != null && currentImage != activeImage) {
                //System.out.println("remove listener:"+currentImage.getTitle());
                removeScrollListener(currentImage, this, this);
                currentImage.killRoi();
                currentImage.updateAndDraw();
                currentImage = null;
            }
            if (currentImage != activeImage) {
                //System.out.println("add listener:"+activeImage.getTitle());
                addScrollListener(activeImage, this, this);
                this.currentImage = activeImage;
            }
        }
    }

    /**
     * Gets the allIndexes attribute of the RoiManager object
     *
     * @return The allIndexes value
     */
    int[] getAllIndexes() {
        int count = model.getSize();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }

        return indexes;
    }

    /**
     * Description of the Method
     */
    void computeRois() {
        ImagePlus imp = this.getImage();
        if (imp == null) {
            return;
        }

        this.registerActiveImage();

//        if (imp == null) {
//            IJ.log("There are no images open.");
//            return;
//        }
        int zmin = imp.getNSlices() + 1;
        int zmax = -1;

        // draw mask of rois
        int[] indexes = list.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        arrayRois = new Roi[imp.getNSlices()];
        // get zmin and zmax

        Object3D obj;
        for (int i = 0; i < indexes.length; i++) {
            obj = objects3D.getObject(indexes[i]);
            if (obj.getZmin() < zmin) {
                zmin = obj.getZmin();
            }
            if (obj.getZmax() > zmax) {
                zmax = obj.getZmax();
            }
        }
        currentZmin = zmin;
        currentZmax = zmax;

        //IJ.log("Computing rois "+zmin+" "+zmax);
        for (int zz = zmin; zz <= zmax; zz++) {
            //IJ.showStatus("Computing Roi " + zz);
            ByteProcessor mask = new ByteProcessor(imp.getWidth(), imp.getHeight());
            boolean ok = false;
            for (int i = 0; i < indexes.length; i++) {
                obj = objects3D.getObject(indexes[i]);
                ok |= obj.draw(mask, zz, 255);
            }
            if (!ok) {
                arrayRois[zz] = null;
                //IJ.log("No draw for " + zz);
            } else {
                mask.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
                ImagePlus maskPlus = new ImagePlus("mask " + zz, mask);
                //maskPlus.show("mask_" + zz);
                ThresholdToSelection tts = new ThresholdToSelection();
                tts.setup("", maskPlus);
                tts.run(mask);

                arrayRois[zz] = maskPlus.getRoi();
            }
        }

        int middle = (int) (0.5 * zmin + 0.5 * zmax);
        imp.setSlice(middle + 1);
        imp.setRoi(arrayRois[middle]);
        imp.updateAndDraw();
    }

    private void updateRois() {
        updateRois(-1);
    }

    private void updateRois(int slice) {
        // synchronized (this) {
        ImagePlus plus = this.getImage();
        if (plus != null) {
            int sl = plus.getSlice() - 1;
            if (slice >= 0) {
                sl = slice;
            }
            if ((sl >= currentZmin) && (sl <= currentZmax)) {
                plus.setRoi(arrayRois[sl]);
            } else {
                plus.killRoi();
            }
            plus.updateAndDraw();
        }
        //}
    }

    private void label() {
        int[] idx = list.getSelectedIndices();
        ImagePlus plus = this.getImage();
        Overlay over = new Overlay();
        over.drawLabels(false);
        Font font = new Font(Font.DIALOG, Font.PLAIN, 10);
        for (int i = 0; i < idx.length; i++) {
            Object3D obj = objects3D.getObject(idx[i]);
            Roi roi = new TextRoi(obj.getCenterX(), obj.getCenterY(), (String) model.get(idx[i]), font);
            roi.setPosition((int) (obj.getCenterZ() + 1));
            over.add(roi);
        }
        plus.setOverlay(over);
        plus.updateAndDraw();

        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_Label");
        }
    }

    /**
     * Description of the Method
     *
     * @param msg Description of the Parameter
     * @return Description of the Return Value
     */
    boolean error(String msg) {
        new MessageDialog(this, "ROI Manager", msg);
        Macro.abort();
        return false;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button3Dviewer;
    private javax.swing.JButton buttonAbout;
    private javax.swing.JButton buttonAddImage;
    private javax.swing.JButton buttonAngles;
    private javax.swing.JButton buttonColoc;
    private javax.swing.JButton buttonConfig;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDeselect;
    private javax.swing.JButton buttonDistances;
    private javax.swing.JButton buttonErase;
    private javax.swing.JButton buttonFillStack;
    private javax.swing.JButton buttonLabel;
    private javax.swing.JButton buttonListVoxels;
    private javax.swing.JToggleButton buttonLiveRoi;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonMeasure;
    private javax.swing.JButton buttonMerge;
    private javax.swing.JButton buttonQuantif;
    private javax.swing.JButton buttonRename;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonSegmentation3D;
    private javax.swing.JButton buttonSelectAll;
    private javax.swing.JButton buttonSplit;
    public javax.swing.JPanel jPanel;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    public javax.swing.JList list;
    // End of variables declaration//GEN-END:variables

    public void itemStateChanged(ItemEvent ie) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseClicked(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mousePressed(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseReleased(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseEntered(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseExited(MouseEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
        if (live) {
            this.updateRois();
        }
    }

    @Override
    public void transformationStarted(View view) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transformationUpdated(View view) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param view
     */
    @Override
    public void transformationFinished(View view) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void contentAdded(Content c) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void contentRemoved(Content c) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void contentChanged(Content c) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void contentSelected(Content c) {
        if ((c == null) || (!Prefs.get("RoiManager3D-Options_sync3DViewer.boolean", false))) {
            return;
        }
        // IJ.log("Selected content " + c.getName());
        selectByName(c.getName());
    }

    @Override
    public void canvasResized() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void universeClosed() {
        universe = null;
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent ae) {
        //if (live) {
        //IJ.log("Adjustement value changed");
        this.updateRois();
        //}
    }

    // run plugin
    @Override
    public void run(String arg) {
    }

    private void selectAll() {
        list.setSelectionInterval(0, model.getSize() - 1);
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_SelectAll");
        }
    }

    private void deselect() {
        list.clearSelection();
        list.updateUI();
        if (Recorder.record) {
            Recorder.record("Ext.Manager3D_DeselectAll");
        }
    }

    public void selectByName(String name) {
        Integer sel = hashNames.get(name);
        if ((sel != null) && (sel >= 0)) {
            list.setSelectedIndex(sel);
        }
    }

    public void selectByNumber(int se) {
        int sel = se;
        if (sel >= 0) {
            list.setSelectedIndex(sel);
        }
    }

    public void selectByNames(String[] names) {
        int[] sels = new int[names.length];
        int c = 0;
        for (String na : names) {
            sels[c++] = hashNames.get(na);
        }
        list.setSelectedIndices(sels);
    }

    public void selectByNumbers(int[] se) {
        IJ.log("Selecting " + se.length + " objects");
        //list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.clearSelection();
        int[] sel = se;
        if (sel.length > 0) {
            for (int c = 0; c < sel.length; c++) {
                int i = sel[c];
                IJ.log(c + " " + i + " " + model.getElementAt(i) + " " + sel.length);
                list.addSelectionInterval(i, i);
                list.updateUI();
            }

            //list.setSelectedIndices(sel);
        }
        //list.ensureIndexIsVisible(list.getSelectedIndex());
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        //IJ.log("Drag enter");
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        IJ.showStatus("<< Reading 3D roi >>");
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        //IJ.log("Drag changed");
    }

    public void dragExit(DropTargetEvent dte) {
        //IJ.log("Drag exit");
    }

    public void drop(DropTargetDropEvent dtde) {
        //IJ.log("Drag drop");
        //Iterator iterator;
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        DataFlavor[] flavors;
        try {
            Transferable t = dtde.getTransferable();
            //iterator = null;
            flavors = t.getTransferDataFlavors();
            // IJ.log("DragAndDrop.drop: " + flavors.length + " flavors");
            for (DataFlavor flavor : flavors) {
                //IJ.log("  flavor[" + i + "]: " + flavors[i].getMimeType());
                if (flavor.isFlavorJavaFileListType()) {
                    //Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                    //iterator = ((java.util.List) data).iterator();
                    break;
                } else if (flavor.isFlavorTextType()) {
                    Object ob = t.getTransferData(flavor);
                    if (!(ob instanceof String)) {
                        continue;
                    }
                    String s = ob.toString().trim();
                    //ArrayList list = new ArrayList();
                    BufferedReader br = new BufferedReader(new StringReader(s));
                    String tmp;
                    while (null != (tmp = br.readLine())) {
                        tmp = java.net.URLDecoder.decode(tmp.replaceAll("\\+", "%2b"), "UTF-8");
                        if (tmp.startsWith("file://")) {
                            tmp = tmp.substring(7);
                        }
                        //IJ.log("  content: " + tmp);
                        loadObjects(tmp);
                        if (tmp.startsWith("http://")) {
                            //list.add(s);
                        } else {
                            //list.add(new File(tmp));
                        }
                    }
                    //iterator = list.iterator();
                    break;
                }
            }

        } catch (UnsupportedFlavorException e) {
            dtde.dropComplete(false);
            return;
        } catch (IOException e) {
            dtde.dropComplete(false);
            return;
        }
        dtde.dropComplete(true);
        if (flavors == null || flavors.length == 0) {
            if (IJ.isMacOSX()) {
                IJ.error("First drag and drop ignored. Please try again. .");
            } else {
                IJ.error("Drag and drop failed");
            }
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        IJ.log("Closing 3DManager");
        closing();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    private void rotateUniverse(int a, int b, int c) {
        universe.rotateX(a);
        universe.rotateY(b);
        universe.rotateZ(c);
    }

    private void loadView3D(String S) {
        try {
            IJ.log("Loading view " + S);
            universe.loadView(S);
        } catch (IOException ex) {
            IJ.log("View " + S + " does not exists");
        }
    }

    private void closeResult(String win) {
        if ((win.startsWith("A") || win.startsWith("M")) && (tableResultsMeasure != null)) {
            tableResultsMeasure.dispose();
        }
        if ((win.startsWith("A") || win.startsWith("Q")) && (tableResultsQuantif != null)) {
            tableResultsQuantif.dispose();
        }
        if ((win.startsWith("A") || win.startsWith("D")) && (tableResultsDistance != null)) {
            tableResultsDistance.dispose();
        }
        if ((win.startsWith("A") || win.startsWith("C")) && (tableResultsColoc != null)) {
            tableResultsColoc.dispose();
        }
        if ((win.startsWith("A") || win.startsWith("L") || win.startsWith("V")) && (tableResultsVoxels != null)) {
            tableResultsVoxels.dispose();
        }
    }

    private void saveResult(String win, String file) {
        File fi = new File(file);
        String dir, name;
        if (fi.isDirectory()) {
            dir = file;
            name = "Manager3DResults.csv";
        } else {
            dir = fi.getParent();
            name = fi.getName();
        }
        String fs = File.separator;
        if ((win.startsWith("A") || win.startsWith("M")) && (tableResultsMeasure != null)) {
            if (!tableResultsMeasure.getModel().writeData(dir + fs + "M_" + name)) {
                IJ.log("Pb saving " + dir + fs + "M_" + name);
            }
        }
        if ((win.startsWith("A") || win.startsWith("Q")) && (tableResultsQuantif != null)) {
            if (!tableResultsQuantif.getModel().writeData(dir + fs + "Q_" + name)) {
                IJ.log("Pb saving " + dir + fs + "Q_" + name);
            }
        }
        if ((win.startsWith("A") || win.startsWith("D")) && (tableResultsDistance != null)) {
            if (!tableResultsDistance.getModel().writeData(dir + fs + "D_" + name)) {
                IJ.log("Pb saving " + dir + fs + "D_" + name);
            }
        }
        if ((win.startsWith("A") || win.startsWith("C")) && (tableResultsColoc != null)) {
            if (!tableResultsColoc.getModel().writeData(dir + fs + "C_" + name)) {
                IJ.log("Pb saving " + dir + fs + "C_" + name);
            }
        }
        if ((win.startsWith("A") || win.startsWith("L") || win.startsWith("V")) && (tableResultsVoxels != null)) {
            if (!tableResultsVoxels.getModel().writeData(dir + fs + "V_" + name)) {
                IJ.log("Pb saving " + dir + fs + "V_" + name);
            }
        }

    }

}
