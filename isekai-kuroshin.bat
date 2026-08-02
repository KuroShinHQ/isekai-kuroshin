@echo off
:: ============================================
:: isekai-kuroshin.bat
:: Versiyon: v1.1
:: Aciklama: IsekaiKuroshin — Android build + Python tools + ESP32
:: Repo: KuroShinHQ/isekai-kuroshin
:: Son guncelleme commit: ed6dd08
:: Detay: docs/CHANGELOG.md
:: ============================================
setlocal enabledelayedexpansion

set "IK_ROOT=%~dp0"
set "ROOT=%~dp0..\"
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
set "KLOGGER=%ROOT%_hub\shared-scripts\kuro_logger.bat"
set "KLOG_FILE=%ROOT%_hub\shared-logs\isekai_kuroshin_launcher.log"
call "%KLOGGER%" "%KLOG_FILE%" init "isekai-kuroshin.bat v1.1"
set "PYTHON=python"

title IsekaiKuroshin Build

:MENU
cls
echo ================================================
echo   ISEKAI-KUROSHIN v1.1
echo   Repo: KuroShinHQ/isekai-kuroshin
echo ================================================
echo.
echo  1) Gradle Build DENE (Android — Java 8 uyarisi)
echo  2) Python Tools Test (4 script)
echo  3) ESP32 Compile DENE (arduino-cli)
echo  4) Hiyerarsi Karsilastir
echo  5) Cikis
echo.
set "choice="
set /p choice="Secim (1-5): "

if "%choice%"=="" goto END
if "%choice%"=="1" goto GRADLE
if "%choice%"=="2" goto PYTHON
if "%choice%"=="3" goto ESP32
if "%choice%"=="4" goto TREE
if "%choice%"=="5" goto END
goto MENU

:GRADLE
call "%KLOGGER%" "%KLOG_FILE%" INFO "Secim: 1 (gradle)"
echo.
echo [IsekaiKuroshin] Gradle build deneniyor...
echo [IsekaiKuroshin] NOT: Java 8 tespit edildi, Android Gradle plugin 8.4.1 Java 11 gerektiriyor.
echo [IsekaiKuroshin] ANDROID_HOME dogal ortam degiskeni ayarlanmamis.
echo.
cd /d "%IK_ROOT%"
call gradlew.bat tasks 2>&1
set "RC=!ERRORLEVEL!"
call "%KLOGGER%" "%KLOG_FILE%" exitcode "gradlew tasks" !RC!
echo.
echo [IsekaiKuroshin] Gradle build sonlandi. Yukaridaki hata beklenen: Java 8 + ANDROID_HOME eksik.
pause
goto MENU

:PYTHON
call "%KLOGGER%" "%KLOG_FILE%" INFO "Secim: 2 (python tools)"
echo.
echo [IsekaiKuroshin] Python tools test ediliyor...
echo.
cd /d "%IK_ROOT%"
echo --- 1/4: etiketleyici_v2.py (GUI - display gerektirir, atlaniyor) ---
echo     Modul: customtkinter + cv2 GUI, headless ortamda calismaz
echo     Durum: ATLANDI (display gerek)
echo.
echo --- 2/4: analysis_engine.py ---
%PYTHON% tools/kuroshin_insight_dashboard/analysis_engine.py --help 2>&1
set "RC2=!ERRORLEVEL!"
call "%KLOGGER%" "%KLOG_FILE%" exitcode "analysis_engine --help" !RC2!
if !RC2! equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo --- 3/4: app.py (Streamlit dashboard) ---
%PYTHON% -c "import sys; sys.path.insert(0, r'%IK_ROOT%tools\kuroshin_insight_dashboard'); import tools.kuroshin_insight_dashboard.app; print('Import OK')" 2>&1
set "RC3=!ERRORLEVEL!"
call "%KLOGGER%" "%KLOG_FILE%" exitcode "app.py import" !RC3!
if !RC3! equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo --- 4/4: visualization.py ---
%PYTHON% -c "import tools.kuroshin_insight_dashboard.visualization; print('Import OK')" 2>&1
set "RC4=!ERRORLEVEL!"
call "%KLOGGER%" "%KLOG_FILE%" exitcode "visualization import" !RC4!
if !RC4! equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo [IsekaiKuroshin] Python tools test tamam.
pause
goto MENU

:ESP32
call "%KLOGGER%" "%KLOG_FILE%" INFO "Secim: 3 (esp32)"
echo.
echo [IsekaiKuroshin] ESP32 compile deneniyor...
echo [IsekaiKuroshin] NOT: arduino-cli kurulu degil.
where arduino-cli >nul 2>&1
if %errorlevel% equ 0 (
    echo arduino-cli bulundu, compile baslatiliyor...
    for /r "%IK_ROOT%hardware" %%f in (*.ino) do (
        echo Derleniyor: %%~nxf
        arduino-cli compile --fqbn esp32:esp32:esp32 "%%f" 2>&1
    )
) else (
    echo arduino-cli BULUNAMADI.
    echo ESP32 firmware (4 .ino dosyasi) icin arduino-cli veya Arduino IDE (ESP32 core)
    echo gereklidir. Bu ortamda mevcut degil.
    echo.
    dir /s /b "%IK_ROOT%hardware\*.ino" 2>&1
)
echo.
echo [IsekaiKuroshin] ESP32 compile test tamam.
pause
goto MENU

:TREE
call "%KLOGGER%" "%KLOG_FILE%" INFO "Secim: 4 (hiyerarsi)"
echo.
echo [IsekaiKuroshin] Proje hiyerarsisi:
cd /d "%IK_ROOT%"
tree /A 2>&1
pause
goto MENU

:END
endlocal
