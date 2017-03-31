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

import org.openide.modules.ModuleInstall;

import org.netbeans.modules.form.layoutsupport.LayoutSupportRegistry;

/**
 * Manages a module's life cycle.
 * Remember that an installer is optional and often not needed at all.
 *
 * @author Jean-Marc Borer
 */
public class KoalaLayoutModule extends ModuleInstall {

    @Override
    public void restored() {
        LayoutSupportRegistry.registerSupportForLayout(net.java.dev.koalalayout.KoalaLayout.class.getName(),
                net.java.dev.koalalayout.nbsupport.KoalaLayoutSupport.class.getName());
    }
}
