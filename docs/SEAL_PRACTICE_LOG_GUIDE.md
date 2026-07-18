# Kadim Mühür Teknikleri - Log Takip Rehberi

## Logcat Filtreleme

Kadim Mühür Teknikleri özelliğini test ederken, binlerce alakasız log arasında kaybolmamak için aşağıdaki filtreleme yöntemlerini kullanabilirsiniz.

### 1. Android Studio Logcat Filtresi

**Yöntem A: Tag'e Göre Filtreleme**
```
tag:GameLogger
```

Bu filtre, yalnızca GameLogger tag'ine sahip logları gösterir.

**Yöntem B: Kategori Bazlı Filtreleme (SEAL_PRACTICE)**
```
SEAL_PRACTICE
```

Bu filtre, yalnızca Seal Practice ile ilgili logları gösterir.

**Yöntem C: Birleşik Filtre (En Detaylı)**
```
tag:GameLogger SEAL_PRACTICE
```

### 2. ADB Logcat Komut Satırı

Terminal'den logları takip etmek isterseniz:

```bash
# Yalnızca SEAL_PRACTICE logları
adb logcat | Select-String "SEAL_PRACTICE"

# GameLogger tag'li tüm loglar
adb logcat -s GameLogger

# Dosyaya kaydet
adb logcat -s GameLogger > seal_practice_logs.txt
```

## Log Kategorileri ve Anlamları

### Initialization Logları
```
[SEAL_PRACTICE] SealPracticeViewModel initialized
[SEAL_PRACTICE] Initializing 5 default seals for player level 1
[SEAL_PRACTICE]   ✓ Unlocked Seal: Temel Güçlendirme Mührü (ID: seal_basic_enhancement, MinLevel: 1)
```
**Anlam**: Sistem başlatılıyor, varsayılan mühürler yükleniyor.

### Screen Rendering Logları
```
[SEAL_PRACTICE] TrainingScreen composable rendered
[SEAL_PRACTICE] SpiritualCultivationHall composable rendered - Kadim Mühür button should be visible
```
**Anlam**: UI ekranları render ediliyor. Buton görünür olmalı.

### Navigation Logları
```
[SEAL_PRACTICE] Kadim Mühür Teknikleri button clicked - Navigating to seal_practice
[SEAL_PRACTICE] SealPracticeScreen composable rendered
```
**Anlam**: Kullanıcı butona tıkladı ve Seal Practice ekranına yönlendirildi.

### Seal Selection Logları
```
[SEAL_PRACTICE] Loading available seals for player level 1 (Total unlocked: 5, Mastered: 0)
[SEAL_PRACTICE] Available seals for practice: 5 out of 5 total seals
[SEAL_PRACTICE]   • Temel Güçlendirme Mührü (Mastery: 0%)
```
**Anlam**: Mevcut mühürler listeleniyor.

### Practice Session Logları
```
[SEAL_PRACTICE] ========================================
[SEAL_PRACTICE] PRACTICE SESSION STARTED: Temel Güçlendirme Mührü
[SEAL_PRACTICE]   Difficulty: NOVICE
[SEAL_PRACTICE]   Current Mastery: 0%
[SEAL_PRACTICE]   Hand Shape: OPEN_PALM
[SEAL_PRACTICE] ========================================
```
**Anlam**: Pratik oturumu başladı.

### Practice Attempt Logları
```
[SEAL_PRACTICE] Practice attempt: Temel Güçlendirme Mührü - ✨ PERFECT (Accuracy: 95%, Total: 1, Success Rate: 100%)
[SEAL_PRACTICE] Mastery update: Temel Güçlendirme Mührü -> 5% (from 0%, +5)
```
**Anlam**: Kullanıcı bir deneme yaptı ve başarılı oldu. Ustalık arttı.

### Mastery Achievement Logları
```
[SEAL_PRACTICE] 🎉 SEAL MASTERED: Temel Güçlendirme Mührü - Related skills will be unlocked
[SEAL_PRACTICE] Mastery update: Temel Güçlendirme Mührü -> 100% (from 95%, +5)
```
**Anlam**: Mühür %100 ustalaştırıldı, ilgili yetenekler açılacak.

