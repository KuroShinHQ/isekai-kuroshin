# 📖 Görsel Etiketleme Sistemi v2.0 - Kullanıcı Kılavuzu

**Versiyon:** 2.0
**Tarih:** 2025-10-22
**Sistem:** Eksen Bazlı Attribute Sistemi

---

## 🎯 Hızlı Başlangıç

### Adım 1: Uygulamayı Başlat

```bash
python gorsel_etiketleyici.py
```

Uygulama başlatıldığında **otomatik olarak medya analiz raporu** gösterilir:

```
📊 MEDYA ANALİZ RAPORU (v2.0)
================================================================================

🎬 FIRSTUSER:
   İlk kullanıcı deneyimi için minimum medya
   📹 Video: 0 / Min: 5 / Önerilen: 10 → ⚠️ EKSIK (0/5)
   📸 Foto:  0 / Min: 3 / Önerilen: 8 → ⚠️ EKSIK (0/3)
   📊 Durum: 🔴 CRITICAL

⚠️ KRİTİK EKSİKLİKLER:
   - FIRSTUSER: Minimum gereksinimler karşılanmadı!
```

Bu rapor **hangi screen type'lar için medya hazırlamanız gerektiğini** gösterir.

---

## 📊 Minimum Medya Gereksinimleri

### 🔴 Kritik Öncelik (Mutlaka Hazırlanmalı)

| Screen Type | Min Video | Min Foto | Önerilen Video | Önerilen Foto |
|-------------|-----------|----------|----------------|---------------|
| **FIRSTUSER** | 5 | 3 | 10 | 8 |
| **RETURNINGUSER** | 8 | 5 | 15 | 10 |
| **POSTDEATH** | 3 | 2 | 8 | 5 |

### 🟡 Orta Öncelik

| Screen Type | Min Video | Min Foto | Önerilen Video | Önerilen Foto |
|-------------|-----------|----------|----------------|---------------|
| **UMBROS** | 2 | 2 | 5 | 5 |
| **DEATH_TRANSITION** | 2 | 0 | 5 | 2 |

### 🟢 Düşük Öncelik

| Screen Type | Min Video | Min Foto | Önerilen Video | Önerilen Foto |
|-------------|-----------|----------|----------------|---------------|
| **JOURNEY** | 1 | 1 | 3 | 3 |
| **DEATH_STATISTICS** | 0 | 1 | 0 | 3 |
| **LAUNCHER_ICON** | 0 | 3 | 0 | 6 |

---

## 🎨 v2.0 Yeni Özellikler

### ✅ Eksen Bazlı Attribute Sistemi

**Eski Sistem (v1.0):**
- 16 ayrı attribute (VIOLENCE, MERCY, ORDER, CHAOS, ...)
- Zıt kavramlar ayrı ayrı etiketleniyordu
- Gereksiz tekrar ve karmaşa

**Yeni Sistem (v2.0):**
- **6 Attribute Ekseni** (Bipolar - Zıt kutuplu)
- **4 Özel Attribute** (Kategorik)

#### 6 Ana Eksen:

1. **AXIS_1: VIOLENCE ↔ MERCY** (Şiddet ↔ Merhamet)
2. **AXIS_2: CHAOS ↔ ORDER** (Kaos ↔ Düzen)
3. **AXIS_3: SELFISH ↔ SACRIFICE** (Bencillik ↔ Fedakarlık)
4. **AXIS_4: FEAR ↔ COURAGE** (Korku ↔ Cesaret)
5. **AXIS_5: DECEIT ↔ LOYALTY** (Hile ↔ Sadakat)
6. **AXIS_6: DARKNESS ↔ LIGHT** (Karanlık ↔ Işık)

#### 4 Özel Attribute:

- **SURVIVAL** - Hayatta kalma
- **DIVINE** - Kutsal / İlahi
- **CORRUPTION** - Bozulma / Yozlaşma
- **MYSTERY** - Gizemli / Esrarengiz

### ✅ Depth Sistemi (3 → 5 Seviye)

| Seviye | İsim | Açıklama | Örnek |
|--------|------|----------|-------|
| **D1** | SURFACE | Yüzeysel - Aksiyon, görsel efekt | Patlama sahnesi, savaş animasyonu |
| **D2** | EMOTIONAL | Duygusal - Karakter duyguları | Ağlayan karakter, kızgın yüz ifadesi |
| **D3** | SYMBOLIC | Sembolik - Metafor, simge | Kırık ayna (kimlik krizi), lotus (aydınlanma) |
| **D4** | ARCHETYPAL | Arketipsel - Jung arketipleri | Hero's Journey, Death archetype |
| **D5** | TRANSCENDENT | Aşkın - Felsefi, varoluşsal | Evren, sonsuzluk, ego death |

### ✅ NARRATIVE_ATMOSPHERE (11 Seçenek)

MORAL_TONE ve NARRATIVE_MOOD birleştirildi:

- TRAGIC_REDEMPTION (Trajik + Kefaret)
- EPIC_JUSTICE (Destansı + Adalet)
- DARK_VENGEANCE (Karanlık + İntikam)
- MELANCHOLIC_FORGIVENESS (Melankolik + Af)
- TRIUMPHANT_JUDGMENT (Zafer + Yargı)
- HORROR_DENIAL (Korku + İnkar)
- MYSTICAL_ACCEPTANCE (Mistik + Kabul)
- ROMANTIC_TRANSFORMATION (Romantik + Dönüşüm)
- COMEDIC (Komedi)
- NOIR (Karanlık dedektif)
- PHILOSOPHICAL (Felsefi)

### ✅ Emotion (6 → 5)

**FEAR kaldırıldı** → AXIS_4_FEAR_COURAGE'a taşındı

Kalan 5 emotion:
- ANGER (Öfke)
- SADNESS (Üzüntü)
- JOY (Sevinç)
- CALM (Sakinlik)
- CONFUSION (Kafa karışıklığı)

---

## 🔧 Ekran Türü Bazlı Wizard Akışı

### LAUNCHER_ICON Özel Kuralı

**Sadece PRIMARY_ATTRIBUTE seçilir!**

Allowed attributes:
- DIVINE (Kutsal)
- DARK (Karanlık)
- MYSTERY (Gizemli)

Diğer tüm adımlar **otomatik NONE** olur.

### JOURNEY Özel Kuralı

- `media_usage` **otomatik IN_SCREEN** olur
- `depth_priority`: D3, D4 (sembolik ve arketipsel içerik)

### DEATH_STATISTICS Özel Kuralı

**Tüm attribute'lar otomatik NONE!**

Sadece `update_mode` seçilir, diğer her şey varsayılan.

### DEATH_TRANSITION Özel Kuralı

- `media_usage` **otomatik TRANSITION_SCENE**
- `secondary_attribute` **otomatik NONE**
- `depth_priority`: D4, D5 (derin içerik)

---

## 📝 Etiketleme İş Akışı

### Standart Akış (FIRSTUSER / RETURNINGUSER / POSTDEATH)

1. **UPDATE_MODE** seç (GM_UPDATED / HARDCODED)
2. **MEDIA_USAGE** seç (TRANSITION_SCENE / IN_SCREEN / BACKGROUND)
3. **PRIMARY_ATTRIBUTE** seç (6 eksen + 4 özel)
4. **SECONDARY_ATTRIBUTE** seç (isteğe bağlı)
5. **EMOTION** seç (5 seçenek)
6. **NARRATIVE_ATMOSPHERE** seç (11 seçenek)
7. **PSYCHOLOGICAL_ARCHETYPE** seç (12 Jung arketipi)
8. **DEPTH** seç (D1-D5)

### Kısayollar

