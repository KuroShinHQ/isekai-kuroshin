# 🤖 ENCODING MAP FOR AI (YZ İçin Kod Çözümleme Kılavuzu)

**Tarih:** 2025-10-22
**Format Versiyonu:** V3 (Ultra Kısaltma)
**Amaç:** Akıllı Kalp (GM) sisteminin medya dosyalarını doğru decode edebilmesi

---

## 📋 DOSYA ADI YAPISI (V3 Format)

```
[tür]_[ekran]_[prim]_[sec]_[emo]_[nar]_[arch]_[dep]_[seq].[ext]
```

**ZORUNLU ALANLAR:**
- ✅ `[tür]` = V veya P (otomatik)
- ✅ `[ekran]` = Screen Type (F1, F2, U1, vb.)
- ✅ `[dep]` = Depth (1-5)
- ✅ `[seq]` = Sequence (001, 002, vb.)

**OPSİYONEL ALANLAR** (0 olabilir):
- ⚪ `[prim]` = Primary Attribute
- ⚪ `[sec]` = Secondary Attribute
- ⚪ `[emo]` = Emotion
- ⚪ `[nar]` = Narrative Atmosphere
- ⚪ `[arch]` = Psychological Archetype

### Örnek 1 (Tam Dolu):
```
v_f1_1n15_1p04_e5_n11_a11_2_001.mp4
```

**Decode:**
- `v` = VIDEO
- `f1` = FIRSTUSER (İlk kullanıcı karşılama ekranı)
- `1n15` = VIOLENCE (Şiddet - Axis 1 Negative)
- `1p04` = MERCY (Merhamet - Axis 1 Positive)
- `e5` = ANGER (Öfke)
- `n11` = DARK_VENGEANCE (Karanlık intikam atmosferi)
- `a11` = SHADOW (Gölge arketipi)
- `2` = D2 (Emotional depth / Duygusal derinlik)
- `001` = Sequence number
- `.mp4` = Video dosyası

### Örnek 2 (Sadece Zorunlu):
```
v_f1_0_0_0_0_0_2_001.mp4
```

**Decode:**
- `v` = VIDEO
- `f1` = FIRSTUSER
- `0` = NONE (Primary yok)
- `0` = NONE (Secondary yok)
- `0` = NONE (Emotion yok)
- `0` = NONE (Narrative yok)
- `0` = NONE (Archetype yok)
- `2` = D2 (Depth seçilmiş)
- `001` = Sequence
- `.mp4` = Video

**Minimal etiketleme:** Sadece ekran türü + derinlik yeterli!

---

## 🔢 ENCODING TABLES (Kodlama Tabloları)

### 1️⃣ FILE TYPE (Dosya Türü)
| Kod | Açıklama | Açıklama (TR) |
|-----|----------|---------------|
| `V` | VIDEO | Video dosyası |
| `P` | PHOTO | Fotoğraf dosyası |

---

### 2️⃣ SCREEN TYPE (Ekran Türü)
| Kod | Açıklama | Açıklama (TR) | Kullanım |
|-----|----------|---------------|----------|
| `F1` | FIRSTUSER | İlk Kullanıcı Ekranı | İlk açılışta gösterilen karşılama ekranı |
| `F2` | RETURNINGUSER | Dönen Kullanıcı Ekranı | Her gün farklı içerik gösterilen ana ekran |
| `J1` | JOURNEY | Günlük Yolculuk Kitabı | Background animasyonu |
| `J2` | JOURNEY_TRANSITION | Günlük Açılış Animasyonu | Karma bazlı dinamik seçim |
| `P1` | POSTDEATH | Ölüm Sonrası Ekran | Yeniden doğuş bekleme ekranı |
| `P2` | DEATH_TRANSITION | Ölüm Geçiş Animasyonu | Ekranlar arası geçiş efekti |
| `P3` | DEATH_STATISTICS | Ölüm İstatistikleri | Arka plan görseli |
| `U1` | UMBROS | Umbros Karar Ekranı | Melek/Şeytan seçimi |
| `L1` | LAUNCHER_ICON | Launcher İkonu | Uygulama ikonu |

---

### 3️⃣ ATTRIBUTES (Nitelikler)

