@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.7
set GRADLE_DIR=%APP_HOME%.gradle\local\gradle-%GRADLE_VERSION%

if not exist "%GRADLE_DIR%\bin\gradle.bat" (
  echo Ce projet utilise le workflow GitHub Actions pour compiler automatiquement sur Linux.
  echo Sur Windows, installe Gradle 8.7 ou lance la compilation via GitHub Actions.
  exit /b 1
)

"%GRADLE_DIR%\bin\gradle.bat" %*