- **Space** - Sonraki dosya
- **Ctrl+Z** - Geri al
- **Ctrl+Y** - İleri al
- **Ctrl+S** - Kaydet
- **F1** - Yardım

---

## 🎮 Karma Sistemi Entegrasyonu

### Player Karma → Medya Seçimi

Oyuncunun karma profili **otomatik olarak** medya seçimini etkiler:

#### Örnek Senaryo:

**Player Durumu:**
- `moralityScore`: -0.7 (Negatif moral)
- `unholyPoints`: 85 (Yüksek unholy)
- `level`: 25

**Sistem Analizi:**
- **Dominant Axis**: AXIS_1_VIOLENCE_MERCY (-0.8) → VIOLENCE yönünde
- **Depth Affinity**: 0.6 (Orta-yüksek, level 25)
- **Karma Complexity**: 0.3 (Düşük, sadece VIOLENCE dominant)

**Medya Seçimi:**
- Primary Axis: **VIOLENCE_MERCY** (negatif kutup)
- Depth: %50 D1, %30 D2, **%15 D3** (depth affinity 0.6 → D3 boost)
- Narrative Atmosphere: **DARK_VENGEANCE** veya **TRIUMPHANT_JUDGMENT**

---

## 🚨 Sık Yapılan Hatalar

### ❌ HATA 1: JOURNEY Medyasını Normal Etiketleme

**Yanlış:**
```
VID_JOURNEY_VIOLENCE_ANGER_D3_001.mp4
```

**Doğru:**
```
vid_journey_bg.mp4  (Basit format, etiketleme YOK!)
```

**Açıklama:** JOURNEY background dosyaları (`vid_journey_bg`, `pht_journey_bg`) **asla** normal etiketleme sistemine tabi değildir!

### ❌ HATA 2: LAUNCHER_ICON için Birden Fazla Attribute

**Yanlış:**
- Primary: DIVINE
- Secondary: LIGHT (❌ İZİN VERİLMEZ!)

**Doğru:**
- Primary: DIVINE
- Secondary: NONE (✅ Otomatik)

### ❌ HATA 3: FEAR Emotion Seçimi

**Yanlış:**
```
Emotion: FEAR (❌ Artık yok!)
```

**Doğru:**
```
Primary Axis: AXIS_4_FEAR_COURAGE (negatif kutup = FEAR)
```

---

## 📈 İlerleme Takibi

### Medya Durumu Dialogu

Uygulama içinde:
1. Menü → **Medya Durumu**
2. Her screen type için **progress bar** gösterir
3. Durum renk kodları:
   - 🟢 EXCELLENT - Önerilen sayıya ulaşıldı
   - 🟡 MINIMUM - Minimum sayı karşılandı
   - 🔴 CRITICAL - Minimum altında!

### Konsol Raporu

Terminal'de otomatik gösterilir:

```bash
python gorsel_etiketleyici.py

📊 MEDYA ANALİZ RAPORU (v2.0)
...
```

---

## 🔗 İlgili Dosyalar

- `etiket_config_v2.json` - Konfigürasyon dosyası
- `gorsel_etiketleyici.py` - Python etiketleme aracı
- `app/src/main/java/com/example/isekaikuroshin/models/MediaTag.kt` - Kotlin data model
- `app/src/main/java/com/example/isekaikuroshin/engine/IntelligentContentEngine.kt` - Medya seçim motoru
- `app/src/main/java/com/example/isekaikuroshin/engine/GameMasterEngine.kt` - GM entegrasyonu

---

## 📞 Destek

Sorunlarla karşılaşırsanız:

1. `GORSEL_ETIKETLEYICI_SISTEM_ANALIZI_VE_YENIDEN_TASARIM.md` dosyasını kontrol edin
2. Minimum gereksinimler tablosunu gözden geçirin
3. Medya analiz raporunu çalıştırın: `python gorsel_etiketleyici.py`

---

**Son Güncelleme:** 2025-10-22
**Versiyon:** 2.0
**Sistem:** Stable ✅
