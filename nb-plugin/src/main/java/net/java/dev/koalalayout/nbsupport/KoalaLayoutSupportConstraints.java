package net.java.dev.koalalayout.nbsupport;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import org.netbeans.modules.form.FormProperty;
import org.netbeans.modules.form.codestructure.CodeExpression;
import org.netbeans.modules.form.codestructure.CodeGroup;
import org.netbeans.modules.form.codestructure.CodeStatement;
import org.netbeans.modules.form.codestructure.CodeStructure;
import org.netbeans.modules.form.codestructure.CodeVariable;
import org.netbeans.modules.form.codestructure.FormCodeSupport;
import org.netbeans.modules.form.layoutsupport.LayoutConstraints;
import org.openide.ErrorManager;
import org.openide.nodes.Node;

/**
 * LayoutConstraints implementation class for GridBagConstraints. GridBagConstraints class is special in that it
 * requires more code statements for initialization (setting up the individual fields).
 *
 * There are two possible code variants: simple and complex. In the simple situation, no parameter of GridBagConstraints
 * is set, so the code looks like: container.add(component, new GridBagConstraints());
 *
 * In the complex situation, there are some parameters set - this requires additional code statement for each parameter,
 * and also a variable to be used for the constraints object. Then the code looks like: GridBagConstraints
 * gridBagConstraints; ... gridBagConstraints = new GridBagConstraints(); gridBagConstraints.gridx = 1;
 * gridBagConstraints.gridy = 2; container.add(component, gridBagConstraints);
 */
public class KoalaLayoutSupportConstraints implements LayoutConstraints {

    private GridBagConstraints constraints;
    private GridBagConstraints defaultConstraints = new GridBagConstraints();
    private Property[] properties;
    private CodeExpression constraintsExpression;
    private CodeGroup constraintsCode; // set of all relevant statements
    // set of all relevant statements
    private CodeStatement[] propertyStatements; // statements for properties
    // statements for properties
    private static Constructor constrConstructor;
    private static final int variableType = CodeVariable.LOCAL | CodeVariable.EXPLICIT_DECLARATION;
    private static final int variableMask = CodeVariable.SCOPE_MASK | CodeVariable.DECLARATION_MASK;
    private static final String defaultVariableName = "gridBagConstraints"; // NOI18N
    // NOI18N

    public KoalaLayoutSupportConstraints() {
        this.constraints = new GridBagConstraints();
        this.constraints.fill = GridBagConstraints.BOTH;
        this.constraints.anchor = GridBagConstraints.CENTER;
    }

    public KoalaLayoutSupportConstraints(GridBagConstraints prototype) {
        this(prototype.gridx, prototype.gridy, prototype.gridwidth, prototype.gridheight, prototype.insets, prototype.fill, prototype.anchor);
    }

    public KoalaLayoutSupportConstraints(int gridx, int gridy, int gridwidth, int gridheight, Insets insets, int fill, int anchor) {
        this.constraints = new GridBagConstraints();
        this.constraints.gridx = gridx;
        this.constraints.gridy = gridy;
        this.constraints.gridwidth = gridwidth;
        this.constraints.gridheight = gridheight;
        this.constraints.insets = insets;
        this.constraints.fill = fill;
        this.constraints.anchor = anchor;
    }

    @Override
    public Node.Property[] getProperties() {
        if (properties == null) {
            createProperties();
            reinstateProperties();
        }
        return properties;
    }

    public Insets getInsets() {
        return this.constraints.insets;
    }

    public int getGridX() {
        return this.constraints.gridx;
    }

    public int getGridY() {
        return this.constraints.gridy;
    }

    public int getGridWidth() {
        return this.constraints.gridwidth;
    }

    public int getGridHeight() {
        return this.constraints.gridheight;
    }

    public int getFill() {
        return this.constraints.fill;
    }

    public int getAnchor() {
        return this.constraints.anchor;
    }

