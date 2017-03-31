/*
 * This file is part of KoalaLayout.
 *
 * Koala layout is a flexible grid layout for Java Swing.
 * Copyright (C) 2012 Peter Flukiger, Jean-Marc Borer
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
package net.java.dev.koalalayout;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author flukiger
 * @version 1.0.1
 */
public class KoalaLayout implements LayoutManager2, Serializable {

    /**
     * Just for NetBeans module debugging (switch boolean to log or not)
     */
    private static final boolean LOGGABLE = false;
    /**
     * associates components with their constraints
     *
     */
    protected Map<Component, GridBagConstraints> componentTable;
    protected GridBagConstraints defaultConstraints;
    protected int gridWidth;
    protected int gridHeight;

    private void netbeansLog(String msg) {
        if (LOGGABLE) {
            System.err.println(msg);
        }
    }

    /**
     * Creates new KoalaLayout
     */
    public KoalaLayout() {
        this(1, 1);
    }

    /**
     * Creates new KoalaLayout
     *
     * @param gridWidth initial cell dimension in x
     * @param gridHeight initial cell dimension in y
     *
     */
    public KoalaLayout(int gridWidth, int gridHeight) {
        if (gridWidth <= 0 || gridHeight <= 0) {
            throw new IllegalArgumentException("cannot create KoalaLayout: width and height must be positive");
        }
        componentTable = new HashMap<Component, GridBagConstraints>();
        defaultConstraints = new GridBagConstraints();
        defaultConstraints.gridx = 0;
        defaultConstraints.gridy = 0;
        defaultConstraints.gridwidth = 1;
        defaultConstraints.gridheight = 1;
        defaultConstraints.fill = GridBagConstraints.BOTH;
        defaultConstraints.anchor = GridBagConstraints.CENTER;

        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        netbeansLog("creating koalalayout with width=" + gridWidth + ", height=" + gridHeight);
        netbeansLog("ThisClass from: " + KoalaLayout.class.getClassLoader());
        netbeansLog("----------------");
    }

    /**
     * Sets the constraints for the specified component in this layout.
     *
     * @param comp the component to be modified.
     * @param constraints the constraints to be applied.
     */
    public void setConstraints(Component comp, GridBagConstraints constraints) {
        constraints = (GridBagConstraints) constraints.clone(); // make local copy
        componentTable.put(comp, constraints);
    }

    /**
     * Gets the constraints for the specified component. A copy of the actual
     * <code>GridBagConstraints</code> object is returned.
     *
     * @param comp the component to be queried.
     * @return the constraint for the specified component in this grid bag layout; a copy of the actual constraint
     * object is returned.
     */
    public GridBagConstraints getConstraints(Component comp) {
        GridBagConstraints constraints = (GridBagConstraints) componentTable.get(comp);
        if (constraints == null) {
            setConstraints(comp, defaultConstraints);
            constraints = (GridBagConstraints) componentTable.get(comp);
        }
        return (GridBagConstraints) constraints.clone();
    }

