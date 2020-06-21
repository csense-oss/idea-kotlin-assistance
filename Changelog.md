# Changelog

### 0.9.13
 - Fixes to initialization order (it now inspects "init" functions and handles local {properties, functions} better)
 - New inspection for "while" loops, to verify if you update the loop parameter (if it is a local var)
 - Potential dangerous return updates / fixes
 
###  0.9.12
 - "Function have same name" inspection not triggering on local variables nor on non-functional types
 - quick fixes updated for labeled returned
 - text updates 
 
### 0.9.11
- improvements to mismatched arg names.

### 0.9.10
- removed dead code, as it caused compatibility issues
- Updated usage after overwrite to handle multiple vars

### 0.9.9
- update descriptions
- removed dead / unused code

### 0.9.8
- usage after overwriting inspection added.
- function and variable name collision inspection added

### TODO in between

### 0.5
- Fixed bugs with order (functions locations does not matter, as it is the variables only) & indirect references duplicated names

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