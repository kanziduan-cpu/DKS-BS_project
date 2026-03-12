@echo off
set sourceDir=C:\Users\TSBJ\Documents\WarehouseMonitor\app\src\main\java\com\warehouse\monitor
set targetDir=c:\Users\TSBJ\Documents\BS_project\app-android\app\src\main\java\com\warehouse\monitor

echo Creating directories...
mkdir "%targetDir%\adapter" 2>nul
mkdir "%targetDir%\ui\fragments" 2>nul

echo Copying files...
xcopy "%sourceDir%\db\*.java" "%targetDir%\db\" /Y
xcopy "%sourceDir%\mqtt\*.java" "%targetDir%\mqtt\" /Y
xcopy "%sourceDir%\service\*.java" "%targetDir%\service\" /Y
xcopy "%sourceDir%\model\*.java" "%targetDir%\model\" /Y
xcopy "%sourceDir%\adapter\*.java" "%targetDir%\adapter\" /Y
xcopy "%sourceDir%\utils\*.java" "%targetDir%\utils\" /Y
xcopy "%sourceDir%\ui\*.java" "%targetDir%\ui\" /Y
xcopy "%sourceDir%\ui\fragments\*.java" "%targetDir%\ui\fragments\" /Y

echo Done!
pause
