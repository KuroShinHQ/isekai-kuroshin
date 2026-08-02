rem ============================================
rem start.bat (Kuroshin Insight Dashboard)
rem Versiyon: v2.1
rem Aciklama: Streamlit dashboard baslatici (port 8501)
rem Repo: KuroShinHQ/isekai-kuroshin
rem Son guncelleme commit: ed6dd08
rem Detay: docs/CHANGELOG.md
rem ============================================
setlocal enabledelayedexpansion

set "IK_ROOT=%~dp0"
set "ROOT=%~dp0..\..\..\"
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
if not "%ROOT:~-1%"=="\" set "ROOT=%ROOT%\"
set "KLOGGER=%ROOT%_hub\shared-scripts\kuro_logger.bat"
set "KLOG_FILE=%ROOT%_hub\shared-logs\isekai_kuroshin_launcher.log"
call "%KLOGGER%" "%KLOG_FILE%" init "start.bat v2.1 (insight dashboard)"

title Kuroshin Insight Dashboard Baslatici

echo.
echo  ============================================================
echo      KUROSHIN INSIGHT DASHBOARD - AKILLI BASLATICI v2.1
echo  ============================================================
echo.
echo [1/4] Python kurulumu kontrol ediliyor...
where python >nul 2>nul
if %errorlevel% neq 0 (
    call "%KLOGGER%" "%KLOG_FILE%" FATAL "Python bulunamadi"
    echo [HATA!] Python bulunamadi. Lutfen Python'u kurun.
    pause
    goto :EOF
)
echo [OK] Python bulundu.
echo.

echo [2/4] Sanal ortam (venv) kontrol ediliyor...
if not exist ".\venv" (
    echo [BILGI] Sanal ortam olusturuluyor...
    python -m venv venv
    echo [OK] Sanal ortam olusturuldu.
) else (
    echo [OK] Mevcut sanal ortam bulundu.
)
echo.

set FLAG_FILE=.\venv\install_complete.flag

echo [3/4] Gerekli kutuphaneler kontrol ediliyor...
if not exist "%FLAG_FILE%" (
    echo [BILGI] Ilk kurulum yapiliyor. Gerekli kutuphaneler indirilecek...
    call ".\venv\Scripts\activate.bat"
    pip install -r requirements.txt
    if %errorlevel% neq 0 (
        call "%KLOGGER%" "%KLOG_FILE%" FATAL "Kutuphaneler yuklenemedi"
        echo [HATA!] Kutuphaneler yuklenirken bir sorun olustu.
        pause
        goto :EOF
    )
    echo [OK] Tum kutuphaneler basariyla yuklendi.
    echo Kurulum Tamamlandi > "%FLAG_FILE%"
) else (
    echo [OK] Kutuphaneler zaten kurulu.
)
echo.

echo [4/4] Uygulama baslatiliyor...
echo Dashboard tarayicinizda acilacak...
echo.

call ".\venv\Scripts\activate.bat"
start "" http://localhost:8501
streamlit run app.py
set "RC=!ERRORLEVEL!"
call "%KLOGGER%" "%KLOG_FILE%" exitcode "streamlit run app.py" !RC!

goto :EOF