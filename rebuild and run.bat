@echo off
echo Deleting old class files...
del *.class 2>nul
echo.
echo Compiling Java files...
javac *.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo.
echo Starting game...
java GameApplication