### Session End Logları
```
[SEAL_PRACTICE] ========================================
[SEAL_PRACTICE] PRACTICE SESSION ENDED: Temel Güçlendirme Mührü
[SEAL_PRACTICE]   Duration: 2 minutes
[SEAL_PRACTICE]   Attempts: 15
[SEAL_PRACTICE]   Successes: 12
[SEAL_PRACTICE]   Success Rate: 80%
[SEAL_PRACTICE]   Best Accuracy: 98%
[SEAL_PRACTICE] ========================================
```
**Anlam**: Pratik oturumu sonlandı, özet istatistikler.

## Hata Ayıklama Senaryoları

### Sorun: Butonu göremiyorum
**Bakılacak Loglar**:
```
[SEAL_PRACTICE] TrainingScreen composable rendered
[SEAL_PRACTICE] SpiritualCultivationHall composable rendered - Kadim Mühür button should be visible
```
**Beklenen**: İki log da görülmeli. Görülmüyorsa TrainingScreen render olmuyor demektir.

### Sorun: Hiç mühür yok
**Bakılacak Loglar**:
```
[SEAL_PRACTICE] Initializing 5 default seals for player level X
[SEAL_PRACTICE] SealSelectionScreen: 0 seals available
[SEAL_PRACTICE] ⚠️ WARNING: No seals available! Check initializeDefaultSeals()
```
**Beklenen**: İlk log "Initializing 5 default seals" görülmeli. 0 seals available uyarısı görülüyorsa initializeDefaultSeals() çalışmamış demektir.

### Sorun: El algılama çalışmıyor
**Bakılacak Loglar**:
```
[SEAL_PRACTICE] PRACTICE SESSION STARTED: ...
```
Sonrasında hiç "Practice attempt" logu gelmiyorsa, hand detection çalışmıyor demektir.
(Hand detection'ın kendi logları "SealPracticeScreen" tag'i altında olacaktır, GameLogger değil)

## Log Dosyası

Tüm SEAL_PRACTICE logları ayrıca şu dosyaya kaydedilir:
```
/data/data/com.example.isekaikuroshin/files/game_logs.txt
```

Bu dosyayı çekmek için:
```bash
adb pull /data/data/com.example.isekaikuroshin/files/game_logs.txt
```

## Örnek Test Senaryosu

1. **Uygulamayı başlat**
   - Beklenen log: `[SEAL_PRACTICE] SealPracticeViewModel initialized`

2. **Training ekranına git**
   - Beklenen log: `[SEAL_PRACTICE] TrainingScreen composable rendered`
   - Beklenen log: `[SEAL_PRACTICE] SpiritualCultivationHall composable rendered`

3. **"Kadim Mühür Teknikleri" butonuna tıkla**
   - Beklenen log: `[SEAL_PRACTICE] Kadim Mühür Teknikleri button clicked`
   - Beklenen log: `[SEAL_PRACTICE] SealPracticeScreen composable rendered`

4. **Bir mühür seç**
   - Beklenen log: `[SEAL_PRACTICE] PRACTICE SESSION STARTED: ...`

5. **El hareketini yap**
   - Beklenen log: `[SEAL_PRACTICE] Practice attempt: ... - ✨ PERFECT/✓ SUCCESS/✗ FAILED`

6. **Oturumu bitir**
   - Beklenen log: `[SEAL_PRACTICE] PRACTICE SESSION ENDED: ...`

## Performans Notları

- SEAL_PRACTICE logları **DEBUG** seviyesindedir, performansa minimal etki eder
- Hand detection gibi tekrarlayan işlemler **VERBOSE** seviyesinde değildir
- Tüm loglar hem Logcat'e hem de `/files/game_logs.txt` dosyasına yazılır
- VERBOSE loglar dosyaya yazılmaz (dosya şişmesin diye)

---

**Not**: Eğer logları göremiyorsanız, Logcat'teki "Show only selected application" filtresinin açık olduğundan emin olun.
