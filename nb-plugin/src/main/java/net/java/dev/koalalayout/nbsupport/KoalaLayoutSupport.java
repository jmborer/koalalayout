/*
 * This file is part of KoalaLayoutSupport.
 *
 * KoalaLayoutSupport is a Netbeans module to work with the KoalaLayout.
 * Copyright (C) 2007 Jean-Marc Borer, Jerome Layat, Christian Lebaudy,
 *                    Peter Flukiger, Julien Piaser
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
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package net.java.dev.koalalayout.nbsupport;

import net.java.dev.koalalayout.KoalaLayout;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.*;
import java.util.*;
import java.lang.reflect.*;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.netbeans.modules.form.FormProperty;
import org.netbeans.modules.form.codestructure.CodeExpression;
import org.netbeans.modules.form.codestructure.CodeGroup;
import org.netbeans.modules.form.codestructure.CodeStatement;
import org.netbeans.modules.form.codestructure.CodeStructure;
import org.netbeans.modules.form.codestructure.CodeVariable;
import org.netbeans.modules.form.codestructure.FormCodeSupport;
import org.netbeans.modules.form.layoutsupport.AbstractLayoutSupport;
import org.netbeans.modules.form.layoutsupport.LayoutConstraints;
import org.netbeans.modules.form.layoutsupport.LayoutSupportContext;
import org.netbeans.modules.form.layoutsupport.delegates.AbsoluteLayoutSupport;
import org.openide.ErrorManager;
import org.openide.util.ImageUtilities;

/**
 * Support class for KoalaLayout. This is an example of support for layout managers with complex layout constraints for
 * which rather special code structure must be managed - GridBagConstraints require to be set up field by field.
 *
 * @author Jean-Marc Borer
 */
public class KoalaLayoutSupport extends AbstractLayoutSupport {

    /**
     * The icon 16x16 for KoalaLayout.
     */
    private static String iconURL = "net/java/dev/koalalayout/nbsupport/resources/koalalayout16.png"; // NOI18N
    /**
     * The icon 32x32 for KoalaLayout.
     */
    private static String icon32URL = "net/java/dev/koalalayout/nbsupport/resources/koalalayout32.png"; // NOI18N
    private static final int X_AXIS = 1;
    private static final int Y_AXIS = 2;
    //    private static Constructor constrConstructor;
    private KoalaLayoutCustomizer layoutCustomizer;
    private JDialog customizerDialog;
    /**
     * Just for netbeans module debugging (switch boolean to log or not)
     */
    private static final boolean LOGGABLE = true;

    private void netbeansLog(String msg) {
        if (LOGGABLE) {
            System.err.println(msg);
        }
    }

    /** Gets the supported layout manager or container class: KoalaLayout. Container class
     * is returned if the delegate is "dedicated" to some special container
     * rather than to a layout manager used generally for any container.
     * @return the class supported by this delegate
     * @see isDedicated method
     */
    @Override
    public Class getSupportedClass() {
        return KoalaLayout.class;
    }

    /**
     * Provides an icon to be used for the layout node in Component Inspector. Only 16x16 color icon is required.
     *
     * @param type is one of BeanInfo constants: ICON_COLOR_16x16, ICON_COLOR_32x32, ICON_MONO_16x16, ICON_MONO_32x32
     * @return icon to be displayed for node in Component Inspector
     */
    @Override
    public Image getIcon(int type) {
        Image img = null;
        switch (type) {
            case BeanInfo.ICON_COLOR_16x16:
            case BeanInfo.ICON_MONO_16x16:
                img = ImageUtilities.loadImage(iconURL);
                break;
            default:
                img = ImageUtilities.loadImage(icon32URL);
        }
        return img;
    }

    /**
     * Returns a class of customizer for KoalaLayout.
     *
     * @return layout customizer class
     */
    @Override
    public Class getCustomizerClass() {
        // return KoalaLayoutCustomizer.class;
        return null;
    }

    /**
     * Creates an instance of customizer for KoalaLayout.
     *
     * @return layout customizer class
     */
    @Override
    public Component getSupportCustomizer() {
//        if (layoutCustomizer == null || customizerDialog == null) {
//            layoutCustomizer = new KoalaLayoutCustomizer(this);
//            customizerDialog = createCustomizerDialog();
//        }
//        return customizerDialog;
        return null;
    }

