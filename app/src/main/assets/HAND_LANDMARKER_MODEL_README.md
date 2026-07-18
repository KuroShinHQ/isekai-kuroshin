# MediaPipe Hand Landmarker Model

## Gerekli Model Dosyası

**Dosya Adı:** `hand_landmarker.task`

**İndirme Linki:**
```
https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
```

**Boyut:** ~3 MB

**Yerleştirilmesi Gereken Klasör:**
```
app/src/main/assets/hand_landmarker.task
```

## İndirme ve Kurulum Adımları

### Option 1: Manuel İndirme (Windows)
```powershell
# PowerShell ile indirme
$url = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task"
$output = "C:\Users\pc\AndroidStudioProjects\IsekaiKuroshin\app\src\main\assets\hand_landmarker.task"
Invoke-WebRequest -Uri $url -OutFile $output
```

### Option 2: Web Tarayıcı
1. Yukarıdaki linke tıkla
2. İndirilen `hand_landmarker.task` dosyasını bu klasöre taşı:
   `app/src/main/assets/hand_landmarker.task`

### Option 3: curl (Git Bash veya WSL)
```bash
curl -L -o app/src/main/assets/hand_landmarker.task \
  https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
```

## Doğrulama

Model dosyası doğru yüklendiyse, `assets` klasöründe şu dosya görünmelidir:
```
app/src/main/assets/
├── gemma3-1b-it-int4.litertlm
├── stitch_ai_designs/
├── stories/
└── hand_landmarker.task  <-- YENİ DOSYA
```

## Teknik Detaylar

- **Model Tipi:** Float16 (optimized for mobile)
- **Landmarks:** 21 nokta per hand
- **Hands:** Maksimum 2 el (configurable)
- **Performance:** 30-45 FPS (GPU delegate ile)
- **RAM Kullanımı:** ~150-200 MB
- **Offline:** Tam offline çalışır

## Kullanım

HandDetectorHelper sınıfı bu model dosyasını otomatik yükler:

```kotlin
val baseOptions = BaseOptions.builder()
    .setModelAssetPath("hand_landmarker.task")  // Bu dosya
    .setDelegate(BaseOptions.Delegate.GPU)
    .build()
```

---

**ÖNEMLİ:** Bu model dosyası olmadan HandDetectorHelper başlatılamaz!
