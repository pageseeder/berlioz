;===============================================================================================
; Launch script for a Berlioz-based Web Application (Debug Mode)
; Requires;
;  * Nullsoft installer NSIS 2.0 or later
;  * launcher-lib.nsh
;
; Purpose:
;   This script uses the silent install mode of NSIS to determine which JVM to use and to start
;   the Jetty bootstrap. It uses JAVA and displays debugging information.
;
;   This script is the launcher used for debugging.
;
; Version specifications:
;   o debug  = true
;   o config = 'config-dev'
;   o jetty  = 'berlioz-dev.xml'
;
; @author  Christophe Lauret (Weborganic)
; @version 9 October 2009
;===============================================================================================

; Configuration
!define APP_NAME_SHORT "Berlioz"
!define APP_NAME_LONG  "Berlioz"
!define APP_JARFILE    "start.jar"
!define APP_ICON       "..\includes\berlioz.ico"
!define APP_EXE        "..\build\berlioz-dev.exe"
!define APP_SYS_VAR    "-Djetty.port=8099 -Djava.security.auth.login.config=etc/login.conf -Dberlioz.debug=true -Dberlioz.config=config-dev -Dxsltfilter.caching.disable=true"
!define APP_JAVAEXE    "java.exe"
!define APP_PARAMS     "etc/berlioz-dev.xml"

; the launcher library to use
!include "launcher.nsh"