    /**
     * This method is called when switching layout - giving an opportunity to convert the previous constraints of
     * components to constraints of the new layout (this layout). Conversion from AbsoluteConstraints to
     * GridBagConstraints is implemented here.
     *
     * @param previousConstraints [input] layout constraints of components in the previous layout
     * @param currentConstraints [output] array of converted constraints for the new layout - to be filled
     * @param components [input] real components in a real container having the previous layout
     */
    @Override
    public void convertConstraints(LayoutConstraints[] previousConstraints,
                                   LayoutConstraints[] currentConstraints,
                                   Component[] components) {
        if (previousConstraints == null
            || currentConstraints == null
            || components == null
            || previousConstraints.length == 0
            || !(previousConstraints[0] instanceof AbsoluteLayoutSupport.AbsoluteLayoutConstraints)) {
            return;
        }

        int MAX_VALUE = 99999;
        int MIN_VALUE = -99999;

        int[] axisX = new int[previousConstraints.length + 1];
        int[] axisY = new int[previousConstraints.length + 1];
        int[] crossingsX = new int[previousConstraints.length + 1];
        int[] crossingsY = new int[previousConstraints.length + 1];
        int axisXnumber = 1;
        int axisYnumber = 1;

        for (int i = 0; i < axisX.length; i++) {
            axisX[i] = MAX_VALUE;
            axisY[i] = MAX_VALUE;
        }

        // define the most left and right components.
        int minX = MAX_VALUE;
        int maxX = MIN_VALUE;
        int minY = MAX_VALUE;
        int maxY = MIN_VALUE;

        int mostLeft = 0;
        int mostRight = 0;
        int mostTop = 0;
        int mostBottom = 0;

        for (int i = 0; i < components.length; i++) {
            int x = components[i].getBounds().x;
            int x1 = x + components[i].getBounds().width;
            int y = components[i].getBounds().y;
            int y1 = y + components[i].getBounds().height;
            if (x < minX) {
                mostLeft = i;
                minX = x;
            }
            if (x1 > maxX) {
                mostRight = i;
                maxX = x1;
            }
            if (y < minY) {
                mostTop = i;
                minY = y;
            }
            if (y1 > maxY) {
                mostBottom = i;
                maxY = y1;
            }
        }
        // define basic axises, all right axises, but not if it's most right one...
        if (components.length > 1) {
            axisX[0] = MIN_VALUE;
            axisY[0] = MIN_VALUE;
            for (int i = 0; i < components.length; i++) {
                int x1 = components[i].getBounds().x + components[i].getBounds().width;
                if (x1 != maxX) {
                    axisX[axisXnumber] = x1;
                    axisXnumber++;
                }
                int y1 = components[i].getBounds().y + components[i].getBounds().height;
                if (y1 != maxY) {
                    axisY[axisYnumber] = y1;
                    axisYnumber++;
                }
            }
            Arrays.sort(axisX);
            Arrays.sort(axisY);

            // define basic crossings (i.e. number of components which are
            // crossed by an axis); the algorithm is trying to minimize the
            // crossings
            for (int i = 1; i < axisXnumber; i++) {
                crossingsX[i] = getCrossings(components, X_AXIS, axisX[i]);
            }
            for (int i = 1; i < axisYnumber; i++) {
                crossingsY[i] = getCrossings(components, Y_AXIS, axisY[i]);
            }

            // shift basic axis if the number of crossings for new place is lower
            for (int i = 1; i < axisXnumber; i++) {
                for (int j = 0; j < components.length; j++) {
                    if (j != mostLeft) {
                        int x = components[j].getBounds().x;
                        int x1 = x + components[j].getBounds().width;
                        if (x < axisX[i] && x > axisX[i - 1] && crossingsX[i] > getCrossings(components, X_AXIS, x) && x != minX) {
                            axisX[i] = x;
                            crossingsX[i] = getCrossings(components, X_AXIS, x);
                        }
                        if (x1 > axisX[i] && x1 < axisX[i + 1] && crossingsX[i] > getCrossings(components, X_AXIS, x1)) {
                            axisX[i] = x1;
                            crossingsX[i] = getCrossings(components, X_AXIS, x1);
                        }
                    }
                }
            }

            for (int i = 1; i < axisYnumber; i++) {
                for (int j = 0; j < components.length; j++) {
                    if (j != mostTop) {
                        int y = components[j].getBounds().y;
                        int y1 = y + components[j].getBounds().height;
                        if (y < axisY[i] && y > axisY[i - 1] && crossingsY[i] > getCrossings(components, Y_AXIS, y) && y != minY) {
                            axisY[i] = y;
                            crossingsY[i] = getCrossings(components, Y_AXIS, y);
                        }
                        if (y1 > axisY[i] && y1 < axisY[i + 1] && crossingsY[i] > getCrossings(components, Y_AXIS, y1)) {
                            axisY[i] = y1;
                            crossingsY[i] = getCrossings(components, Y_AXIS, y1);
                        }
                    }
                }
            }

            // checking validity of all axis
            // checking if any axis is doubled (2 same axis)
            int removedX = 0;
            for (int i = 1; i < axisXnumber; i++) {
                if (axisX[i] == axisX[i + 1]) {
                    axisX[i] = MAX_VALUE;
                    removedX++;
                }
            }
            if (removedX > 0) {
                Arrays.sort(axisX);
                axisXnumber = axisXnumber - removedX;
            }
            int removedY = 0;
            for (int i = 1; i < axisYnumber; i++) {
                if (axisY[i] == axisY[i + 1]) {
                    axisY[i] = MAX_VALUE;
                    removedY++;
                }
            }
            if (removedY > 0) {
                Arrays.sort(axisY);
                axisYnumber = axisYnumber - removedY;
            }
            // checking if any axis is redundand (i.e. no component is
            // fixing size of this axis)
            int last = axisX[0];
            removedX = 0;
            for (int i = 1; i < axisXnumber; i++) {
                boolean removing = true;
                for (int j = 0; j < components.length; j++) {
                    int x = components[j].getBounds().x;
                    int x1 = x + components[j].getBounds().width;
                    if (x < axisX[i] && x >= last && x1 <= axisX[i]) {
                        removing = false;
                        break;
                    }
                }
                last = axisX[i];
                if (removing) {
                    axisX[i] = MAX_VALUE;
                    removedX++;
                }
            }
            if (removedX > 0) {
                Arrays.sort(axisX);
                axisXnumber = axisXnumber - removedX;
            }
            last = axisY[0];
            removedY = 0;
            for (int i = 1; i < axisYnumber; i++) {
                boolean removing = true;
                for (int j = 0; j < components.length; j++) {
                    int y = components[j].getBounds().y;
                    int y1 = y + components[j].getBounds().height;
                    if (y < axisY[i] && y >= last && y1 <= axisY[i]) {
                        removing = false;
                        break;
                    }
                }
                last = axisY[i];
                if (removing) {
                    axisY[i] = MAX_VALUE;
                    removedY++;
                }
            }
            if (removedY > 0) {
                Arrays.sort(axisY);
                axisYnumber = axisYnumber - removedY;
            }
            // removing most right and bottom axises if they are invalid
            if (axisX[axisXnumber - 1] == maxX) {
                axisXnumber--;
            }
            if (axisY[axisYnumber - 1] == maxY) {
                axisYnumber--;
            }
        }

        // seting first and last axis to proper values (i.e to form size)
        axisX[0] = 0;
        axisX[axisXnumber] = components[0].getParent().getSize().width;
        axisY[0] = 0;
        axisY[axisYnumber] = components[0].getParent().getSize().height;

        // define constraints based on axis
        for (int i = 0; i < components.length; i++) {
            GridBagConstraints cons = new GridBagConstraints();
            int gridX = 0;
            int gridY = 0;
            int gridwidth = 1;
            int gridheight = 1;
            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;
            int x = components[i].getBounds().x;
            int x1 = x + components[i].getBounds().width;
            int y = components[i].getBounds().y;
            int y1 = y + components[i].getBounds().height;
            for (int j = 1; j < axisXnumber + 1; j++) {
                if (x < axisX[j] && x >= axisX[j - 1]) {
                    gridX = j - 1;
                    left = x - axisX[j - 1];
                }
                if (x1 <= axisX[j] && x1 > axisX[j - 1]) {
                    gridwidth = j - gridX;
                    right = axisX[j] - x1;
                }
            }
            for (int j = 1; j < axisYnumber + 1; j++) {
                if (y < axisY[j] && y >= axisY[j - 1]) {
                    gridY = j - 1;
                    top = y - axisY[j - 1];
                }
                if (y1 <= axisY[j] && y1 > axisY[j - 1]) {
                    gridheight = j - gridY;
                    bottom = axisY[j] - y1;
                }
            }
            // checking whether the preffered size must be adjusted
            cons.ipadx = 0;
            cons.ipady = 0;
            if (components[i].getWidth() > 0) {
                cons.ipadx = components[i].getWidth() - components[i].getPreferredSize().width;
            }
            if (components[i].getHeight() > 0) {
                cons.ipady = components[i].getHeight() - components[i].getPreferredSize().height;
            }
            // storing calculated values
            cons.gridx = gridX;
            cons.gridy = gridY;
            cons.gridwidth = gridwidth;
            cons.gridheight = gridheight;
            cons.insets = new Insets(top, left, bottom, right);
            cons.fill = GridBagConstraints.BOTH;

            currentConstraints[i] = new KoalaLayoutSupportConstraints(cons);
        }
    }

    private static int getCrossings(Component[] components, int axis, int value) {
        int number = 0;
        if (axis == X_AXIS) {
            for (int i = 0; i < components.length; i++) {
                int x = components[i].getBounds().x;
                int x1 = x + components[i].getBounds().width;
                if (x < value && x1 > value) {
                    number++;
                }
            }
        } else {
            for (int i = 0; i < components.length; i++) {
                int y = components[i].getBounds().y;
                int y1 = y + components[i].getBounds().height;
                if (y < value && y1 > value) {
                    number++;
                }
            }
        }
        return number;
    }

    /**
     * This method is called to get a default component layout constraints metaobject in case it is not provided (e.g.
     * in addComponents method).
     *
     * @return the default LayoutConstraints object for the supported layout; null if no component constraints are used
     */
    @Override
    protected LayoutConstraints createDefaultConstraints() {
        return new KoalaLayoutSupportConstraints();
    }

    /**
     * Called from createComponentCode method, creates code for a component layout constraints (opposite to
     * readConstraintsCode).
     *
     * @param constrCode CodeGroup to be filled with constraints code
     * @param constr layout constraints metaobject representing the constraints
     * @param compExp CodeExpression object representing the component; not needed here
     * @return created CodeExpression representing the layout constraints
     */
    @Override
    protected CodeExpression createConstraintsCode(CodeGroup constrCode,
                                                   LayoutConstraints constr,
                                                   CodeExpression compExp,
                                                   int index) {
        if (!(constr instanceof KoalaLayoutSupportConstraints)) {
            return null;
        }

        // the code creation is done in KoalaLayoutSupportConstraints
        return ((KoalaLayoutSupportConstraints) constr).createCodeExpression(getCodeStructure(), constrCode);
    }

