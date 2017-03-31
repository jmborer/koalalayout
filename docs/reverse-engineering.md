### How form loading works in different conditions
#### A new panel is created with default layout then layout of panel is set to koalalayout:

1. `AbstractLayoutSupport.initialize(context, null, false)`
   * `clean()`: creates a clean code structure and assigns it to `setLayoutCode` instance  variable: `setLayoutCode = layoutContext.getCodeStructure().createCodeGroup()`.  
     A `CodeStructure` is a Class representing code structure of one form. Also manages a pool of variables for code expressions, and a undo/redo queue.
     A `CodeGroup` is a Class representing code statements such as variable declaration, field origin or statements and also other code groups (see `CodeSupport` class for further information). It is a way to define and generate code in a form file.  
   * `initializeInstance()` -> `createDefaultLayoutInstance()`:  
     Creates a default instance of LayoutManager (for internal use) with the help of method `CreationFactory.createDefaultInstance(Class)`. Override this method if the layout manager is not a bean (cannot be created from default constructor). It also initializes `metaLayout` instance variable of `AbstractLayoutSupport`.
   * `readLayoutCode(CodeGroup)`: where CodeGroup is layoutCode CodeGroup to be filled with relevant layout code.  
     This method is used for "reading layout from code", called from `initialize` method. It recognizes relevant code which sets the layout manager on the container and reads the layout information from the code.  
     This includes the code for setting up the layout manager itself and the code for setting the layout manager to container. For setting up just the layout manager bean, the method readInitLayoutCode is used.  
     Reading components code is not done here.
3. Continure here


##### CodeSupport Class
| Class                    | extends            | Generates              |
|---------------------------|-------------------|-------------------------
| `AssignVariableStatement`| `AbstractCodeStatement`|`full_class_nam var_name = origin_string;`|
| `ConstructorOrigin`| `CodeExpressionOrigin`|`new constructor(<params +",">)`|
| `DeclareVariableStatement`| `AbstractCodeStatement`| `public static <type> var_name;`| 
| `DefaultCodeGroup`| `CodeGroup`| A list of statements |
| `FieldOrigin`| `CodeExpressionOrigin`|`class_name.field`|
| `FieldStatement`| `AbstractCodeStatement`|`[parent.]field = params[0];`|
| `MethodOrigin`| `CodeExpressionOrigin`|`[parent.]method(params[])`|
| `MethodStatement`| `AbstractCodeStatement`|`[parent.]performMethod(params[]);`|
| `ValueOrigin`| `CodeExpressionOrigin`| `javaString`|
