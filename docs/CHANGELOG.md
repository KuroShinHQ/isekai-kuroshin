# isekai-kuroshin — Changelog

## v1.0 (30 Temmuz 2026)

**Faz F: Bağımsız repo taşıması.** GitHub'dan `C:\KuroshinHQ\isekai-kuroshin\`'a temiz clone.

- **İç hiyerarşi:** 583 dosya — Faz C subtree ile birebir eşleşiyor
- **Launcher:** `isekai-kuroshin.bat` (standart başlık bloğu, menülü)
- **Gradle build:** DENENDİ → başarısız (sistemde Java 8, AGP 8.4.1 Java 11 gerektiriyor; ANDROID_HOME ayarlı değil)
- **ESP32 compile:** DENENDİ → başarısız (arduino-cli kurulu değil, winget/pip ile kurulamadı)
- **Python tools:** 3/4 PASS (analysis_engine ✓, app.py (Streamlit) ✓, visualization ✓); 1/4 ATLANDI (etiketleyici_v2.py GUI — headless ortamda çalışmaz)
- **Önceki commit:** 13679bc ("docs: update README (repo rename + content refresh)")