    @Override
    public String toString() {
        String str = "GridBagConstraints "
                + "x=" + this.constraints.gridx 
                + ", y=" + this.constraints.gridy 
                + ", w=" + this.constraints.gridwidth 
                + ", h=" + this.constraints.gridheight 
                + ", insets=" + this.constraints.insets
                + ", fill=" + this.constraints.fill
                + ", anchor=" + this.constraints.anchor;
        return super.toString();
    }

    @Override
    public Object getConstraintsObject() {
        return constraints;
    }

    @Override
    public LayoutConstraints cloneConstraints() {
        return new KoalaLayoutSupportConstraints((GridBagConstraints) constraints.clone());
    }

    // -------
    /**
     * This method creates code expression for the constraints. It's called from the delegate's createConstraintsCode
     * method.
     *
     * @param codeStructure CodeStructure in which the expression will be created
     * @param constrCode CodeGroup to be filled with all the initialization statements
     * @return CodeExpression representing the constraints
     */
    CodeExpression createCodeExpression(CodeStructure codeStructure, CodeGroup constrCode) {
        this.constraintsCode = constrCode;
        propertyStatements = null;
        // GridBagConstraints is created by a simple constructor...
        constraintsExpression = codeStructure.createExpression(getConstraintsConstructor(), CodeStructure.EMPTY_PARAMS);
        // ...but the additionlly it requires to create the initialization
        // code statements
        updateCodeExpression();
        return constraintsExpression;
    }

    /**
     * This method reads CodeExpression object representing the constraints and also all its initialization statements
     * which are mapped to the constraints properties. It's called from the delegate's readConstraintsCode method.
     *
     * @param constrExp CodeExpression of the constraints
     * @param constrCode CodeGroup to be filled with recognize initialization statements
     */
    void readCodeExpression(CodeExpression constrExp, CodeGroup constrCode) {
        constraintsExpression = constrExp;
        constraintsCode = constrCode;
        propertyStatements = null;
        //            constrExp.setOrigin(CodeStructure.createOrigin(
        //                                        getConstraintsConstructor(),
        //                                        CodeStructure.EMPTY_PARAMS));
        getProperties(); // ensure properties are created
        // ensure properties are created
        boolean isAnyChanged = false;
        Iterator it = CodeStructure.getDefinedStatementsIterator(constrExp);
        while (it.hasNext()) {
            // go through all the statements of constraints code expression
            CodeStatement statement = (CodeStatement) it.next();
            for (int j = 0; j < properties.length; j++) {
                Property prop = properties[j];
                if (prop.field.equals(statement.getMetaObject())) {
                    // this statement represents a GridBagConstraints field
                    // assignment, we map the corresponding property to it
                    FormCodeSupport.readPropertyStatement(statement, prop, false);
                    setPropertyStatement(j, statement);
                    if (prop.isChanged()) {
                        // this is a non-default value
                        constrCode.addStatement(statement);
                        isAnyChanged = true;
                    }
                    break;
                }
            }
        }
        setupVariable(isAnyChanged);
    }

    /**
     * This method updates the constraints code according to the properties. This is called at the beginning - when the
     * constraints code expression is created - and then after each change of the constraints properties. This keeps the
     * code consistent with the properties.
     */
    private void updateCodeExpression() {
        if (constraintsCode == null || constraintsExpression == null) {
            return;
        }
        constraintsCode.removeAll();
        getProperties(); // ensure properties are created
        // ensure properties are created
        boolean isAnyChanged = false;
        for (int i = 0; i < properties.length; i++) // for each changed property, add the corresponding statement
        // to the code (constraintsCode - instance of CodeGroup)
        {
            if (properties[i].isChanged()) {
                constraintsCode.addStatement(getPropertyStatement(i));
                isAnyChanged = true;
            }
        }
        setupVariable(isAnyChanged);
    }

    /**
     * This method returns the code statement corresponding to property of given index. The statement is created if it
     * does not exist yet.
     *
     * @param index index of required statement
     */
    private CodeStatement getPropertyStatement(int index) {
        if (propertyStatements == null) {
            propertyStatements = new CodeStatement[properties.length];
        }
        CodeStatement propStatement = propertyStatements[index];
        if (propStatement == null) {
            CodeExpression propExp = constraintsExpression.getCodeStructure().createExpression(FormCodeSupport.createOrigin(properties[index]));
            // statement is field assignment; the property code expression
            // represents the assigned value
            propStatement = CodeStructure.createStatement(constraintsExpression, properties[index].field, propExp);
            propertyStatements[index] = propStatement;
        }
        return propStatement;
    }