    /**
     * This method is called from readComponentCode method to read layout constraints of a component from code
     * (GridBagConstraints in this case).
     *
     * @param constrExp CodeExpression object of the constraints (taken from add method in the code)
     * @param constrCode CodeGroup to be filled with the relevant constraints initialization code
     * @param compExp CodeExpression of the component for which the constraints are read (not needed here)
     * @return LayoutConstraints based on information read form code
     */
    @Override
    protected LayoutConstraints readConstraintsCode(CodeExpression constrExp,
                                                    CodeGroup constrCode,
                                                    CodeExpression compExp) {
        KoalaLayoutSupportConstraints constr = new KoalaLayoutSupportConstraints();
        // reading is done in KoalaLayoutSupportConstraints
        constr.readCodeExpression(constrExp, constrCode);
        return constr;
    }

    // -----------------
    private String getContainerName() {
        CodeVariable var = getLayoutContext().getContainerCodeExpression().getVariable();
        return var != null ? var.getName() : null;
    }

    // ---------
    /**
     * Sets up the layout (without adding components) on a real container, according to the internal metadata
     * representation. This method must override AbstractLayoutSupport because TableLayout instance cannot be used
     * universally - new instance must be created for each container.
     *
     * @param container instance of a real container to be set
     * @param containerDelegate effective container delegate of the container; for layout managers we always use
     * container delegate instead of the container
     */
    @Override
    public void setLayoutToContainer(Container container, Container containerDelegate) {
        super.setLayoutToContainer(container, containerDelegate);
    }
    /**
     * This method should calculate layout constraints for a component dragged over a container (or just for mouse
     * cursor being moved over container, without any component). This method is useful for "constraints oriented"
     * layout managers (like e.g. BorderLayout or GridBagLayout).
     *
     * @param container instance of a real container over/in which the component is dragged
     * @param containerDelegate effective container delegate of the container (e.g. like content pane of JFrame)
     * @param component the real component being dragged, can be null
     * @param index position (index) of the component in its current container; -1 if there's no dragged component
     * @param posInCont position of mouse in the container delegate
     * @param posInComp position of mouse in the dragged component; null if there's no dragged component
     * @return new LayoutConstraints object corresponding to the position of the component in the container; may return
     * null if the layout does not use component constraints, or if default constraints should be used
     */
    private Dimension lastSize = null;
    private Point componentPosition = null;

    @Override
    public LayoutConstraints getNewConstraints(Container container,
                                               Container containerDelegate,
                                               Component component,
                                               int index,
                                               Point posInCont,
                                               Point posInComp) {
        if (component != null) {
            lastSize = component.getSize();
        }

        netbeansLog("----- getNewConstraints: ");
        netbeansLog("          container        : " + container);
        netbeansLog("          containerDelegate: " + containerDelegate);
        netbeansLog("          component        : " + component);
        netbeansLog("          index            : " + index);
        netbeansLog("          posInCont        : " + posInCont);
        netbeansLog("          posInComp        : " + posInComp);
        netbeansLog("          last size        : " + lastSize);

        // top left corner of the component
        componentPosition = new Point(posInCont.x - posInComp.x, posInCont.y - posInComp.y);

        KoalaLayout koalaLayout = (KoalaLayout) containerDelegate.getLayout();
        KoalaLayoutSupportConstraints koalaConstraints = (KoalaLayoutSupportConstraints) getConstraints(index);
        Insets koalaConstraintsInsets = (koalaConstraints != null ? koalaConstraints.getInsets() : new Insets(0, 0, 0, 0));

        // top left corner of the component including the koalalayout insets
        int topLeftPosX = componentPosition.x - koalaConstraintsInsets.left;
        int topLeftPosY = componentPosition.y - koalaConstraintsInsets.top;
        Point topLeftPos = new Point(topLeftPosX, topLeftPosY);
        Point topLeftGridCoords = determineGridCoordsForPosition(topLeftPos, containerDelegate, koalaLayout);
        netbeansLog("          X grid pos       : " + topLeftGridCoords.x);
        netbeansLog("          Y grid pos       : " + topLeftGridCoords.y);

        // bottom right position including the koala insets
        int bottomRightPosX = componentPosition.x + lastSize.width + koalaConstraintsInsets.right;
        int bottomRightPosY = componentPosition.y + lastSize.height + koalaConstraintsInsets.bottom;
        Point bottomRightPos = new Point(bottomRightPosX, bottomRightPosY);
        Point brGridCoords = determineGridCoordsForPosition(bottomRightPos, containerDelegate, koalaLayout);
        int defaultWidth = Math.max(brGridCoords.x - topLeftGridCoords.x, 1);
        int defaultHeight = Math.max(brGridCoords.y - topLeftGridCoords.y, 1);

        int gridWidth = (koalaConstraints != null ? koalaConstraints.getGridWidth() : defaultWidth);
        int gridHeight = (koalaConstraints != null ? koalaConstraints.getGridHeight() : defaultHeight);
        int koalaConstraintsFill = (koalaConstraints != null ? koalaConstraints.getFill() : GridBagConstraints.BOTH);
        int koalaConstraintsAnchor = (koalaConstraints != null ? koalaConstraints.getAnchor() : GridBagConstraints.CENTER);

        return new KoalaLayoutSupportConstraints(topLeftGridCoords.x,
                                                 topLeftGridCoords.y,
                                                 gridWidth,
                                                 gridHeight,
                                                 koalaConstraintsInsets,
                                                 koalaConstraintsFill,
                                                 koalaConstraintsAnchor);
    }

    /**
     * This method should calculate layout constraints for a component being resized.
     *
     * @param container instance of a real container in which the component is resized
     * @param containerDelegate effective container delegate of the container (e.g. like content pane of JFrame)
     * @param component real component being resized
     * @param index position of the component in its container
     * @param sizeChanges Insets object with size differences
     * @param posInCont position of mouse in the container delegate
     * @return component layout constraints for resized component; null if resizing is not possible or not implemented
     */
    @Override
    public LayoutConstraints getResizedConstraints(Container container, Container containerDelegate, Component component, int index, Rectangle originalBounds, Insets sizeChanges, Point posInCont) {
        netbeansLog("----- getResizedConstraints: ");
        netbeansLog("          container        : " + container);
        netbeansLog("          containerDelegate: " + containerDelegate);
        netbeansLog("          component        : " + component);
        netbeansLog("          index            : " + index);
        netbeansLog("          originalBounds   : " + originalBounds);
        netbeansLog("          sizeChanges      : " + sizeChanges);
        netbeansLog("          posInCont        : " + posInCont);

        // top left corner of the component
        componentPosition = new Point(originalBounds.x - sizeChanges.left, originalBounds.y - sizeChanges.top);

        KoalaLayout koalaLayout = (KoalaLayout) containerDelegate.getLayout();
        KoalaLayoutSupportConstraints koalaConstraints = (KoalaLayoutSupportConstraints) getConstraints(index);
        Insets koalaConstraintsInsets = (koalaConstraints != null ? koalaConstraints.getInsets() : new Insets(0, 0, 0, 0));

        // top left corner of the component including the koalalayout insets
        int topLeftPosX = componentPosition.x - koalaConstraintsInsets.left;
        int topLeftPosY = componentPosition.y - koalaConstraintsInsets.top;
        Point topLeftPos = new Point(topLeftPosX, topLeftPosY);
        Point topLeftGridCoords = determineGridCoordsForPosition(topLeftPos, containerDelegate, koalaLayout);
        netbeansLog("          X top left grid pos       : " + topLeftGridCoords.x);
        netbeansLog("          Y top left grid pos       : " + topLeftGridCoords.y);

        // Calc width and height changes
        int bottomRightX = originalBounds.x + originalBounds.width + sizeChanges.right + koalaConstraintsInsets.right;
        int bottomRightY = originalBounds.y + originalBounds.height + sizeChanges.bottom + koalaConstraintsInsets.bottom;
        Point bottomRightPoint = new Point(bottomRightX, bottomRightY);
        Point bottomRightGridCoords = determineGridCoordsForPosition(bottomRightPoint, containerDelegate, koalaLayout);
        netbeansLog("          bottom right point        : " + bottomRightPoint);
        netbeansLog("          X bottom right grid pos   : " + bottomRightGridCoords.x);
        netbeansLog("          Y bottom right grid pos   : " + bottomRightGridCoords.y);

        int gridWidth = bottomRightGridCoords.x - topLeftGridCoords.x;
        int gridHeight = bottomRightGridCoords.y - topLeftGridCoords.y;
        netbeansLog("          Component width           : " + gridWidth);
        netbeansLog("          Component height          : " + gridHeight);

        int koalaConstraintsFill = (koalaConstraints != null ? koalaConstraints.getFill() : GridBagConstraints.BOTH);
        int koalaConstraintsAnchor = (koalaConstraints != null ? koalaConstraints.getAnchor() : GridBagConstraints.CENTER);

        KoalaLayoutSupportConstraints newConstraints = new KoalaLayoutSupportConstraints(topLeftGridCoords.x,
                                                                                         topLeftGridCoords.y,
                                                                                         gridWidth,
                                                                                         gridHeight,
                                                                                         koalaConstraintsInsets,
                                                                                         koalaConstraintsFill,
                                                                                         koalaConstraintsAnchor);
        return newConstraints;
    }