#### Special Attributes (Özel Nitelikler - Eksen dışı)
| Kod | Attribute | TR | Açıklama |
|-----|-----------|----|-----------|
| `S01` | DIVINE | İlahi / Kutsal | Tanrısal, kutsal, ilahi güç |
| `S08` | MYSTERY | Gizemli | Esrarengiz, anlaşılmaz |
| `S09` | SURVIVAL | Hayatta Kalma | Yaşama içgüdüsü |
| `S16` | CORRUPTION | Bozulma | Yozlaşma, çürüme |
| `0` | NONE | Yok | Seçilmemiş |

#### Axis-Based Attributes (Eksen Bazlı Nitelikler)

**Format:** `[axis_id][P/N][attr_id]`
- `axis_id` = Eksen numarası (1-6)
- `P` = Positive (olumlu kutup)
- `N` = Negative (olumsuz kutup)
- `attr_id` = Attribute ID (01-16)

**AXIS 1: VIOLENCE ↔ MERCY (Şiddet ↔ Merhamet)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `1N15` | VIOLENCE | Şiddet | Negative | Kötü karma (-) |
| `1P04` | MERCY | Merhamet | Positive | İyi karma (+) |

**AXIS 2: CHAOS ↔ ORDER (Kaos ↔ Düzen)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `2N12` | CHAOS | Kaos | Negative | Kötü karma (-) |
| `2P07` | ORDER | Düzen | Positive | İyi karma (+) |

**AXIS 3: SELFISH ↔ SACRIFICE (Bencillik ↔ Fedakarlık)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `3N11` | SELFISH | Bencillik | Negative | Kötü karma (-) |
| `3P06` | SACRIFICE | Fedakarlık | Positive | İyi karma (+) |

**AXIS 4: FEAR ↔ COURAGE (Korku ↔ Cesaret)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `4N10` | FEAR | Korku | Negative | Kötü karma (-) |
| `4P05` | COURAGE | Cesaret | Positive | İyi karma (+) |

**AXIS 5: DECEIT ↔ LOYALTY (Hile ↔ Sadakat)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `5N13` | DECEIT | Hile | Negative | Kötü karma (-) |
| `5P03` | LOYALTY | Sadakat | Positive | İyi karma (+) |

**AXIS 6: DARK ↔ LIGHT (Karanlık ↔ Işık)**
| Kod | Attribute | TR | Polarity | Karma Etkisi |
|-----|-----------|----|-----------| ------------|
| `6N14` | DARK | Karanlık | Negative | Kötü karma (-) |
| `6P02` | LIGHT | Işık | Positive | İyi karma (+) |

---

### 4️⃣ EMOTIONS (Duygular)
| Kod | Emotion | TR | Açıklama |
|-----|---------|----|-----------|
| `E1` | JOY | Sevinç | Mutluluk, neşe |
| `E2` | CALM | Sakinlik | Huzur, dinginlik |
| `E3` | CONFUSION | Kafa Karışıklığı | Şaşkınlık, belirsizlik |
| `E4` | SADNESS | Üzüntü | Hüzün, keder |
| `E5` | ANGER | Öfke | Kızgınlık, gazap |
| `0` | NONE | Yok | Seçilmemiş |

---

### 5️⃣ NARRATIVE ATMOSPHERE (Anlatı Atmosferi)
| Kod | Narrative | TR | Açıklama |
|-----|-----------|----|-----------|
| `N1` | TRIUMPHANT_JUDGMENT | Zafer Yargısı | Muzaffer adalet dağıtımı |
| `N2` | EPIC_JUSTICE | Epik Adalet | Destansı hak teslimi |
| `N3` | ROMANTIC_TRANSFORMATION | Romantik Dönüşüm | Aşkla değişim |
| `N4` | MYSTICAL_ACCEPTANCE | Mistik Kabul | Gizemli kabullenme |
| `N5` | TRAGIC_REDEMPTION | Trajik Kefaret | Acı kurtuluş |
| `N6` | MELANCHOLIC_FORGIVENESS | Melankolik Af | Hüzünlü bağışlama |
| `N7` | PHILOSOPHICAL | Felsefi | Düşünsel, sorgulatıcı |
| `N8` | COMEDIC | Komedi | Eğlenceli, mizahi |
| `N9` | NOIR | Noir | Karanlık dedektif havası |
| `N10` | HORROR_DENIAL | Korku İnkarı | Korkunç reddediş |
| `N11` | DARK_VENGEANCE | Karanlık İntikam | Acımasız öç alma |
| `0` | NONE | Yok | Seçilmemiş |

---