    /**
     * Sets the code statement read form code for given property index.
     *
     * @param index index of the corresponding property
     * @param propStatement CodeStatement to be set
     */
    private void setPropertyStatement(int index, CodeStatement propStatement) {
        if (propertyStatements == null) {
            propertyStatements = new CodeStatement[properties.length];
        }
        propertyStatements[index] = propStatement;
    }

    /**
     * This method sets up the variable for constraints code expression. The variable is needed only there's some
     * property change (i.e. there's some statement in which the variable would be used). Once the variable is created,
     * it's used for all the GridBagConstraints in the form.
     */
    private void setupVariable(boolean anyChangedProperty) {
        CodeStructure codeStructure = constraintsExpression.getCodeStructure();
        CodeVariable var = constraintsExpression.getVariable();
        if (anyChangedProperty) {
            // there should be a variable
            if (var == null) {
                // no variable currently used
                var = findVariable(); // find and reuse variable
                // find and reuse variable
                if (var == null) {
                    // create a new variable
                    var = codeStructure.createVariableForExpression(constraintsExpression, variableType, defaultVariableName);
                } else {
                    // attach the constraints expression to the variable
                    codeStructure.attachExpressionToVariable(constraintsExpression, var);
                }
            }
            // add variable assignment code
            constraintsCode.addStatement(0, var.getAssignment(constraintsExpression));
        } else {
            // no variable needed
            codeStructure.removeExpressionFromVariable(constraintsExpression);
        }
    }

    private CodeVariable findVariable() {
        CodeStructure codeStructure = constraintsExpression.getCodeStructure();
        // first try "gridBagConstraints" name - this succeeds in most
        // cases (unless the name is used elsewhere or not created yet)
        CodeVariable var = codeStructure.getVariable(defaultVariableName);
        if (var != null && (var.getType() & variableMask) == variableType && GridBagConstraints.class.equals(var.getDeclaredType())) {
            return var;
        }
        // try to find variable of corresponding type (time expensive)
        Iterator it = codeStructure.getVariablesIterator(variableType, variableMask, GridBagConstraints.class);
        while (it.hasNext()) {
            var = (CodeVariable) it.next();
            if (var.getName().startsWith(defaultVariableName)) {
                return var;
            }
        }
        return null;
    }

