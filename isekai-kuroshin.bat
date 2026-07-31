@echo off
:: ============================================
:: isekai-kuroshin.bat
:: Versiyon: v1.0
:: Aciklama: IsekaiKuroshin — Android build + Python tools + ESP32
:: Repo: KuroShinHQ/isekai-kuroshin
:: Son guncelleme commit: 13679bc ("docs: update README (repo rename + content refresh)")
:: Detay: docs/CHANGELOG.md
:: ============================================
setlocal enabledelayedexpansion

set "IK_ROOT=%~dp0"
set "PYTHON=python"

title IsekaiKuroshin Build

:MENU
cls
echo ================================================
echo   ISEKAI-KUROSHIN v1.0
echo   Repo: KuroShinHQ/isekai-kuroshin
echo ================================================
echo.
echo  1) Gradle Build DENE (Android — Java 8 uyarisi)
echo  2) Python Tools Test (4 script)
echo  3) ESP32 Compile DENE (arduino-cli)
echo  4) Hiyerarsi Karsilastir
echo  5) Cikis
echo.
set /p choice="Secim (1-5): "

if "%choice%"=="1" goto GRADLE
if "%choice%"=="2" goto PYTHON
if "%choice%"=="3" goto ESP32
if "%choice%"=="4" goto TREE
if "%choice%"=="5" goto END
goto MENU

:GRADLE
echo.
echo [IsekaiKuroshin] Gradle build deneniyor...
echo [IsekaiKuroshin] NOT: Java 8 tespit edildi, Android Gradle plugin 8.4.1 Java 11 gerektiriyor.
echo [IsekaiKuroshin] ANDROID_HOME dogal ortam degiskeni ayarlanmamis.
echo.
cd /d "%IK_ROOT%"
call gradlew.bat tasks 2>&1
echo.
echo [IsekaiKuroshin] Gradle build sonlandi. Yukaridaki hata beklenen: Java 8 + ANDROID_HOME eksik.
pause
goto MENU

:PYTHON
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
if %errorlevel% equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo --- 3/4: app.py (Streamlit dashboard) ---
%PYTHON% -c "import tools.kuroshin_insight_dashboard.app; print('Import OK')" 2>&1
if %errorlevel% equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo --- 4/4: visualization.py ---
%PYTHON% -c "import tools.kuroshin_insight_dashboard.visualization; print('Import OK')" 2>&1
if %errorlevel% equ 0 ( echo     Durum: BASARILI ) else ( echo     Durum: BASARISIZ )
echo.
echo [IsekaiKuroshin] Python tools test tamam.
pause
goto MENU

:ESP32
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
echo.
echo [IsekaiKuroshin] Proje hiyerarsisi:
cd /d "%IK_ROOT%"
tree /A 2>&1
pause
goto MENU

:END
endlocal