### 6️⃣ PSYCHOLOGICAL ARCHETYPE (Psikolojik Arketip)
| Kod | Archetype | TR | Açıklama |
|-----|-----------|----|-----------|
| `A1` | HERO | Kahraman | Cesur kurtarıcı |
| `A2` | SAGE | Bilge | Bilgili rehber |
| `A3` | CAREGIVER | Koruyucu | İlgi gösteren |
| `A4` | LOVER | Aşık | Sevgi dolu |
| `A5` | CREATOR | Yaratıcı | Sanatçı ruh |
| `A6` | RULER | Hükümdar | Lider, yönetici |
| `A7` | EXPLORER | Kaşif | Maceracı, özgür |
| `A8` | INNOCENT | Masum | Saf, temiz |
| `A9` | JESTER | Soytarı | Eğlenceli, oyuncu |
| `A10` | REBEL | İsyancı | Kurallara karşı çıkan |
| `A11` | SHADOW | Gölge | Karanlık yön |
| `A12` | DESTROYER | Yıkıcı | Tahripkar güç |
| `0` | NONE | Yok | Seçilmemiş |

---

### 7️⃣ DEPTH (Derinlik Seviyesi)
| Kod | Depth | TR | Açıklama |
|-----|-------|----|-----------|
| `1` | D1_SURFACE | Yüzey | Basit, açık anlam |
| `2` | D2_EMOTIONAL | Duygusal | Hissi katman |
| `3` | D3_SYMBOLIC | Sembolik | Metaforik anlam |
| `4` | D4_ARCHETYPAL | Arketipsel | Jung arketipleri |
| `5` | D5_TRANSCENDENT | Aşkın | Manevi, yüce |

---

## 🎯 AI KULLANIM SENARYOLARI

### Senaryo 1: Karma Bazlı Filtreleme
**Kullanıcı Karması:** -50 (Kötü)

**AI Akışı:**
```
1. 800 dosyayı tara
2. Ekran türü: F1 (FIRSTUSER) olanları filtrele
3. Attribute filtreleme:
   - 1N15 (VIOLENCE) ✅
   - 6N14 (DARK) ✅
   - 3N11 (SELFISH) ✅
   - 1P04 (MERCY) ❌ (iyi karma, kötü kullanıcıya uygun değil)
4. Emotion filtreleme:
   - E5 (ANGER) ✅
   - E4 (SADNESS) ✅
   - E1 (JOY) ❌
5. Sonuç: 12 uygun video bulundu
6. 5 tanesini rastgele seç ve oynat
```

**Örnek Uygun Dosya:**
```
v_f1_1n15_0_e5_n11_a11_2_001.mp4
→ VIDEO, FIRSTUSER ekranı, VIOLENCE, ANGER, DARK_VENGEANCE, SHADOW, D2
→ Kötü kullanıcı için mükemmel! ✅
```

---

### Senaryo 2: Karma Bazlı Filtreleme (İyi Kullanıcı)
**Kullanıcı Karması:** +80 (İyi)

**AI Akışı:**
```
1. 800 dosyayı tara
2. Ekran türü: F2 (RETURNINGUSER) olanları filtrele
3. Attribute filtreleme:
   - 1P04 (MERCY) ✅
   - 6P02 (LIGHT) ✅
   - 3P06 (SACRIFICE) ✅
   - 1N15 (VIOLENCE) ❌ (kötü karma)
4. Emotion filtreleme:
   - E1 (JOY) ✅
   - E2 (CALM) ✅
   - E5 (ANGER) ❌
5. Sonuç: 25 uygun video bulundu
6. 10 tanesini seç ve oynat
```

**Örnek Uygun Dosya:**
```
v_f2_1p04_6p02_e1_n3_a1_3_001.mp4
→ VIDEO, RETURNINGUSER, MERCY, LIGHT, JOY, ROMANTIC_TRANSFORMATION, HERO, D3
→ İyi kullanıcı için mükemmel! ✅
```

---

### Senaryo 3: Özel Durum - UMBROS Karar Ekranı
**Kullanıcı ölmek üzere, son karar zamanı**

**AI Akışı:**
```
1. Ekran türü: U1 (UMBROS) olanları filtrele
2. Attribute: S01 (DIVINE) veya 6N14 (DARK) olanlar
3. Depth: 4 veya 5 (D4_ARCHETYPAL, D5_TRANSCENDENT) tercih et
4. Sonuç: 8 uygun medya bulundu
5. Tümünü oynat (kritik karar anı)
```

