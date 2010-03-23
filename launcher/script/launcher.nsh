;===============================================================================================
; Launch script for Berlioz Web Application
; Requires;
;  * Nullsoft installer NSIS 2.0 or later
;
; Purpose:
;   This script uses the silent install mode of NSIS to determine which JVM to use and to start
;   the Jetty bootstrap.
;
;   This script can be used as a library by the other launchers.
;
; @author  Christophe Lauret (Weborganic)
; @version 9 October 2009
;===============================================================================================

;-----------------------------------------------------------------------------------------------
; Configuration

;!define APP_NAME_SHORT ""   ; A short name for the application
;!define APP_NAME_LONG  ""   ; A long name for the application
;!define APP_JARFILE    ""   ; The jar file to execute (bootstrap)
;!define APP_ICON       ""   ; The path to the icon use
;!define APP_EXE        ""   ; The path to the executable to generate
;!define APP_SYS_VAR    ""   ; To Pass additional system property use eg. "-Dberlioz.debug=true"
;!define APP_JAVAEXE    ""   ; The Java executable to use "java.exe" or "javaw.exe"

;-----------------------------------------------------------------------------------------------
; Script Initialisation

Name "${APP_NAME_SHORT}"
Caption "${APP_NAME_LONG}"
Icon "${APP_ICON}"
OutFile "${APP_EXE}"

SilentInstall silent
XPStyle on

!addplugindir .

;-----------------------------------------------------------------------------------------------
; Hidden section

Section ""
  ; create a MUTEX object (prevents simultaneous executions)
  System::Call "kernel32::CreateMutexA(i 0, i 0, t '${APP_NAME_SHORT}') i .r1 ?e"
  Pop $R0
  StrCmp $R0 0 +2
  Quit

  ; check if the JRE shipped exist
  StrCpy $R0 "$EXEDIR\jre\1_5_0"
  IfFileExists "$R0\bin\${APP_JAVAEXE}" FoundVM 0

  ; check if a JRE is installed on the system
  ClearErrors
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
  IfErrors 0 FoundVM

  ; check if a JDK is installed on the system
  ClearErrors
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R0" "JavaHome"
  IfErrors 0 FoundVM

  ; try the JAVA_HOME environment variable
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  IfErrors NotFound 0

  ; OK let's start the process
  FoundVM:
  StrCpy $R0 "$R0\bin\${APP_JAVAEXE}"
  IfFileExists $R0 0 NotFound

  StrCpy $R1 ""
  Call GetParameters
  Pop $R1

  SetOverwrite ifdiff
  SetOutPath "$EXEDIR\jetty"
  StrCpy $R0 '$R0 ${APP_SYS_VAR} -jar "${APP_JARFILE}" ${APP_PARAMS} $R1'
  Exec "$R0"

  ; give some time to the JVM to start up
  Sleep 5000
  Quit

  ; Could not find the JRE, ask if the user wants to download it from Sun's website
  NotFound:
  Sleep 800
  MessageBox MB_ICONEXCLAMATION|MB_YESNO \
          'Could not find a Java Runtime Environment installed on your computer. \
          $\nWithout it you cannot run "${APP_NAME_LONG}". \
          $\n$\nWould you like to visit the Java website to download it?' \
          IDNO +2
  ExecShell open "http://java.sun.com/getjava"
  Quit
SectionEnd

;-----------------------------------------------------------------------------------------------
; Utility functions

; get the command line parameters
Function GetParameters
  Push $R0
  Push $R1
  Push $R2
  StrCpy $R0 $CMDLINE 1
  StrCpy $R1 '"'
  StrCpy $R2 1
  StrCmp $R0 '"' loop
  StrCpy $R1 ' '
  loop:
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 $R1 loop2
    StrCmp $R0 "" loop2
    IntOp $R2 $R2 + 1
    Goto loop
  loop2:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 " " loop2
  StrCpy $R0 $CMDLINE "" $R2
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd
