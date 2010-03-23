;===============================================================================================
; Launch script for a Berlioz-based Web Application (Web Mode)
; Requires;
;  * Nullsoft installer NSIS 2.0 or later
;  * launcher-lib.nsh
;
; Purpose:
;   This script uses the silent install mode of NSIS to determine which JVM to use and to start
;   the Jetty bootstrap. It uses JAVAW and does not enable debugging.
;
;   This script is the launcher used for production.
;
;   WEB version specifications
;   o debug  = none
;   o config = 'config-web'
;   o jetty  = 'berlioz-web.xml'
;
; @author  Christophe Lauret (Weborganic)
; @version 1 August 2006
;===============================================================================================

; Configuration
!define APP_NAME_SHORT "Berlioz"
!define APP_NAME_LONG  "Berlioz"
!define APP_JARFILE    "start.jar"
!define APP_ICON       "..\includes\berlioz.ico"
!define APP_EXE        "..\build\berlioz-web.exe"
!define APP_SYS_VAR    "-Djava.security.auth.login.config=etc/login.conf -Dberlioz.config=config-web"
!define APP_JAVAEXE    "javaw.exe"
!define APP_PARAMS     "etc/berlioz-web.xml"

; the launcher library to use
!include "launcher.nsh"