    /**
     * This method should paint a feedback for a component dragged over a container (or just for mouse cursor being
     * moved over container, without any component). In principle, it should present given component layout constraints
     * or index graphically.
     *
     * @param container instance of a real container over/in which the component is dragged
     * @param containerDelegate effective container delegate of the container (e.g. like content pane of JFrame) - here
     * the feedback is painted
     * @param component the real component being dragged, can be null
     * @param newConstraints component layout constraints to be presented
     * @param newIndex component's index position to be presented (if newConstraints == null)
     * @param g Graphics object for painting (with color and line style set)
     * @return whether any feedback was painted (may return false if the constraints or index are invalid, or if the
     * painting is not implemented)
     */
    @Override
    public boolean paintDragFeedback(Container container,
                                     Container containerDelegate,
                                     Component component,
                                     LayoutConstraints newConstraints,
                                     int newIndex,
                                     Graphics g) {
        netbeansLog("----- paintDragFeedback: ");
        netbeansLog("          container        : " + container);
        netbeansLog("          containerDelegate: " + containerDelegate);
        netbeansLog("          component        : " + component);
        netbeansLog("          newConstraints   : " + newConstraints.getConstraintsObject());
        netbeansLog("          newIndex         : " + newIndex);
        netbeansLog("          component size   : " + component.getSize());

        KoalaLayout koalaLayout = (KoalaLayout) containerDelegate.getLayout();
        KoalaLayoutSupportConstraints koalaConstraints = (KoalaLayoutSupportConstraints) newConstraints;

        Dimension containerSize = containerDelegate.getSize();
        int containerWidth = containerSize.width;
        int containerHeight = containerSize.height;
        int cellWidth = containerWidth / koalaLayout.getGridwidth();
        int cellHeight = containerHeight / koalaLayout.getGridheight();
        netbeansLog("          grid width       : " + koalaLayout.getGridwidth());
        netbeansLog("          grid height      : " + koalaLayout.getGridheight());

        Rectangle sRect = new Rectangle(-1, -1, 0, 0);

        Insets insets = containerDelegate.getInsets();
        int viewY = insets.top;
        int viewHeight = containerDelegate.getHeight() - insets.top - insets.bottom;
        int viewX = insets.left;
        int viewWidth = containerDelegate.getWidth() - insets.left - insets.right;

        Graphics2D g2d = (Graphics2D) g.create();
        BasicStroke initialStroke = (BasicStroke) g2d.getStroke();

        // Smaller stroke required when cells are small
        if ((cellHeight <= 10) || (cellWidth <= 10)) {
            g2d.setStroke(new BasicStroke(1.0f));
        }

        // component area feedback is not painted when ther are no insets
        Insets ins = koalaConstraints.getInsets();
        if ((ins.top > 0) || (ins.left > 0) || (ins.right > 0) || (ins.bottom > 0)) {
            Color c = g.getColor();
            g2d.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            g2d.fillRect(componentPosition.x - ins.left,
                         componentPosition.y - ins.top,
                         component.getWidth() + ins.right + ins.left,
                         component.getHeight() + ins.top + ins.bottom);

            // restore color
            g2d.setColor(c);
        }

        // Draw grid cols
        int x = viewX;
        int gridWidth = koalaLayout.getGridwidth();
        for (int i = 0; i < gridWidth; i++) {
            x = viewX + (i * viewWidth) / gridWidth;
            if (i == koalaConstraints.getGridX()) {
                sRect.x = x;
            }
            g2d.drawLine(x, viewY, x, viewHeight);
        }
        g2d.drawLine(viewX + viewWidth, viewY, viewX + viewWidth, viewHeight);

        // Draw grid rows
        int y = viewY;
        int gridHeight = koalaLayout.getGridheight();
        for (int i = 0; i < gridHeight; i++) {
            y = viewY + (i * viewHeight) / gridHeight;
            if (i == koalaConstraints.getGridY()) {
                sRect.y = y;
            }
            g2d.drawLine(viewX, y, viewWidth - 1, y);
        }
        g2d.drawLine(viewX, viewY + viewHeight, viewWidth - 1, viewY + viewHeight);

        // draw feedback
        g2d.setStroke(initialStroke);
        g2d.setColor(Color.RED);
        sRect.width = (koalaConstraints.getGridWidth() * viewWidth) / gridWidth;
        sRect.height = (koalaConstraints.getGridHeight() * viewHeight) / gridHeight;
        g2d.drawRect(sRect.x, sRect.y, sRect.width, sRect.height);

        // draw info labels
        Color boxColor = new Color(155, 191, 225, 180);
        Color fontColor = Color.BLACK;
        Font oriFont = g2d.getFont();

        g2d.setFont(oriFont.deriveFont(Font.BOLD, 12.0f));
        FontMetrics fm = g2d.getFontMetrics();

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String gridLabel = " Grid: " + gridWidth + "x" + gridHeight + " ";
        Rectangle2D gridStringBounds = fm.getStringBounds(gridLabel, g2d);
        RoundRectangle2D.Double gridRect = new RoundRectangle2D.Double((containerWidth - gridStringBounds.getWidth()) / 2,
                                                                       containerHeight + 7,
                                                                       gridStringBounds.getWidth(),
                                                                       gridStringBounds.getHeight() + 1,
                                                                       10.0f, 10.0f);

        g2d.setColor(boxColor.darker());
        g2d.fill(gridRect);
        g2d.setColor(Color.WHITE);
        g2d.drawString(gridLabel, (int) gridRect.getX(), (int) gridRect.getY() + fm.getAscent());


        g2d.setFont(oriFont.deriveFont(10.0f));
        fm = g2d.getFontMetrics();

        String topLeftLabel = " x: " + koalaConstraints.getGridX() + ", y: " + koalaConstraints.getGridY() + " ";
        Rectangle2D tlStringBounds = fm.getStringBounds(topLeftLabel, g2d);
        RoundRectangle2D.Double topLeftRect = new RoundRectangle2D.Double(sRect.x,
                                                                          sRect.y - tlStringBounds.getHeight() - 3,
                                                                          tlStringBounds.getWidth(),
                                                                          tlStringBounds.getHeight() + 1,
                                                                          10.0f, 10.0f);

        g2d.setColor(boxColor);
        g2d.fill(topLeftRect);
        g2d.setColor(fontColor);
        g2d.drawString(topLeftLabel, (int) topLeftRect.getX(), (int) topLeftRect.getY() + fm.getAscent());

        String widhtHeightLabel = " w: " + koalaConstraints.getGridWidth() + ", h: " + koalaConstraints.getGridHeight() + " ";
        Rectangle2D whStringBounds = fm.getStringBounds(widhtHeightLabel, g2d);
        RoundRectangle2D.Double widthHeighRect = new RoundRectangle2D.Double(sRect.x + (sRect.width - whStringBounds.getWidth()) / 2,
                                                                             sRect.y + sRect.height + 2,
                                                                             whStringBounds.getWidth(),
                                                                             whStringBounds.getHeight() + 1,
                                                                             10.0f, 10.0f);

        g2d.setColor(boxColor);
        g2d.fill(widthHeighRect);
        g2d.setColor(fontColor);
        g2d.drawString(widhtHeightLabel, (int) widthHeighRect.getX(), (int) widthHeighRect.getY() + fm.getAscent());


        g2d.dispose();
        return true;
    }

