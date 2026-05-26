@echo off
cd /d "C:\Users\Opriasa\Desktop\Java_Repository_System\Repository System"
javac -cp "lib\*" -d bin src\*.java
if %errorlevel% equ 0 (
    start "" javaw -cp "bin;lib\*" login
    exit
) else (
    echo Compilation failed.
    pause
)