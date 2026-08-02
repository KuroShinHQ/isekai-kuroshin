# isekai-kuroshin — Changelog

## v1.1 (1 Ağustos 2026)

**GÖREV 1: Ortak launcher logger standardı (kuro_logger).**

- **Ortak logger:** `_hub/shared-scripts/kuro_logger.bat` entegre edildi
- **Log hedefi:** `_hub/shared-logs/isekai_kuroshin_launcher.log`
- **Launcher güncellemeleri:**
  - `isekai-kuroshin.bat` v1.0 → v1.1 — her menü dalında log (GRADLE/PYTHON/ESP32/TREE), exit code yakalama + **menü EOF düzeltmesi** (boş seçimde döngü yerine çıkış)
  - `tools/kuroshin_insight_dashboard/start.bat` v2.0 → v2.1 — başlık bloğu + logger + FATAL dalları + exit code
- Commit: ed6dd08 (başlık referansı) — bu değişikliklerin commit'i bekliyor

## v1.0 (30 Temmuz 2026)

**Faz F: Bağımsız repo taşıması.** GitHub'dan `C:\KuroshinHQ\isekai-kuroshin\`'a temiz clone.

- **İç hiyerarşi:** 583 dosya — Faz C subtree ile birebir eşleşiyor
- **Launcher:** `isekai-kuroshin.bat` (standart başlık bloğu, menülü)
- **Gradle build:** DENENDİ → başarısız (sistemde Java 8, AGP 8.4.1 Java 11 gerektiriyor; ANDROID_HOME ayarlı değil)
- **ESP32 compile:** DENENDİ → başarısız (arduino-cli kurulu değil, winget/pip ile kurulamadı)
- **Python tools:** 3/4 PASS (analysis_engine ✓, app.py (Streamlit) ✓, visualization ✓); 1/4 ATLANDI (etiketleyici_v2.py GUI — headless ortamda çalışmaz)
- **Önceki commit:** 13679bc ("docs: update README (repo rename + content refresh)")