    private void createProperties() {
        properties = new Property[]{new Property("gridx", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_gridx"), KoalaLayoutSupport.getBundle().getString("HINT_gridx"), GridPosEditor.class),
                                    new Property("gridy", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_gridy"), KoalaLayoutSupport.getBundle().getString("HINT_gridy"), GridPosEditor.class),
                                    new Property("gridwidth", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_gridwidth"), KoalaLayoutSupport.getBundle().getString("HINT_gridwidth"), GridSizeEditor.class),
                                    new Property("gridheight", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_gridheight"), KoalaLayoutSupport.getBundle().getString("HINT_gridheight"), GridSizeEditor.class),
                                    new Property("fill", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_fill"), KoalaLayoutSupport.getBundle().getString("HINT_fill"), FillEditor.class),
                                    new Property("ipadx", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_ipadx"), KoalaLayoutSupport.getBundle().getString("HINT_ipadx"), null),
                                    new Property("ipady", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_ipady"), KoalaLayoutSupport.getBundle().getString("HINT_ipady"), null),
                                    new Property("insets", Insets.class, KoalaLayoutSupport.getBundle().getString("PROP_insets"), KoalaLayoutSupport.getBundle().getString("HINT_insets"), null),
                                    new Property("anchor", Integer.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_anchor"), KoalaLayoutSupport.getBundle().getString("HINT_anchor"), AnchorEditor.class),
                                    new Property("weightx", Double.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_weightx"), KoalaLayoutSupport.getBundle().getString("HINT_weightx"), null),
                                    new Property("weighty", Double.TYPE, KoalaLayoutSupport.getBundle().getString("PROP_weighty"), KoalaLayoutSupport.getBundle().getString("HINT_weighty"), null)};
        // properties with editable combo box
        properties[0].setValue("canEditAsText", Boolean.TRUE); // NOI18N
        // NOI18N
        properties[1].setValue("canEditAsText", Boolean.TRUE); // NOI18N
        // NOI18N
        properties[2].setValue("canEditAsText", Boolean.TRUE); // NOI18N
        // NOI18N
        properties[3].setValue("canEditAsText", Boolean.TRUE); // NOI18N
        // NOI18N
    }

    private void reinstateProperties() {
        try {
            for (int i = 0; i < properties.length; i++) {
                FormProperty prop = (FormProperty) properties[i];
                prop.reinstateProperty();
            }
        } catch (IllegalAccessException e1) {
        } // should not happen
        // should not happen
        catch (InvocationTargetException e2) {
        } // should not happen
        // should not happen
    }

    private static Constructor getConstraintsConstructor() {
        if (constrConstructor == null) {
            try {
                constrConstructor = GridBagConstraints.class.getConstructor(new Class[0]);
            } catch (NoSuchMethodException ex) {
                // should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
            }
        }
        return constrConstructor;
    }

    // ---------
    /**
     * Property implementation for KoalaLayoutSupportConstraints. Each property is tied to one field of
     * GridBagConstraints. After a change in property, updateCodeExpression is called to reflect the change in the code.
     */
    private final class Property extends FormProperty {

        private Field field;
        private Class propertyEditorClass;

        Property(String name, Class type, String displayName, String shortDescription, Class propertyEditorClass) {
            super("KoalaLayoutSupportConstraints " + name, type, displayName, shortDescription);
            this.propertyEditorClass = propertyEditorClass;
            try {
                field = GridBagConstraints.class.getField(name);
            } catch (NoSuchFieldException ex) {
                // should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
            }
        }

        @Override
        public Object getTargetValue() {
            try {
                return field.get(constraints);
            } catch (Exception ex) {
                // should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
                return null;
            }
        }

        @Override
        public void setTargetValue(Object value) {
            try {
                field.set(constraints, value);
            } catch (Exception ex) {
                // should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
            }
        }

        @Override
        public boolean supportsDefaultValue() {
            return true;
        }

        @Override
        public Object getDefaultValue() {
            try {
                return field.get(defaultConstraints);
            } catch (Exception ex) {
                // should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
                return null;
            }
        }

        @Override
        public PropertyEditor getExpliciteEditor() {
            if (propertyEditorClass == null) {
                return null;
            }
            try {
                return (PropertyEditor) propertyEditorClass.newInstance();
            } catch (Exception ex) {
                //should not happen
                ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
                return null;
            }
        }

        @Override
        protected void propertyValueChanged(Object old, Object current) {
            if (isChangeFiring()) {
                updateCodeExpression();
            }
            super.propertyValueChanged(old, current);
        }

        @Override
        public void setPropertyContext(org.netbeans.modules.form.FormPropertyContext ctx) {
            // disabling this method due to limited persistence
            // disabling this method due to limited persistence
        } // capabilities (compatibility with previous versions)
        // capabilities (compatibility with previous versions)
    }
    // ------------
    // property editors for properties of KoalaLayoutSupportConstraints

    private abstract static class GridBagConstrEditor extends PropertyEditorSupport {

        String[] tags;
        Integer[] values;
        String[] javaInitStrings;
        boolean otherValuesAllowed;

        @Override
        public String[] getTags() {
            return tags;
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    return tags[i];
                }
            }

            return otherValuesAllowed && value != null ? value.toString() : null;
        }

        @Override
        public void setAsText(String str) {
            for (int i = 0; i < tags.length; i++) {
                if (tags[i].equals(str)) {
                    setValue(values[i]);
                    return;
                }
            }

            if (otherValuesAllowed) {
                try {
                    setValue(new Integer(Integer.parseInt(str)));
                } catch (NumberFormatException e) {
                }
            } // ignore
        }

        @Override
        public String getJavaInitializationString() {
            Object value = getValue();
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    return javaInitStrings[i];
                }
            }

            if (!otherValuesAllowed) {
                return javaInitStrings[0];
            }
            return value != null ? value.toString() : null;
        }
    }

    static final class GridPosEditor extends GridBagConstrEditor {

        public GridPosEditor() {
            tags = new String[]{
                KoalaLayoutSupport.getBundle().getString("VALUE_relative") // NOI18N
            };
            values = new Integer[]{
                new Integer(GridBagConstraints.RELATIVE)
            };
            javaInitStrings = new String[]{
                "java.awt.GridBagConstraints.RELATIVE" // NOI18N
            };
            otherValuesAllowed = true;
        }
    }

    static final class GridSizeEditor extends GridBagConstrEditor {

        public GridSizeEditor() {
            tags = new String[]{KoalaLayoutSupport.getBundle().getString("VALUE_relative"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_remainder") // NOI18N
            };
            values = new Integer[]{new Integer(GridBagConstraints.RELATIVE),
                                   new Integer(GridBagConstraints.REMAINDER)
            };
            javaInitStrings = new String[]{"java.awt.GridBagConstraints.RELATIVE", // NOI18N
                                           "java.awt.GridBagConstraints.REMAINDER" // NOI18N
            };
            otherValuesAllowed = true;
        }
    }

    static final class FillEditor extends GridBagConstrEditor {

        public FillEditor() {
            tags = new String[]{KoalaLayoutSupport.getBundle().getString("VALUE_fill_none"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_fill_horizontal"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_fill_vertical"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_fill_both") // NOI18N
            };
            values = new Integer[]{new Integer(GridBagConstraints.NONE),
                                   new Integer(GridBagConstraints.HORIZONTAL),
                                   new Integer(GridBagConstraints.VERTICAL),
                                   new Integer(GridBagConstraints.BOTH)
            };
            javaInitStrings = new String[]{"java.awt.GridBagConstraints.NONE", // NOI18N
                                           "java.awt.GridBagConstraints.HORIZONTAL", // NOI18N
                                           "java.awt.GridBagConstraints.VERTICAL", // NOI18N
                                           "java.awt.GridBagConstraints.BOTH" // NOI18N
            };
            otherValuesAllowed = false;
        }
    }

    static final class AnchorEditor extends GridBagConstrEditor {

        public AnchorEditor() {
            tags = new String[]{KoalaLayoutSupport.getBundle().getString("VALUE_anchor_center"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_north"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_northeast"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_east"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_southeast"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_south"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_southwest"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_west"), // NOI18N
                                KoalaLayoutSupport.getBundle().getString("VALUE_anchor_northwest")}; // NOI18N

            values = new Integer[]{new Integer(GridBagConstraints.CENTER),
                                   new Integer(GridBagConstraints.NORTH),
                                   new Integer(GridBagConstraints.NORTHEAST),
                                   new Integer(GridBagConstraints.EAST),
                                   new Integer(GridBagConstraints.SOUTHEAST),
                                   new Integer(GridBagConstraints.SOUTH),
                                   new Integer(GridBagConstraints.SOUTHWEST),
                                   new Integer(GridBagConstraints.WEST),
                                   new Integer(GridBagConstraints.NORTHWEST)
            };
            javaInitStrings = new String[]{
                "java.awt.GridBagConstraints.CENTER", // NOI18N
                "java.awt.GridBagConstraints.NORTH", // NOI18N
                "java.awt.GridBagConstraints.NORTHEAST", // NOI18N
                "java.awt.GridBagConstraints.EAST", // NOI18N
                "java.awt.GridBagConstraints.SOUTHEAST", // NOI18N
                "java.awt.GridBagConstraints.SOUTH", // NOI18N
                "java.awt.GridBagConstraints.SOUTHWEST", // NOI18N
                "java.awt.GridBagConstraints.WEST", // NOI18N
                "java.awt.GridBagConstraints.NORTHWEST" // NOI18N
            };
            otherValuesAllowed = false;
        }
    }
}