    /**
     * Retrieves the constraints for the specified component. The return value is not a copy, but is the actual
     * <code>GridBagConstraints</code> object used by the layout mechanism.
     *
     * @param comp the component to be queried
     * @return the constraints for the specified component.
     */
    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints constraints = (GridBagConstraints) componentTable.get(comp);
        if (constraints == null) {
            setConstraints(comp, defaultConstraints);
            constraints = (GridBagConstraints) componentTable.get(comp);
        }
        return constraints;
    }

    /**
     * Removes the constraints for the specified component in this layout
     *
     * @param comp the component to be modified.
     */
    private void removeConstraints(Component comp) {
        componentTable.remove(comp);
    }

    /**
     * Returns the maximum dimensions for this layout given the components in the specified target container.
     *
     * @param target the component which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize(Container)
     * @see #preferredLayoutSize(Container)
     */
    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Invalidates the layout, indicating that if the layout manager has cached information it should be discarded.
     */
    @Override
    public void invalidateLayout(Container target) {
        // Nothing to do here
    }

    /**
     * Returns the alignment along the x axis. This specifies how the component would like to be aligned relative to
     * other components. The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1
     * is aligned the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param parent
     * @return
     */
    @Override
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /**
     * Adds the specified component to the layout, using the specified constraint object.
     *
     * @param comp the component to be added.
     * @param constraints an object that determines how the component is added to the layout.
     */
    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof GridBagConstraints) {
            setConstraints(comp, (GridBagConstraints) constraints);
        } else if (constraints != null) {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a GridBagConstraints");
        }
    }

    /**
     * Returns the alignment along the y axis. This specifies how the component would like to be aligned relative to
     * other components. The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1
     * is aligned the furthest away from the origin, 0.5 is centered, etc.
     */
    @Override
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /**
     * Determines the preferred size of the
     * <code>target</code> container using this KoalaLayout.
     * <p>
     * Most applications do not call this method directly.
     *
     * @return preferred size of the component that uses this layout
     * @param parent the container in which to do the layout.
     * @see java.awt.Container#getPreferredSize
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            int maxX = 0;
            int maxY = 0;
            Component children[] = parent.getComponents();
            for (int i = 0; i < children.length; i++) {
                Dimension dim = children[i].getPreferredSize();
                GridBagConstraints constraints = lookupConstraints(children[i]);
                int x = constraints.gridwidth > 0 ? dim.width / constraints.gridwidth : 1;
                int y = constraints.gridheight > 0 ? dim.height / constraints.gridheight : 1;

                // take into account possible insets
                x += constraints.insets.left + constraints.insets.right;
                y += constraints.insets.top + constraints.insets.bottom;
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }

            Dimension preferredDim = new Dimension(maxX * gridWidth, maxY * gridHeight);

            Insets insets = parent.getInsets();
            if (insets != null) {
                preferredDim.width += insets.left + insets.right;
                preferredDim.height += insets.top + insets.bottom;
            }

            return preferredDim;
        }
    }

    /**
     * Determines the minimum size of the
     * <code>target</code> container using this grid bag layout.
     * <p>
     * Most applications do not call this method directly.
     *
     * @return minimum size that might be accepted by a component that uses this layout
     * @param parent the container in which to do the layout.
     * @see java.awt.Container#doLayout
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            int maxX = 0;
            int maxY = 0;
            Component children[] = parent.getComponents();
            for (int i = 0; i < children.length; i++) {
                Dimension d = children[i].getMinimumSize();
                GridBagConstraints c = lookupConstraints(children[i]);
                int x = c.gridwidth > 0 ? ((c.gridx + c.gridwidth) * d.width) / c.gridwidth : 1;
                int y = c.gridheight > 0 ? ((c.gridy + c.gridheight) * d.height) / c.gridheight : 1;
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
            return new Dimension(maxX, maxY);
        }
    }

    /**
     * Removes the specified component from this layout.
     * <p>
     * Most applications do not call this method directly.
     *
     * @param comp the component to be removed.
     * @see java.awt.Container#remove(java.awt.Component)
     * @see java.awt.Container#removeAll()
     */
    @Override
    public void removeLayoutComponent(Component comp) {
        removeConstraints(comp);
    }

    /**
     * Adds the specified component with the specified name to the layout.
     *
     * @param name the name of the component.
     * @param comp the component to be added.
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Lays out the specified container using this grid bag layout. This method reshapes components in the specified
     * container in order to satisfy the contraints of this <code>GridBagLayout</code> object.
     * <p>
     * Most applications do not call this method directly.
     * <p>
     * To avoid rounding issues, a grid is precalculated and used during the layout of the components.
     * This ensures that contiguous components have no single pixel gap due to rounding issues.
     *
     * @param parent the container in which to do the layout.
     * @see java.awt.Container
     * @see java.awt.Container#doLayout
     */
    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {

            // first, determine gridWidth and gridHeight
            int startx = 0;
            int starty = 0;
            Component children[] = parent.getComponents();
            for (int i = 0; i < children.length; i++) {
                GridBagConstraints c = lookupConstraints(children[i]);
                int gridx = c.gridx == GridBagConstraints.RELATIVE ? startx : c.gridx;
                int gridy = c.gridy == GridBagConstraints.RELATIVE ? starty : c.gridy;
                int gridw = c.gridwidth == GridBagConstraints.REMAINDER ? 1 : c.gridwidth;
                int gridh = c.gridheight == GridBagConstraints.REMAINDER ? 1 : c.gridheight;
                startx = gridx + gridw;
                starty = gridy + gridh;
                if (startx > gridWidth) {
                    gridWidth = startx;
                }
                if (starty > gridHeight) {
                    gridHeight = starty;
                }
            }

            Insets insets = parent.getInsets();
            int viewY = insets.top;
            int viewHeight = parent.getHeight() - insets.top - insets.bottom;
            int viewX = insets.left;
            int viewWidth = parent.getWidth() - insets.left - insets.right;

            Point coords[][] = new Point[gridWidth + 1][gridHeight + 1];
            for (int i = 0; i <= gridWidth; i++) {
                for (int j = 0; j <= gridHeight; j++) {
                    int x = (i * viewWidth) / gridWidth;
                    int y = (j * viewHeight) / gridHeight;
                    coords[i][j] = new Point(x, y);
                }
            }

            startx = starty = 0;
            for (int i = 0; i < children.length; i++) {
                Component comp = children[i];
                GridBagConstraints c = lookupConstraints(comp);
                int gridx = c.gridx == GridBagConstraints.RELATIVE ? startx : c.gridx;
                int gridy = c.gridy == GridBagConstraints.RELATIVE ? starty : c.gridy;
                int gridw = c.gridwidth == GridBagConstraints.REMAINDER ? gridWidth - gridx : c.gridwidth;
                int gridh = c.gridheight == GridBagConstraints.REMAINDER ? gridHeight - gridy : c.gridheight;
                startx = gridx + gridw;
                starty = gridy + gridh;

                int cellX = viewX + coords[gridx][gridy].x + c.insets.left;
                int cellY = viewY + coords[gridx][gridy].y + c.insets.top;
                int cellWidth = coords[gridx + gridw][gridy].x - coords[gridx][gridy].x - c.insets.left - c.insets.right;
                int cellHeight = coords[gridx][gridy + gridh].y - coords[gridx][gridy].y - c.insets.top - c.insets.bottom;

                int width;
                int height;

                // Apply fill constraints
                Dimension prefDims = comp.getPreferredSize();
                switch (c.fill) {
                    case GridBagConstraints.NONE:
                        // use preferred size if possible
                        width = Math.min(prefDims.width, cellWidth);
                        height = Math.min(prefDims.height, cellHeight);
                        break;
                    case GridBagConstraints.HORIZONTAL:
                        // horizontal -> use all cell space horizontally use preferred hight if smaller than cell size
                        width = cellWidth;
                        height = Math.min(prefDims.height, cellHeight);
                        break;
                    case GridBagConstraints.VERTICAL:
                        // verticall -> use all cell space vertically use preferred width if smaller than cell size
                        width = Math.min(prefDims.width, cellWidth);
                        height = cellHeight;
                        break;
                    case GridBagConstraints.BOTH:
                    default:
                        // fill both -> use all space
                        width = cellWidth;
                        height = cellHeight;
                }

                int x;
                int y;
                switch (c.anchor) {
                    case GridBagConstraints.NORTH:
                        x = cellX + (cellWidth - width) /2;
                        y = cellY;
                        break;
                    case GridBagConstraints.NORTHEAST:
                        x = cellX + (cellWidth - width);
                        y = cellY;
                        break;
                    case GridBagConstraints.EAST:
                        x = cellX + (cellWidth - width);
                        y = cellY + (cellHeight - height) / 2;
                        break;
                    case GridBagConstraints.SOUTHEAST:
                        x = cellX + (cellWidth - width);
                        y = cellY + (cellHeight - height);
                        break;
                    case GridBagConstraints.SOUTH:
                        x = cellX + (cellWidth - width) /2;
                        y = cellY + (cellHeight - height);
                        break;
                    case GridBagConstraints.SOUTHWEST:
                        x = cellX;
                        y = cellY + (cellHeight - height);
                        break;
                    case GridBagConstraints.WEST:
                        x = cellX;
                        y = cellY + (cellHeight - height) / 2;
                        break;
                    case GridBagConstraints.NORTHWEST:
                        x = cellX;
                        y = cellY;
                        break;
                    case GridBagConstraints.CENTER:
                    default:
                        x = cellX + (cellWidth - width) / 2;
                        y = cellY + (cellHeight - height) / 2;
                }

                Rectangle bounds = comp.getBounds();
                if ((bounds.x != x) || (bounds.y != y) || (bounds.width != width) || (bounds.height != height)) {
                    comp.setBounds(x, y, width, height);
                }
            }
        }
    }

    public void setGridheight(int gridheight) {
        this.gridHeight = gridheight;
        netbeansLog("setting gridheight from KoalaLayout from classLoader " + this.getClass().getClassLoader() + ": " + this.gridHeight);
    }

    public int getGridheight() {
        netbeansLog("getting gridheight from KoalaLayout from classLoader " + this.getClass().getClassLoader() + ": " + this.gridHeight);
        return this.gridHeight;
    }

    public void setGridwidth(int gridwidth) {
        this.gridWidth = gridwidth;
        netbeansLog("setting gridwidth from KoalaLayout from classLoader " + this.getClass().getClassLoader() + ": " + this.gridWidth);
    }

    public int getGridwidth() {
        netbeansLog("getting gridwidth from KoalaLayout from classLoader " + this.getClass().getClassLoader() + ": " + this.gridWidth);
        return this.gridWidth;
    }
}
