rem Turkce karakter destegi icin kod sayfasi ayarlanir.
chcp 65001 > nul

title Kuroshin Insight Dashboard Baslatici

echo.
echo  ============================================================
echo      KUROSHIN INSIGHT DASHBOARD - AKILLI BASLATICI v2.0
echo  ============================================================
echo.
echo [1/4] Python kurulumu kontrol ediliyor...
where python >nul 2>nul
if %errorlevel% neq 0 (
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

goto :EOF