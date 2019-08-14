# Csense kotlin assistance 
This plugin is made to help with programming in kotlin and to avoid common JVM issues, and other weird cases, where the compiler nor the IDE (IDEA) warns about potential issues.
Examples include
 - wrong initialization order
 - mismatching parameter / lambda names
 - problems with inheritance and initialization order


## Changelog

### 0.5
- Fixed bugs with order (functions locations does not matter, as its the variables only) & indirect references duplicated names

### 0.4
- handle extensions better for initialization order.
- handle 1 level of indirect references for Initialization order.


### 0.3
- avoid repeating names in inspections.
- exclude synthetic properties for inheritance initialization order (only getter (optionally setter)) since they are only functions
- reference names in initializationInheritance order.
- ignore inspections added (WIP)
- handles getter only properties better & handles property setter better
- improved rearrange so that it only inspects the initializer, thus are able to work in more cases.  


### 0.2
- fixed a minor issue
- work on inheritance inspection.

### 0.1 
- first version 
