# README #

Rigado Module Examples

### What is this repository for? ###

* Quick summary
    + This repository provides a selection of examples for interacting with Rigado modules via iOS and/or Android mobile applications.  It may also eventually include node.js examples for noble (https://github.com/sandeepmistry/noble)

### How do I get set up? ###

* This repository requires the use of submodules.  It must be cloned using the --recursive option to ensure all submodules are initialized.  Note that one of these submodules is the Rigablue library which is a private repository.  Ask an admin for access if not granted with access to this repository.
    + git clone --recursive git@github.com:rigado/rigado-module-examples

* Dependencies
    + Rigablue
    + SVProgressHUD (for some iOS Examples; included as a submodule) https://github.com/TransitApp/SVProgressHUD

* Firmware files for the DFU Example
    + The DFU example has some firmware files included in the project tree but these files are not kept with the project.  Please delete them and add your own files.  The build will fail until the files are removed from the project.

### Contribution guidelines ###

* If you are adding to the example app sections, please be sure to keep all submodules up to date and add new ones as needed.

### Who do I talk to? ###

* Repo owner or admin
estutzenberger, jrigling, Rigado, LLC

* Other community or team contact
None at this time