    /**
     * Provides resizing options for given component. It can combine the bit-flag constants RESIZE_UP, RESIZE_DOWN,
     * RESIZE_LEFT, RESIZE_RIGHT.
     *
     * @param container instance of a real container in which the component is to be resized
     * @param containerDelegate effective container delegate of the container (e.g. like content pane of JFrame)
     * @param component real component to be resized
     * @param index position of the component in its container
     * @return resizing options for the component; 0 if no resizing is possible
     */
    @Override
    public int getResizableDirections(Container container,
                                      Container containerDelegate,
                                      Component component,
                                      int index) {
        KoalaLayout koalaLayout = (KoalaLayout) containerDelegate.getLayout();
        Point gridCoords = determineGridCoordsForPosition(component.getLocation(), containerDelegate, koalaLayout);

        // The layout allows a component to be outside the left and bottom borders
        // It will expend accordingly.
        int resizingDirections = RESIZE_RIGHT | RESIZE_DOWN;
        if (gridCoords.x >= 0) {
            resizingDirections |= RESIZE_LEFT;
        }
        if (gridCoords.y >= 0) {
            resizingDirections |= RESIZE_UP;
        }

        return resizingDirections;
    }

    /**
     * Determines grid coodinates for x y cursor position.
     *
     *
     * @param position
     * @param container
     * @param koalaLayout
     * @param koalaLayoutSupportConstraints
     * @param isTopLeft
     * @return
     */
    private Point determineGridCoordsForPosition(Point position, Container container, KoalaLayout koalaLayout) {

        Insets insets = container.getInsets();
        int viewY = insets.top;
        int viewHeight = container.getHeight() - insets.top - insets.bottom;
        int viewX = insets.left;
        int viewWidth = container.getWidth() - insets.left - insets.right;

        int xLimit = viewX;
        int prevX = xLimit;
        int gridWidth = koalaLayout.getGridwidth();
        int cellWidth = 0;

        int gridX = 0;
        int xPos = 0;
        int x = 0;
        for (int i = 0; i < gridWidth; i++) {
            x = viewX + (i * viewWidth) / gridWidth;
            cellWidth = x - prevX;
            xPos = position.x + cellWidth / 2;
            if (xPos >= x) {
                gridX = i;
            }

            if (xPos < x) {
                break;
            }

            prevX = x;
        }

        // limit case for the last cell
        if (xPos >= viewX + viewWidth) {
            gridX++;
        }

        int y = viewY;
        int prevY = y;
        int gridHeight = koalaLayout.getGridheight();
        int cellHeight = 0;

        int yPos = 0;
        int gridY = 0;
        for (int i = 0; i < gridHeight; i++) {
            y = viewY + (i * viewHeight) / gridHeight;
            cellHeight = y - prevY;
            yPos = position.y + cellHeight / 2;
            if (yPos >= y) {
                gridY = i;
            }

            if (yPos < y) {
                break;
            }

            prevY = y;
        }
        if (yPos >= viewY + viewHeight) {
            gridY++;
        }

        return new Point(gridX, gridY);
    }

    // CustomizerDialog - a non-modal dialog holding the customizer itself.
    // Some tweaks are required to obtain a non-modal dialog behaving like
    // a floating window reasonably.
    private JDialog createCustomizerDialog() {
        Frame dialogOwner = null;
        Point designLocation = null;

        Component activeComp =
                  org.openide.windows.TopComponent.getRegistry().getActivated();
        if (activeComp != null) {
            Component comp = activeComp.getParent();
            while (comp != null) {
                if (comp instanceof Frame) {
                    dialogOwner = (Frame) comp;
                    break;
                }
                comp = comp.getParent();
            }
        }

        if (dialogOwner != null) {
            designLocation = activeComp.getLocation();
            SwingUtilities.convertPointToScreen(designLocation, activeComp.getParent());
        } else {
            dialogOwner = org.openide.windows.WindowManager.getDefault().getMainWindow();
        }

        String title = getContainerName();
        title = (title != null ? title : "Form") + " -> " + "koalaLayout";

        JDialog dialog = new CustomizerDialog(dialogOwner, title, false);
        dialog.getContentPane().add(layoutCustomizer);
        dialog.pack();

        if (designLocation != null) {
            designLocation.x -= dialog.getWidth() + 1;
            if (designLocation.x < 0) {
                designLocation.x = 0;
            }
            if (designLocation.y < 0) {
                designLocation.y = 0;
            }
            dialog.setLocation(designLocation);
        }

        return dialog;
    }

