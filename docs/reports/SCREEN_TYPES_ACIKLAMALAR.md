# 📋 Ekran Türleri Açıklama Raporu

**Tarih:** 2025-10-22
**Durum:** ✅ Tamamlandı
**Amaç:** SCREEN_TYPES, UPDATE_MODE, MEDIA_USAGE kategorilerine emoji ve açıklama eklemek

---

## 🎯 GEREKSİNİM

Wizard'da ekran türü seçerken kullanıcı her seçeneğin ne işe yaradığını net görmeli:
- ✅ Emoji ile görsel ayırım
- ✅ → ile mini açıklama
- ✅ Özellikle POSTDEATH için "İlk giren kullanıcı ekranını manipüle eder" açıklaması

---

## ✅ EKLENEN AÇIKLAMALAR

### 1️⃣ SCREEN_TYPES (Ekran Türleri)

**TÜRKÇE:**
```
🌟 İlk Kullanıcı Karşılama → İlk açılış ekranı
👋 Dönen Kullanıcı Karşılama → Tekrar giriş ekranı
💀 Ölüm Sonrası Ekran → İlk giren kullanıcı ekranını manipüle eder ⭐
🌙 Umbros Karar Ekranı → Seçim ve karar anları
📖 Günlük Yolculuk (Kitap) → Hikaye akışı
⚰️ Ölüm Animasyonu → Ölüm geçiş efekti
📊 Ölüm İstatistik Ekranı → Ret sonrası istatistikler
📱 Uygulama Launcher İkonu → Ana uygulama ikonu
⚠️ Seçilmedi
```

**İNGİLİZCE:**
```
🌟 First User → Initial welcome screen
👋 Returning User → Re-entry welcome screen
💀 Post-Death → Manipulates first user screen ⭐
🌙 Umbros Decision Screen → Choice and decision moments
📖 Daily Journey (Book) → Story flow
⚰️ Death Animation → Death transition effect
📊 Death Statistics → Stats after rejection
📱 App Launcher Icon → Main app icon
⚠️ Not Selected
```

---

### 2️⃣ UPDATE_MODE (Güncelleme Modu)

**TÜRKÇE:**
```
🤖 GM Tarafından Güncellenir → AI otomatik seçer
✍️ Sabit Kodlanmış → Manuel belirlendi
⚠️ Seçilmedi
```

**İNGİLİZCE:**
```
🤖 GM Updated → AI auto-selects
✍️ Hardcoded → Manually determined
⚠️ Not Selected
```

---

### 3️⃣ MEDIA_USAGE (Medya Kullanım Yeri)

**TÜRKÇE:**
```
🎬 Geçiş Animasyonu → Ekranlar arası animasyon
📺 Ekran İçeriği → Ana gösterim alanı
🖼️ Arka Plan → Dekoratif arka plan
⚠️ Seçilmedi
```

**İNGİLİZCE:**
```
🎬 Transition Scene → Between-screen animation
📺 In-Screen → Main display area
🖼️ Background → Decorative background
⚠️ Not Selected
```

---

## 🎨 EMOJI REHBERİ

| Kategori | Emoji | Anlamı |
|----------|-------|--------|
| **SCREEN_TYPES** | | |
| FIRSTUSER | 🌟 | Yeni başlangıç |
| RETURNINGUSER | 👋 | Karşılama |
| POSTDEATH | 💀 | Ölüm |
| UMBROS | 🌙 | Gece/Karar |
| JOURNEY | 📖 | Kitap/Hikaye |
| DEATH_TRANSITION | ⚰️ | Ölüm animasyonu |
| DEATH_STATISTICS | 📊 | İstatistik |
| LAUNCHER_ICON | 📱 | Mobil uygulama |
| **UPDATE_MODE** | | |
| GM_UPDATED | 🤖 | AI/Otomasyon |
| HARDCODED | ✍️ | Manuel/Elle |
| **MEDIA_USAGE** | | |
| TRANSITION_SCENE | 🎬 | Film/Animasyon |
| IN_SCREEN | 📺 | Ekran/Görüntü |
| BACKGROUND | 🖼️ | Resim/Arka plan |

---

## 🧪 TEST

```bash
python gorsel_etiketleyici_v2.py
```

Wizard'da artık:
- ✅ Her ekran türü emojili ve açıklamalı gösterilecek
- ✅ POSTDEATH için "İlk giren kullanıcı ekranını manipüle eder" açıklaması görünecek
- ✅ UPDATE_MODE ve MEDIA_USAGE da açıklamalı

---

## 📋 ÖZET

✅ **TAMAMLANAN:**
1. SCREEN_TYPES için 8 emoji + açıklama eklendi
2. UPDATE_MODE için 2 emoji + açıklama eklendi
3. MEDIA_USAGE için 3 emoji + açıklama eklendi
4. Özel vurgu: POSTDEATH → "İlk giren kullanıcı ekranını manipüle eder"
5. Hem TR hem EN dillerinde eksiksiz

✅ **FORMAT:**
- Emoji + İsim → Kısa Açıklama
- Örnek: 💀 Ölüm Sonrası Ekran → İlk giren kullanıcı ekranını manipüle eder

🎯 **SONUÇ:** Kullanıcı artık her seçeneğin ne işe yaradığını wizard'da görebilecek!
