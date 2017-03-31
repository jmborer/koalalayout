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
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.openide.ErrorManager;

/**
 * Utility class to introspect the KoalaLayout
 *
 * @author Jean-Marc Borer
 */
public class KoalaLayoutIntrospector {

    private static Constructor constrConstructor;
    private static Constructor layoutConstructor;
    private static Method setGridwidthMethod;
    private static Method setGridheightMethod;

    private KoalaLayoutIntrospector() {
    }

    public static Constructor getConstraintsConstructor() {
        if (constrConstructor == null) {
            try {
                constrConstructor = GridBagConstraints.class.getConstructor(
                        new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
                    Double.TYPE, Double.TYPE,
                    Insets.class,
                    Integer.TYPE, Integer.TYPE
                });
            } catch (NoSuchMethodException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return constrConstructor;
    }

    public static Constructor getLayoutConstructor() {
        if (layoutConstructor == null) {
            try {
                layoutConstructor = KoalaLayout.class.getConstructor(new Class[0]);
            } catch (NoSuchMethodException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return layoutConstructor;
    }

    public static Method getSetGridwidthMethod() {
        if (setGridwidthMethod == null) {
            try {
                setGridwidthMethod = KoalaLayout.class.getMethod("setGridwidth", new Class[]{Integer.TYPE});
            } catch (NoSuchMethodException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return setGridwidthMethod;
    }

    public static Method getSetGridheightMethod() {
        if (setGridheightMethod == null) {
            try {
                setGridheightMethod = KoalaLayout.class.getMethod("setGridheight", new Class[]{Integer.TYPE});
            } catch (NoSuchMethodException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return setGridheightMethod;
    }
}