    private class CustomizerDialog extends JDialog {

        CustomizerDialog(Frame owner, String title, boolean modal) {
            super(owner, title, modal);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }

        @Override
        public void setVisible(boolean value) {
            super.setVisible(value);
            if (value == false && layoutCustomizer != null) {
                layoutCustomizer = null;
                customizerDialog = null;
            }
        }
    }

    // ------
    // temporary hacks for GridBagCustomizer and GridBagControlCenter
    protected static ResourceBundle getBundle() {
        return org.openide.util.NbBundle.getBundle(KoalaLayoutSupport.class);
    }

    static ResourceBundle getBundleHack() {
        return getBundle(); // from AbstractLayoutSupport
    }

    LayoutSupportContext getLayoutSupportHack() {
        return super.getLayoutContext();
    }

    /**
     * Creates code structures for a new layout manager (opposite to readInitLayoutCode).
     * As the Koalalayout is a bean, we can call super.createInitLayoutCode
     *
     * @param initLayoutCode CodeGroup to be filled with relevant initialization code
     * @return created CodeExpression representing the layout manager
     */
    @Override
    protected CodeExpression createInitLayoutCode(CodeGroup initLayoutCode) {
        netbeansLog("Creating init layout code!");
        return super.createInitLayoutCode(initLayoutCode);
    }

    /**
     * This method is called from readLayoutCode to read the layout manager bean code (i.e. code for constructor and
     * properties).
     *
     * @param layoutExp CodeExpressin of the layout manager
     * @param initLayoutCode CodeGroup to be filled with relevant initialization code
     */
    @Override
    protected void readInitLayoutCode(CodeExpression codeExpression, CodeGroup layoutCode) {
        netbeansLog("reading init layout code...");
        CodeVariable var = codeExpression.getVariable();
        try {
            layoutCode.addStatement(0, var.getAssignment(codeExpression));
        } catch (Exception e) {
            System.err.println("*********************");
            System.err.println("Layout information missing in form, do not panic and save your job !");
            System.err.println("*********************");
        }
        Iterator it = CodeStructure.getDefinedStatementsIterator(codeExpression);
        FormProperty[] properties = getAllProperties();
        try {
            netbeansLog("(before reading) properties listing begin");
            for (int i = 0; i < properties.length; i++) {
                netbeansLog("    properties[" + i + "]: " + properties[i].getName() + "=" + properties[i].getValue());
            }
            netbeansLog("(before reading) properties listing end");
        } catch (Exception e) {
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, e);
        }
        while (it.hasNext()) {
            CodeStatement statement = (CodeStatement) it.next();
            if (isMethod(statement, KoalaLayoutIntrospector.getSetGridheightMethod())) {
                FormCodeSupport.readPropertyStatement(statement, properties[1], true);
                traceReadStatement(statement);
            }
            if (isMethod(statement, KoalaLayoutIntrospector.getSetGridwidthMethod())) {
                FormCodeSupport.readPropertyStatement(statement, properties[2], true);
                traceReadStatement(statement);
            }
            layoutCode.addStatement(statement);
        }
        try {
            netbeansLog("(after reading) properties listing begin");
            for (int i = 0; i < properties.length; i++) {
                netbeansLog("    properties[" + i + "]: " + properties[i].getName() + "=" + properties[i].getValue());
            }
            netbeansLog("(after reading) properties listing end");
        } catch (Exception e) {
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, e);
        }

        //        updateLayoutInstance();
        netbeansLog("init layout code read");
    }

    private void traceReadStatement(CodeStatement statement) {
        Object obj = statement.getMetaObject();
        if (obj != null && obj instanceof Method) {
            Method other = (Method) obj;
            netbeansLog(other.getName() + " statement read");
        }
    }

    public boolean isMethod(CodeStatement statement, Method method) {
        Object obj = statement.getMetaObject();
        if (obj != null && obj instanceof Method) {
            Method other = (Method) obj;
            // Compare class names only since classes can be loaded by different ClassLoaders

            netbeansLog("Comparing method class'" + method.getDeclaringClass().getName() + "' with other '" + other.getDeclaringClass().getName() + "'");

            netbeansLog("and comparing method name'" + method.getName() + "' with other '" + other.getName() + "'");
            if ((method.getDeclaringClass().getName().equals(other.getDeclaringClass().getName())) && (method.getName().equals(other.getName()))) {
                if (!method.getReturnType().equals(other.getReturnType())) {
                    return false;
                }
                Class[] params1 = method.getParameterTypes();
                Class[] params2 = other.getParameterTypes();
                if (params1.length == params2.length) {
                    for (int i = 0; i < params1.length; i++) {
                        if (params1[i] != params2[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