**Örnek Uygun Dosyalar:**
```
v_u1_s01_0_e2_n4_a2_5_001.mp4
→ UMBROS, DIVINE (kutsal), CALM, MYSTICAL_ACCEPTANCE, SAGE, D5
→ Melek yolu için uygun ✅

v_u1_6n14_0_e5_n11_a11_4_001.mp4
→ UMBROS, DARK (karanlık), ANGER, DARK_VENGEANCE, SHADOW, D4
→ Şeytan yolu için uygun ✅
```

---

## 💡 AI PROMPT ÖRNEĞİ

YZ'ye şu şekilde context verebilirsin:

```
Sen Akıllı Kalp (GM) sisteminin medya seçim algoritmasısın.

ENCODING_MAP:
- V = VIDEO, P = PHOTO
- F1 = FIRSTUSER, F2 = RETURNINGUSER, U1 = UMBROS
- 1N15 = VIOLENCE, 1P04 = MERCY, 6N14 = DARK, 6P02 = LIGHT
- E1 = JOY, E2 = CALM, E5 = ANGER
- N11 = DARK_VENGEANCE, N5 = TRAGIC_REDEMPTION
- A11 = SHADOW, A1 = HERO
- 1 = D1, 2 = D2, 5 = D5

Kullanıcı karması: -50 (kötü)
Mevcut dosyalar:
- v_f1_1n15_0_e5_n11_a11_2_001.mp4
- v_f1_1p04_0_e1_n3_a1_2_002.mp4
- v_f1_6n14_0_e5_n9_a11_3_003.mp4

Hangi dosyaları seçersin ve neden?
```

**AI Cevabı:**
```
1. v_f1_1n15_0_e5_n11_a11_2_001.mp4 ✅
   - VIOLENCE (1n15) = kötü karma ile uyumlu
   - ANGER (e5) = kullanıcının duygusal durumuna uygun
   - DARK_VENGEANCE (n11) + SHADOW (a11) = karanlık tema güçlendirme

3. v_f1_6n14_0_e5_n9_a11_3_003.mp4 ✅
   - DARK (6n14) = karanlık yön vurgulama
   - ANGER (e5) = duygusal uyum
   - NOIR (n9) + SHADOW (a11) = karanlık estetik

2. v_f1_1p04_0_e1_n3_a1_2_002.mp4 ❌
   - MERCY (1p04) = iyi karma (kullanıcıya ters)
   - JOY (e1) = mutluluk (uygunsuz)
   - ROMANTIC_TRANSFORMATION (n3) + HERO (a1) = iyimserlik (istenmeyen)
```

---

## 🚀 TASARRUF İSTATİSTİKLERİ

### Önce (V1 Format):
```
vid_firstuser_violence_mercy_anger_tragic_redemption_hero_d2_001.mp4
(72 karakter)
```

### Sonra (V3 Format):
```
v_f1_1n15_1p04_e5_n5_a1_2_001.mp4
(33 karakter)
```

**TASARRUF:** %54 (39 karakter azalma!)

### 1000 Dosya İçin:
- **Önce:** ~72,000 karakter
- **Sonra:** ~33,000 karakter
- **Tasarruf:** ~39,000 karakter ≈ **9,750 token** (Google Vision API için)

---

## ✅ BACKWARD COMPATIBILITY (Geriye Uyumluluk)

Sistem 3 formatı da destekliyor:

1. **V3 (yeni):** `v_f1_1n15_1p04_e5_n11_a11_2_001.mp4` ✅
2. **V2 (orta):** `vid_firstuser_1n15_1p04_e5_n11_a11_d2_001.mp4` ✅
3. **V1 (eski):** `vid_firstuser_violence_joy_d1_001.mp4` ✅

YZ her üçünü de okuyup anlayabilir!

---

## 📝 ÖZET

1. **Dosya adları tam encode edildi** (tür, ekran, tüm alanlar)
2. **NONE → 0** (token tasarrufu)
3. **Eksen bazlı attribute sistem** (1N15 = Axis 1 Negative ID:15)
4. **YZ için mapping tablosu** (hızlı decode)
5. **Karma bazlı filtreleme** mümkün
6. **%54 token tasarrufu** (72 → 33 karakter)

**AI bu sistemi kullanarak yüzlerce dosyayı anında filtreleyip en uygun içeriği seçebilir!** 🎯
