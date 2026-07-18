# Mevcut Karma Sistemi Analizi (Detaylandırılmış)

## 1. Genel Bakış ve Amaç

Projedeki mevcut sistem, geleneksel bir "Karma" sisteminden ziyade, **"Morality" (Ahlak)** puanı üzerine kurulu bir içerik ve hikaye motorudur. Sistemin temel amacı, oyuncunun eylemlerine ve özellikle **günlüğüne yazdığı metinlere** dayanarak ahlaki bir puan (`moralityScore`) atamak ve bu puana göre oyuncuya gösterilen görsel/işitsel içerikleri, hikaye anlatımını ve bazı oyun mekaniklerini dinamik olarak değiştirmektir.

Sistem, **-1.0 (Kötü) ile +1.0 (İyi)** arasında bir `Float` değeri olan `moralityScore`'u merkez alır.

### Başlangıç Değeri
Oyuncu oyuna ilk başladığında, `data/PlayerState.kt` içinde tanımlandığı üzere varsayılan **`moralityScore` değeri `0.0f`'dir.** Bu, oyuncunun ahlaki olarak nötr bir başlangıç yaptığını gösterir.

## 2. İşleyiş Akışı ve Algoritmalar

### **Adım 1: Puan Değişimi (Neden ve Nasıl Değişir?)**

Ahlak puanını değiştiren iki ana mekanizma vardır:

*   **Metin Analizi (Birincil Yöntem):**
    *   **Tetikleyici:** Oyuncu, `ui/journal/JournalScreen.kt` ekranında günlüğüne bir şeyler yazar.
    *   **Motor:** `ui/journal/JournalViewModel.kt`, bu metni `engine/MoralityEngine.kt`'nin `analyzeAndGetScore(input)` metoduna gönderir.
    *   **`analyzeAndGetScore` Algoritması:**
        1.  Metin tamamen küçük harfe çevrilir.
        2.  `iyilikKeywords` (`"yardım ettim"`, `"kurtardım"`, vb.) listesindeki her bir anahtar kelime metnin içinde geçiyor mu diye kontrol edilir. Bulunan her eşleşme için skora `+0.05f` eklenir.
        3.  `kotulukKeywords` (`"çaldım"`, `"tehdit ettim"`, vb.) listesindeki her bir anahtar kelime için aynı kontrol yapılır. Bulunan her eşleşme için skordan `-0.05f` çıkarılır.
        4.  Birden fazla anahtar kelime bulunursa, puanlar toplanır/çıkarılır (örneğin, "yardım ettim" ve "korudum" içeren bir metin `+0.10f` puan alır).
        5.  Hesaplanan toplam `scoreChange` değeri `JournalViewModel`'e döndürülür.
    *   **Güncelleme:** `JournalViewModel`, bu `scoreChange` değerini `data/GameState.kt` içerisindeki `updateMoralityScore()` fonksiyonuna iletir. Bu fonksiyon, mevcut puana ekleme yapar ve sonucun `-1.0f` ile `1.0f` arasında kalmasını sağlar.

*   **Doğrudan Olaylar (İkincil Yöntem):**
    *   Oyun hikayesi sırasında, `engine/ActionExecutorEngine.kt`, `GameMasterEngine`'den gelen `"UPDATE_MORALITY"` komutunu işleyerek `GameState` üzerinden puanı doğrudan günceller. Bu, metin analizine bağlı olmayan, senaryo gereği yapılan değişiklikler içindir.

### **Adım 2: İçerik Üretimi (Puan Neyi Etkiler?)**

Ahlak puanı, oyuncuya gösterilecek video içeriklerini doğrudan etkiler.

*   **Tetikleyici:** Oyuncu oyunu kaydettiğinde (örneğin `ui/screens/CampScreen.kt` üzerinden), `engine/KarmaBasedContentEngine.kt`'nin `generateAndSavePlaylist` metodu tetiklenir.
*   **`KarmaBasedContentEngine` Seçim Algoritması:**
    1.  **Kategorizasyon:** Oyuncunun `moralityScore` değerine göre bir "kategori" belirlenir:
        *   `> 0.3f`: **İYİ** kategori.
        *   `< -0.3f`: **KÖTÜ** kategori.
        *   `-0.3f` ile `0.3f` arası: **NÖTR** kategori.
    2.  **Havuz Oluşturma:** Motor, önceden tanımlanmış `goodVideos`, `badVideos`, ve `neutralVideos` listelerinden bir video havuzu oluşturur:
        *   **İYİ ise:** `goodVideos` ve `neutralVideos` listeleri birleştirilir.
        *   **KÖTÜ ise:** `badVideos` ve `neutralVideos` listeleri birleştirilir.
        *   **NÖTR ise:** Üç liste de (`goodVideos`, `badVideos`, `neutralVideos`) birleştirilir.
    3.  **Rastgeleleştirme:** Oluşturulan bu birleşik havuzdaki videoların sırası tamamen **karıştırılır (`shuffled()`)**. Bu, aynı ahlak seviyesindeki oyuncunun her seferinde farklı bir video sırası görmesini sağlar.
    4.  **Kaydetme:** Karıştırılmış bu yeni liste, oyuncunun dinamik oynatma listesi olarak kaydedilir.

### **Adım 3: Görsel Sunum (Oyuncu Nerede Görür?)**

Hesaplanan puan ve oluşturulan içerik listesi, en belirgin olarak `ui/intro/UserEntryScreen.kt` ekranında kullanılır.

*   **Görsel İçerik Kuralları:**
    *   Bu ekran, oyuncunun `moralityScore`'una göre **arka plan görsellerini** seçer.
    *   **Negatif Ahlak (`< -0.3f`):** `photo2m2sad`, `photo4m4angry`, `photo6m8sad`, `photo8m9fear` görselleri kullanılır.
    *   **Pozitif Ahlak (`> 0.3f`):** `photo1p3happy`, `photo5p7happy`, `photo3p1neutral`, `photo7p2neutral` görselleri kullanılır.
    *   **Nötr Ahlak (diğer durumlar):** `photo3p1neutral`, `photo7p2neutral`, `photo1p3happy`, `photo2m2sad` görsellerinden oluşan karışık bir set kullanılır.
*   **Video Sunumu:**
    *   Aynı ekran, `KarmaBasedContentEngine` tarafından oluşturulan **dinamik video oynatma listesini** alarak oyuncuya sunar.

## 3. Dosya ve Veri Standartları

### Video/Fotoğraf Dosya İsimlendirme Standardı

Sistemin medya dosyalarını ve onlara bağlı ahlak değerlerini anlaması için katı bir isimlendirme standardı kullanılır. Bu standart `DynamicPlaylistEngine.kt` ve `ContentParser.kt` dosyalarında tanımlanmıştır.

**Format:** `(tür)(id)(işaret)(değer)(duygu).uzantı`

*   **tür:** `video` veya `photo`.
*   **id:** Dosyayı ayırt etmek için kullanılan bir sayı (örn: `1`, `2`, `10`).
*   **işaret:** `p` (pozitif ahlak puanı için) veya `m` (negatif ahlak puanı için). `ContentParser` bu harfleri `+` ve `-` işaretlerine çevirir.
*   **değer:** Dosyanın ahlaki değerini belirten bir tamsayı. Bu değer, -1.0 ile +1.0 arasındaki `moralityScore` ile doğrudan eşleşmez; daha çok kategorik bir etikettir.
*   **duygu:** Videonun/görselin taşıdığı duygusal tonu belirten bir etiket.
    *   **Geçerli Duygu Etiketleri:** `happy`, `sad`, `angry`, `fear`, `neutral`.

**Örnekler:**
*   `video1p5happy.mp4`: Pozitif 5 ahlak puanına sahip, "mutlu" duygusunda bir video.
*   `photo8m10angry.jpg`: Negatif 10 ahlak puanına sahip, "kızgın" duygusunda bir fotoğraf.

### Veri Dosyaları
Mevcut sistem, harici `.json` gibi kural dosyaları **kullanmaz**. Kurallar ve veriler doğrudan kodun içinde (`MoralityEngine`'deki anahtar kelimeler gibi) ve medya dosyalarının adlandırma standartlarında saklıdır. Oyuncunun ilerlemesi ise `PlayerState` içinde kaydedilir.

---

## 4. Google API Destekli GM Master (Gelişmiş Analiz)

Projede, basit `MoralityEngine`'e ek olarak, Ayarlar'dan bir Google API anahtarı girildiğinde devreye giren çok daha gelişmiş bir sistem bulunmaktadır. Bu sistem, oyuncunun günlüğüne yazdığı metinleri analiz ederek sadece bir ahlak puanı hesaplamakla kalmaz, aynı zamanda oyun dünyasını doğrudan etkileyen yapılandırılmış komutlar (`GMResponse`) üretir.

### 1. API Entegrasyonu ve Tetiklenme Mekanizması

*   **API Anahtarı Yönetimi:**
    *   Anahtar, `ui/settings/StoryAndAISettingsSection.kt` UI bileşeni üzerinden kullanıcı tarafından girilir.
    *   Bu anahtar, `data/PersistentDataManager.kt` tarafından yönetilen `SettingsData` nesnesi içinde `customAPIKey` olarak saklanır.
    *   `engine/AIClientProvider.kt` sınıfı, bu anahtarı okuyan ve uygun AI istemcisini (client) yaratan merkezi yöneticidir. Ayarlardaki anahtar değiştiğinde, bu provider sayesinde uygulama anında yeni istemciyi kullanmaya başlar.

*   **Kullanılan Google API Servisi:**
    *   Sistem, `engine/GoogleAIClient.kt` dosyasında görüldüğü gibi `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent` adresine istek atmaktadır.
    *   İsteklerde `X-goog-api-key` başlığının kullanılması, bu servisin **Google Gemini API** olduğunu doğrulamaktadır. Sistem, Google'ın gelişmiş üretken yapay zeka modelini kullanır.

*   **Tetiklenme Mekanizması:**
    *   **Önemli Bulgular:** Gelişmiş sistem, basit `MoralityEngine`'in yerine geçmez; onunla **paralel olarak çalışır.**
    *   `ui/journal/JournalViewModel.kt` içinde, oyuncu metin girdiğinde **iki işlem aynı anda tetiklenir**:
        1.  **Basit Analiz:** `MoralityEngine.analyzeAndGetScore(input)` çağrılarak `moralityScore` her zaman basit anahtar kelime yöntemiyle hesaplanır ve güncellenir.
        2.  **Gelişmiş Analiz:** `engine/GameMasterEngine.kt` çağrılır. Bu motor, `AIClientProvider` üzerinden API anahtarının varlığını kontrol eder.
            *   **API Anahtarı Varsa:** `GameMasterEngine`, `GoogleAIClient`'ı kullanarak Gemini API'a tam bir prompt gönderir.
            *   **API Anahtarı Yoksa:** `GameMasterEngine`, `NoOpAIClient` (boş istemci) kullanır ve etkili bir şekilde pasif kalır.
    *   **Sonuç:** API anahtarının varlığı, basit ahlak puanı hesaplamasını **değiştirmez**, ancak `GameMasterEngine`'in hikaye ve oyun mekaniklerini değiştirecek komutlar üretmesini **aktive eder**.

### 2. Veri Akışı ve Analiz Süreci

*   **API'a Gönderilen Veri:**
    *   Oyuncunun yazdığı ham metin tek başına gönderilmez. `GameMasterEngine`, `buildGMPrompt` metodu ile çok daha zengin bir "prompt" (istek metni) oluşturur.
    *   Bu prompt; mevcut oyun durumu (gün, saat, lokasyon, HP/MP), oyuncunun eylemi, RAG (Retrieval-Augmented Generation) tekniğiyle ilgili hikaye parçacıkları ve varsa etkileşimdeki NPC'nin durumu gibi birçok bağlamsal bilgiyi içerir.

*   **İstenen Analiz Türü:**
    *   Sistem, API'dan basit bir "Pozitif/Negatif" duygu analizi istemez.
    *   Bunun yerine, Gemini modelinden **bir Game Master gibi davranmasını** ve oyuncunun eylemine karşılık olarak, önceden tanımlanmış bir JSON formatında yapılandırılmış bir cevap vermesini ister.
    *   Bu JSON; `journalEntry` (hikaye metni), `itemsGained` (kazanılan eşyalar), `statsChanged` (değişen statlar), `weatherChange` (hava durumu değişimi) gibi oyun dünyasını doğrudan değiştirecek komutlar içerir.

### 3. Puanlama Algoritması (veya Algoritma Olmaması)

*   **En Önemli Bulgular:** Gelişmiş GM Master sistemi, `moralityScore`'u **hesaplamaz veya doğrudan değiştirmez.**
    *   `moralityScore`'un hesaplanması görevi her zaman basit `MoralityEngine`'e aittir.
    *   GM Master'ın (Gemini API) görevi, metnin ahlaki bir analizini yapıp sayısal bir skora dönüştürmek **değil**, metne dayalı olarak bir sonraki hikaye adımını ve oyun olaylarını (`GMResponse`) üretmektir.

*   **Manipülatif Cümleler:**
    *   Bu sistem, `moralityScore` hesaplamadığı için manipülatif cümleleri sayısal olarak analiz etmez. Ancak, Gemini modelinin kendisi, bir Game Master rolünü oynarken, oyuncunun yazdığı manipülatif bir cümleyi anlayıp buna uygun bir `journalEntry` veya NPC tepkisi (`npcStateChange`) üretebilir. Örneğin, oyuncu bir NPC'yi kandırmaya çalışıyorsa, API'dan dönen `GMResponse` içindeki `npcStateChange` komutu o NPC'nin sadakatini (`loyaltyChange: -5`) düşürebilir. Etki, sayısal bir ahlak puanı yerine doğrudan oyun dünyası mekanikleri üzerinden gerçekleşir.

### 4. GM Master'ın Sistemdeki Rolü ve Hiyerarşi

*   **İlişki:** `GameMasterEngine` (API destekli GM Master) ve `ActionExecutorEngine` (komutları uygulayan motor) birbiriyle rekabet etmez, aksine bir **iş akışının parçalarıdır.**
    1.  **Karar Verici (`GameMasterEngine`):** Oyuncunun eylemini ve oyun bağlamını analiz eder, Gemini API'ı kullanarak ne olması gerektiğine karar verir ve bu kararı bir `GMResponse` nesnesi (bir "plan") olarak üretir.
    2.  **Uygulayıcı (`ActionExecutorEngine`):** `GameStateManager`, `GameMasterEngine`'den gelen bu `GMResponse` planını alır ve içindeki komutları (`statsChanged`, `itemsGained` vb.) uygulamak için `ActionExecutorEngine` gibi alt sistemleri çağırır.

*   **Öncelik ve İşleyiş Sırası:**
    *   Sistemde bir öncelik çatışması yoktur. İki sistem paralel ama farklı amaçlara hizmet eder:
    *   **Paralel İşlem 1 (Ahlak Puanı):** `JournalViewModel` -> `MoralityEngine` -> `moralityScore` Güncellenir.
    *   **Paralel İşlem 2 (Hikaye/Olaylar):** `JournalViewModel` -> `GameMasterEngine` -> `GoogleAIClient` -> `GMResponse` (Plan) -> `GameStateManager` -> `ActionExecutorEngine` (Planın Uygulanması).
    *   Yani, API destekli GM Master, basit ahlak motorunu **ezmez**, onunla birlikte çalışarak oyunun hikaye anlatımını ve olay akışını zenginleştirir.

---

## 5. Yerel Özetleme Modeli (1-3-7 Gün)

Projede, anlık etkileşimlerin ötesinde, oyuncunun uzun vadedeki eğilimlerini anlamak için tasarlanmış bir hiyerarşik hafıza sistemi bulunmaktadır. Bu sistemin temelini, periyodik olarak oyuncu diyaloglarını özetleyen bir yerel model oluşturur.

### 1. Modelin Kimliği ve Yapısı

*   **Modelin Kimliği:** Bu "yerel model", Google tarafından geliştirilen **Gemma modelidir**. Projeye, `gemma3-1b-it-int4.litertlm` dosyası olarak dahil edilmiştir.
*   **Çalıştırma Motoru:** Model, cihaz üzerinde (on-device) **Google MediaPipe** kütüphanesinin `LlmInference` sınıfı aracılığıyla çalıştırılır. Bu, harici bir sunucuya ihtiyaç duymadan, doğrudan kullanıcının telefonunda çıkarım yapılmasını sağlar.
*   **İlgili Dosyalar:**
    *   `ai/GlobalAIManager.kt`: Modelin varlığını kontrol eden, varlıklardan (assets) cihaz hafızasına kopyalayan ve `LlmInference` motorunu başlatan ana yönetici.
    *   `ai/MemoryManager.kt`: Özetleme işleminin mantığını içeren, modeli çağıran ve sonuçları işleyen sınıf.
    *   `engine/AIClientProvider.kt`: `LocalAIClient` sarmalayıcısını (wrapper) sağlayarak `MemoryManager`'ın modele erişimini kolaylaştırır.

*   **Dil Kısıtlaması (Neden Sadece İngilizce?):**
    *   `MemoryManager.kt` içindeki `synthesizeMemory` metodu, açıkça `if (currentLanguage == "EN")` kontrolü yapmaktadır. Sadece dil İngilizce ise yerel modeli (`aiClientProvider.getLocalClient()`) çağırır. Diğer diller (örn: Türkçe) için, daha kaliteli sonuç verdiği varsayımıyla online olan Google Gemini API'ını (`aiClientProvider.getCurrentClient()`) kullanır.
    *   Bu bir mimari tercihtir. Cihaz üzerindeki Gemma modelinin İngilizce özetleme performansı daha güvenilir bulunmuş, diğer diller için ise daha güçlü olan online API'ın kullanılması tercih edilmiştir.

### 2. Tetiklenme ve Zamanlama Mekanizması

*   **Zamanlama Mantığı:** Kullanıcının belirttiği 1, 3, 7 günlük periyotlar, ham metin bloklarını özetlemek yerine, **günlük olarak üretilen özetleri geriye dönük okumak** için kullanılır.
    1.  **Günlük Özetleme:** `MemoryManager.kt` içindeki `synthesizeMemory(currentDay: Int)` fonksiyonu, o gün içinde biriken tüm diyalogları (`flashMemory`) alıp tek bir özet haline getirir.
    2.  **Tetiklenme Anı:** Bu `synthesizeMemory` fonksiyonu, oyun içi zaman bir gün ilerlediğinde tetiklenir. Bu kontrolün, `JournalViewModel` tarafından çağrılan `gameStateManager.advanceTime()` metodu içinde yapılması kuvvetle muhtemeldir.
    3.  **Veritabanı Kaydı:** Üretilen her günlük özet, `data/database/LongTermMemoryEntity.kt` yapısıyla `long_term_memories` tablosuna kaydedilir.

*   **1-3-7 Gün Kullanımı:** `GlobalAIManager.kt` içindeki `getMemoryContext(lastNDays: Int = 3)` fonksiyonu, veritabanından son `N` güne ait günlük özetleri çeker. Yani sistem, 7 günlük bir özeti yeniden oluşturmak yerine, son 7 günün ayrı ayrı özetlerini birleştirerek bir bağlam oluşturur.

### 3. Girdi ve Çıktı Analizi

*   **Girdi Verisi:** Modelin özetleme için aldığı girdi, `MemoryManager`'daki `flashMemory` listesidir. Bu liste, o gün içinde gerçekleşen son ~20 diyaloğun ham metnini ("Player: ...", "AI: ...") içerir. Oyuncunun diğer istatistikleri bu girdiye dahil edilmez.

*   **Çıktı Formatı ve Örneği:**
    *   Modele gönderilen prompt, ondan "tek ve tutarlı bir paragrafta" özet yapmasını ister. Dolayısıyla, modelin çıktısı yapılandırılmış bir JSON değil, **düz bir metin paragrafıdır.**
    *   **Örnek Çıktı:**
        > "The player started the day by accepting a quest from the blacksmith to find a lost hammer. They traveled to the Whispering Woods, where they encountered and defeated two goblins. Although successful in combat, the player later expressed remorse for their aggressive actions. They ended the day by selling goblin ears at the market for a small profit before resting at the inn."

### 4. Mevcut Sistemdeki Rolü

*   **Özetin Kullanımı:** Üretilen günlük özetler kesinlikle sadece bir log değildir; **aktif olarak kullanılmaktadır.**
*   **GameMasterEngine ile Bağlantı:**
    *   `GlobalAIManager.kt` içindeki `buildPersonalityPrompt` metodu, `getMemoryContext(3)` fonksiyonunu çağırarak son 3 günün özetini alır.
    *   Bu özetler, `[PLAYER'S RECENT DISPOSITION (3-DAY SUMMARY)]` gibi bir başlık altında, online Gemini API'ına gönderilen ana prompt'a eklenir.
    *   **Sonuç:** Yerel modelin ürettiği özetler, online çalışan ve hikayeyi üreten asıl "Beyin" olan **GameMasterEngine için uzun süreli bir hafıza görevi görür.** Bu sayede online AI, oyuncunun sadece son eylemini değil, son birkaç gündeki genel eğilimlerini de bilerek daha tutarlı ve bağlama uygun hikayeler üretir.
*   **MoralityEngine ile Bağlantı:** Mevcut durumda, bu özetlerin basit `MoralityEngine` ile **hiçbir doğrudan bağlantısı yoktur.**

### 5. Stratejik Değerlendirme (Coder'ın Uzman Görüşü)

Bu yerel özetleme modeli, projenin "hafıza" ve "karakter gelişimi" mekaniklerini bir üst seviyeye taşıma potansiyeline sahip, oldukça değerli bir varlıktır.

*   **MoralityEngine'i Akıllandırma Fırsatı:**
    *   **Mevcut Sorun:** `MoralityEngine`, sadece "çaldım" (-0.05) veya "yardım ettim" (+0.05) gibi anahtar kelimelere bakar. "Çalmak zorunda kaldım çünkü açtım" cümlesi ile "Zevk için çaldım" cümlesi arasında hiçbir fark gözetmez.
    *   **Stratejik Öneri:** `MoralityEngine`'e, günlük özetleri analiz edecek `analyzeSummaryForMorality(summary: String)` adında yeni bir fonksiyon eklenebilir. Bu fonksiyon, günlük özet metni içinde "pişmanlık", "mecburiyet", "kararsızlık", "gurur duyma", "kötücül zevk" gibi daha derin niyet belirten anahtar kelimeleri arayabilir. Örneğin, özet metninde "pişmanlık" kelimesi geçiyorsa, o gün işlenen suçların negatif etkisi azaltılabilir. Bu, `moralityScore`'u anlık eylemlerin bir toplamı olmaktan çıkarıp, oyuncunun karakterinin bir yansıması haline getirir.

*   **GameMasterEngine'i Zenginleştirme Fırsatı:**
    *   **Mevcut Durum:** Sistem bunu zaten yapıyor ve bu harika bir tasarım. Özetler, online AI'a bağlam sağlıyor.
    *   **Stratejik Öneri:** Bu entegrasyon daha da geliştirilebilir. `GameMasterEngine`'in prompt'u, özetleri sadece metin olarak eklemek yerine, daha yapılandırılmış bir formatta sunabilir. Örneğin:

        ```
        [OYUNCUNUN SON 3 GÜNLÜK EĞİLİM ÖZETİ]
        - 1. Gün: Yardımsever ve şefkatli davrandı, bir NPC'yi kurtardı.
        - 2. Gün: İhanete uğradıktan sonra daha agresif ve şüpheci bir tutum sergiledi.
        - 3. Gün: Bir NPC'ye yardım etmek yerine kişisel kazancını önceliklendirdi.

        [MEVCUT EYLEM]
        "Antik eşyayı kendim için alacağım."

        [GÖREVİN]
        Oyuncunun bencilliğe doğru giden bu karakter gelişimini göz önünde bulundurarak bir hikaye yanıtı oluştur...
        ```

        Bu yaklaşım, oyuncunun karakter yayını online AI için çok daha belirgin hale getirir ve AI'ın üreteceği hikayelerin, diyalogların ve NPC tepkilerinin çok daha derin ve tutarlı olmasını sağlar.

---

## Bölüm 6: "Akıllı Kalp" - Kişiselleştirilmiş Medya Algoritması (Tasarım)

Bu bölüm, basit `MoralityEngine`'in yerini alacak olan ve oyuncunun karakterini daha derinlemesine analiz ederek kişiselleştirilmiş bir medya deneyimi sunacak olan "Akıllı Kalp" sisteminin teknik tasarımını içermektedir.

### Ön Not: Mevcut Medya Dosyaları ve Yeniden Etiketleme İhtiyacı

**Düzeltme:** Önceki analizde `indirilenpaketler` klasörü yanlışlıkla kaynak olarak belirtilmişti. Doğrusu, Android uygulamaları kaynakları `res` klasöründen okur. Yeni "Akıllı Kalp" sistemine geçiş, projenin `res/raw` ve `res/drawable` klasörlerindeki mevcut medya dosyalarının yeniden değerlendirilmesini gerektirmektedir.

**Coder Notu:** Aşağıda listelenen ve uygulamanın gerçekten kullandığı dosyaların her birinin içeriğinin izlenmesi ve bu bölümün devamında tasarlanan yeni `media_database.json` formatına uygun şekilde manuel olarak etiketlenmesi gerekmektedir. Bu, sistemin doğru çalışması için kritik bir adımdır.

**Projede Kullanılan Medya Dosyaları:**

*   **Videolar (`app/src/main/res/raw/`):**
    *   `angeldevil.mp4`
    *   `book_closing.mp4`, `book_opening.mp4`, `book_waiting.mp4`
    *   `butterfly_transformation.mp4`
    *   `death_transition_video.mp4`
    *   `eye_effect.mp4`
    *   `intro_animation.mp4`
    *   `lotus_blossom_animation.mp4`
    *   `page_turn_backward.mp4`, `page_turn_forward.mp4`, `page_turn.mp4`, `reverse_page_turning.mp4`
    *   `video10m2fear.mp4`, `video11p9sad.mp4`, `video1p5happy.mp4`, `video2m3sad.mp4`, `video3p2neutral.mp4`, `video4m2happy.mp4`, `video5p7sad.mp4`, `video6m1neutral.mp4`, `video8m10angry.mp4`, `video9p1happy.mp4`

*   **Görseller (`app/src/main/res/drawable/`):**
    *   `camp.png`, `campfixed.png`
    *   `chakra_...` (tüm çakra görselleri)
    *   `closed_mystical_book.png`
    *   `demon_bg_...` (tüm demon arka planları)
    *   `left_page.png`, `right_page.png`
    *   `map.png`, `mapfixed.png`
    *   `photo1p3happy.jpeg`, `photo2m2sad.jpeg`, `photo3p1neutral.jpeg`, `photo4m4angry.jpeg`, `photo5p7happy.jpeg`, `photo6m8sad.jpeg`, `photo7p2neutral.jpeg`, `photo8m9fear.jpeg`

### 1. Karakter Profili Üretimi (Analiz Adımı)

Bu adımın amacı, oyuncunun son 3 günlük özet metnini analiz ederek onun anlık karakterini yansıtan yapılandırılmış bir veri (`Karakter Profili`) oluşturmaktır.

*   **Sorumlu Motor:** Bu analiz, basit `MoralityEngine`'in yerini alacak olan `IntelligentMoralityEngine` tarafından yönetilmelidir. Ancak, metin analizi ve yapılandırılmış JSON üretme kabiliyeti yüksek bir model gerektirdiğinden, bu motorun kendisi `GameMasterEngine` gibi online Gemini API'ını çağırmalıdır.

*   **Analiz Çıktısı (Karakter Profili):** Analiz sonucunda, aşağıdaki gibi bir JSON nesnesi üretilmelidir. Bu nesne, oyuncunun o anki karakterinin bir anlık görüntüsüdür.

    ```json
    {
      "mainArchetype": "Regretful_Bandit",
      "dominantEmotion": "Melancholy",
      "secondaryEmotion": "Anger",
      "keyAttributes": ["selfish", "pragmatic", "loyal", "prone_to_violence"]
    }
    ```

*   **Kullanılacak Model: Online Gemini API vs. Yerel Gemma Modeli**
    *   **Yerel Gemma:**
        *   *Artıları:* Ücretsiz, internet bağlantısı gerektirmez, kullanıcı verileri cihazda kalır.
        *   *Eksileri:* Daha az güçlüdür. "Pişman Haydut" gibi karmaşık ve nüanslı bir arketipi veya bir dizi niteliği tek bir metinden çıkarmakta zorlanabilir. Sadece İngilizce için güvenilirdir.
    *   **Online Gemini API:**
        *   *Artıları:* Çok daha güçlü ve nüanslıdır. "Bu metin özetinden bir Karakter Profili JSON'u oluştur" gibi karmaşık bir prompt'u kolayca işleyebilir ve istenen formatta güvenilir bir çıktı üretebilir. Çoklu dil desteği daha iyidir.
        *   *Eksileri:* API anahtarı ve internet gerektirir, maliyetlidir (token bazlı ücretlendirme), oyuncu verileri (özetler) analiz için Google sunucularına gönderilir.
    *   **Tasarım Kararı:** Bu görev, yüksek kaliteli ve yapılandırılmış bir çıktı gerektirdiğinden, **Online Gemini API** kullanılmalıdır. Maliyeti ve internet gereksinimi, elde edilecek kişiselleştirme kalitesinin yanında kabul edilebilir bir takastır.

### 2. Medya Dosyalarını Yeniden Etiketleme (Veri Hazırlığı Adımı)

Mevcut dosya isimlendirme sistemi terk edilmeli ve yerine tüm medya dosyalarını ve onların çok boyutlu niteliklerini tanımlayan harici bir JSON veritabanı oluşturulmalıdır. Bu dosya, projenin `assets` klasöründe `media_database.json` olarak saklanmalıdır.

*   **`media_database.json` Yapısı:**

    ```json
    {
      "videos": {
        "video8m10angry.mp4": {
          "keyAttributes": ["violence", "survival", "action", "desperation"],
          "dominantEmotion": "Anger",
          "secondaryEmotion": "Fear",
          "suitableArchetypes": ["Warrior", "Bandit", "Survivor", "Cornered_Animal"]
        },
        "video1p5happy.mp4": {
          "keyAttributes": ["compassion", "helpfulness", "community"],
          "dominantEmotion": "Joy",
          "secondaryEmotion": "Calm",
          "suitableArchetypes": ["Healer", "Paladin", "Good_Samaritan"]
        }
      },
      "photos": {
        "demon_bg_01.jpg": {
          "keyAttributes": ["darkness", "power", "evil", "menace"],
          "dominantEmotion": "Anger",
          "secondaryEmotion": "Contempt",
          "suitableArchetypes": ["Tyrant", "Archvillain"]
        }
      }
    }
    ```

### 3. "Akıllı Kalp" Eşleştirme Algoritması (Uygulama Adımı)

Bu algoritma, oyuncu için üretilen "Karakter Profili" ile `media_database.json` içindeki her bir medya dosyasını karşılaştırarak bir "uyum puanı" hesaplar ve en uygun içeriklerden bir oynatma listesi oluşturur.

*   **Sorumlu Motor:** Bu mantık, `KarmaBasedContentEngine`'in yerini alacak olan yeni `IntelligentContentEngine.kt` içinde yer almalıdır.

*   **Eşleştirme Adımları:**
    1.  **Girdileri Al:** Oyuncunun güncel `Karakter Profili` nesnesini ve `media_database.json` içeriğini yükle.
    2.  **Puanlama Döngüsü:** Veritabanındaki her bir medya dosyası için bir `uyumPuanı` hesapla (başlangıç puanı 0):
        *   **Arketip Eşleşmesi:** Eğer oyuncunun `mainArchetype`'i, medyanın `suitableArchetypes` listesinde varsa, `uyumPuanı += 50`.
        *   **Nitelik Eşleşmesi:** Oyuncunun `keyAttributes` listesindeki her bir nitelik, medyanın `keyAttributes` listesinde de varsa, her eşleşme için `uyumPuanı += 10`.
        *   **Duygu Eşleşmesi:** Eğer oyuncunun `dominantEmotion`'ı, medyanın `dominantEmotion`'ı ile aynıysa, `uyumPuanı += 25`.
        *   **İkincil Duygu Eşleşmesi:** Eğer oyuncunun `dominantEmotion`'ı, medyanın `secondaryEmotion`'ı ile aynıysa, `uyumPuanı += 15`.
        *   **İç Çatışma Bonusu:** Eğer oyuncunun `dominantEmotion`'ı medyanın `secondaryEmotion`'ına VE oyuncunun `secondaryEmotion`'ı medyanın `dominantEmotion`'ına eşleşiyorsa (çapraz eşleşme), bu durum oyuncunun içsel çatışmasını yansıttığı için ekstra `uyumPuanı += 20`.
    3.  **Filtreleme ve Sıralama:** `uyumPuanı` 0 olan tüm medyaları ele. Kalanları, puanlarına göre büyükten küçüğe doğru sırala.
    4.  **Oynatma Listesi Oluşturma:**
        *   En yüksek puanlı ilk 5-10 medyayı seç.
        *   Listeye bir miktar rastgelelik katmak için, en yüksek puanlı ilk 3 medyayı kendi içinde karıştır (`shuffle`). Bu, oyuncunun her seferinde birebir aynı videoyu görmesini engeller.
        *   Son listeyi oyuncunun dinamik oynatma listesi olarak kaydet.

### 4. Sistem Entegrasyonu ve Akış Şeması

Yeni "Akıllı Kalp" sisteminin tam iş akışı aşağıdaki gibi olacaktır:

1.  **Olay: Oyun Günü Biter.**
    *   `GameStateManager`, günün bittiğini algılar.
    *   `GlobalAIManager.synthesizeEndOfDayMemory()` çağrılır.
    *   **Yerel Gemma Modeli**, o günün diyaloglarını (`flashMemory`) özetler ve sonucu `long_term_memories` veritabanına kaydeder.

2.  **Olay: Oyuncu Oyunu Kaydeder ve Çıkar.**
    *   Kaydetme işlemi sırasında yeni bir servis olan `CharacterAnalysisService` tetiklenir.
    *   Bu servis, veritabanından son 3 günün özet metinlerini çeker.
    *   **Online Gemini API**'ını kullanarak bu özetlerden bir `Karakter Profili` JSON'u üretir.
    *   Bu `Karakter Profili` JSON'u, oyuncunun ana kayıt dosyasına (`PlayerState` veya benzeri bir yere) kaydedilir.

3.  **Olay: Oyuncu Oyuna Geri Döner.**
    *   `UserEntryViewModel` (veya ilgili başlangıç yöneticisi) tetiklenir.
    *   Yeni `IntelligentContentEngine` çağrılır.
    *   Motor, oyuncunun kayıt dosyasından `Karakter Profili`'ni ve `assets` klasöründen `media_database.json`'ı okur.
    *   Yukarıda tanımlanan **"Akıllı Kalp Eşleştirme Algoritması"** çalıştırılır.
    *   Oyuncunun profiline en uygun medyalar seçilerek kişiselleştirilmiş bir oynatma listesi oluşturulur.

4.  **Sonuç:** `UserEntryScreen`, oyuncunun son karakter durumunu birebir yansıtan, tamamen kişiselleştirilmiş bir video ve fotoğraf döngüsü ile açılır.

---

## Bölüm 7: Zincirleme Etki - Rozet, Karma ve Şans Mekanikleri

### 1. "Rozet" Sisteminin Mekanik Tetiklemeleri

Kod tabanı analizi sonucunda şu bulgulara ulaşıldı:

**motivationalBadges:** Arama sonucu bu isimle doğrudan psikolojik rozet sistemi bulunamadı. Ancak, mevcut `Badge` sistemi detaylıca incelendiğinde, rozetlerin sadece görsel etiket olmadığını, mekanik etkileri olan bir sistem olduğunu gördük.

**BadgeManager:** Kodda `BadgeManager` adında ayrı bir olay yöneticisi sınıfı bulunmamaktadır. Bunun yerine, `GameStateManager` içinde rozet işlemleri şu şekilde yapılmaktadır:

- `grantBadge(badgeId: String)` - Rozet verme
- `revokeBadge(badgeId: String)` - Rozet kaldırma
- `equipTitle(titleId: String)` - Unvan takma
- `getEquippedTitle()` - Aktif unvanı alma

**Olay Tetikleme:** Rozet kazanımı doğrudan başka bir sistemi tetiklemez. Ancak, rozetlerin `statBonuses` (stat bonusları) ve `narrativeEffect` (anlatısal etkisi) özellikleri vardır. Bu bonuslar doğrudan oyuncunun `PlayerState`'ine entegre edilir ve tüm mekaniklerde etkisini gösterir.

### 2. "Karma" Sisteminin Varlığı ve İşleyişi

**moralityScore vs karmaPoints:** Kodda `moralityScore` adında -1.0 ile +1.0 arası ahlak puanı bulunmaktadır. Ancak `karmaPoints` adında ayrı bir karma sistemi bulunmamaktadır. Bu, mevcut "Morality" sisteminin temelinde yattığını göstermektedir.

**Stat Etkileşimi:** `moralityScore` doğrudan stat sistemine entegre değildir. Ancak, `KarmaBasedContentEngine` aracılığıyla içerik üretimini etkilemektedir. Bu sistem, ahlak puanına göre video ve görsel içeriklerin seçimini yapar.

**Rozet Etkileşimi:** Rozetler doğrudan `moralityScore`'a etki etmez. Ancak, `SystemicEngine` ve `UmbrosEngine` gibi sistemler, oyuncunun sahip olduğu rozet sayısına göre oyun mekaniklerini etkileyebilir. Örneğin, Umbros Engine rozet sayısına göre karakterin profilini analiz eder.

### 3. "Şans" Sisteminin Varlığı ve İşleyişi

**Luck Stat:** Kodda `PlayerState` içinde `luck` adında bir stat bulunmaktadır. Bu stat, `StatType.LUCK` olarak tanımlanmıştır ve varsayılan değeri 10'dur.

**Varsayılan Değer:** `PlayerState.kt` dosyasında `val luck: Int = 10` olarak tanımlanmıştır. Bu, oyuncunun başlangıçta ortalama bir şans seviyesine sahip olduğunu gösterir.

**Değişim Mekanizması:** Şans statı şu yollarla değişebilir:
- Stat_allocation sistemi ile oyuncu doğrudan puan atayabilir: `allocateStatPoint("luck", amount)`
- GameMasterEngine ile hikaye bazlı bonus stat verilebilir: `grantBonusStats(luck = X)`
- Rozetlerin stat bonusları ile dolaylı olarak etkilenebilir

**Mekanik Etkileri:** Şans statı doğrudan şu mekanikleri etkiler:

**DiceEngine'de Etki:** `DiceEngine.kt` dosyasında `calculateStatModifier` fonksiyonu şu şekilde çalışır:
```kotlin
private fun calculateStatModifier(context: DiceContext): Int {
    val luckMod = (context.luck / 20).toInt() // Every 20 points of luck = +1
    // diğer modlar...
    return luckMod + intMod + charismaMod + perceptionMod
}
```
Bu, her 20 şans puanı için +1 bonus anlamına gelir.

**DiceSystem'de Etki:** `DiceSystem.kt` içinde `luckCheck` fonksiyonu şu şekilde çalışır:
```kotlin
fun luckCheck(luckStat: Int): DiceRoll {
    val difficulty = when {
        luckStat >= 15 -> Difficulty.EASY
        luckStat >= 10 -> Difficulty.MEDIUM
        else -> Difficulty.HARD
    }
    return skillCheck(luckStat, difficulty)
}
```

### 4. Zincirleme Etkinin Tam Akış Şeması

Mevcut sistemde doğrudan "Rozet -> Karma -> Şans" zinciri bulunmamaktadır. Ancak, dolaylı ve zayıf bağlantılar şu şekildedir:

**Varsayılan Akış (Zayıf Bağlantılar):**

1. **Rozet Kazanımı:** Oyuncu bir rozet kazanır (`grantBadge` çağrısı)
   - Örnek: "Dark_Triad_Machiavellianism" rozeti

2. **Rozet Etkisi:** Rozetin `statBonuses` etkileri devreye girer
   - Örnek: `mapOf(StatType.LUCK_PERCENT to 0.10f)` gibi bir bonus

3. **Stat Değişimi:** Rozet bonusu player statlarını değiştirir
   - `PlayerState.luck` doğrudan değil, ama `luckPercent` gibi bonuslar şansı artırabilir

4. **Şans Etkisi:** Artan şans değeri, dice rolları etkiler
   - Daha fazla kritik başarı şansı
   - Daha iyi eşya düşme oranları
   - Daha iyi rastgele olay sonuçları

**Geliştirilebilir Akış (Tasarım Önerisi):**

Daha güçlü bir zincirleme etki için şu mekanikler eklenebilir:

1. **Rozet Bazlı Moral Etkisi:** Belirli rozetler ahlak puanı kazanma oranını etkileyebilir
   - Örnek: "Chaos_Simulator" rozeti sahibi oyuncunun "anarchic" eylemleri +0.10 bonus verir

2. **Ahlak Bazlı Şans Etkisi:** moralityScore belirli aralıklarda şans bonusları verebilir
   - Örnek: moralityScore > 0.5 iken +2 şans bonusu
   - Örnek: moralityScore < -0.5 iken -2 şans cezası

3. **Dinamik Bonus Sistemleri:** Rozet + ahlak kombinasyonları özel bonuslar üretebilir
   - Örnek: "Dark_Triad" rozeti + negative morality = +%15 luck bonus

**Gerçek Zamanlı Akış Örneği:**

Mevcut sistemde çalışan örnek senaryo:
1. Oyuncu "Swift_Blade" rozetini kazanır (statBonus: AGI_PERCENT +0.15f, STR_PERCENT +0.08f)
2. Rozetin bonusları doğrudan PlayerState'e uygulanır
3. Artan AGI ve STR, combat ve exploration dice rollarını etkiler
4. Şans statı doğrudan etkilenmez, ancak toplam performans artar

**Sonuç:**
Mevcut sistemde "Rozet -> Şans" bağlantısı vardır ama bu dolaylıdır ve doğrudan "Rozet -> Karma -> Şans" zinciri mevcut değildir. Rozetler doğrudan ahlak puanını etkilemez, ahlak puanı doğrudan şans statını değiştirmez. Ancak, rozetlerin stat bonusları ve ahlak puanının içerik kişiselleştirme üzerindeki etkileri, oyunun genel mekaniği üzerinde dolaylı etkiler yaratır.

---

## 4.B Dice Sistemi ve Üçlü Sistem Etkileşimi

### Dice Sistemine Genel Bakış

Oyun içinde rastgelelik ve_skill_checks_ için **DiceEngine** ve **DiceSystem** olmak üzere iki ana sistem kullanılmaktadır:

- **DiceSystem**: Temel zar atışı ve beceri kontrolleri için kullanılır (örneğin: `skillCheck`, `luckCheck`, `combatRoll`)
- **DiceEngine**: Daha karmaşık, bağlama duyarlı zar atışları için kullanılır (`calculateComplexRoll`)

### Luck Stat'ının Dice Sistemlerindeki Rolü

Luck stat'ı doğrudan iki dice sisteminde etkilidir:

**DiceEngine'de (Karmaşık Sistem):**
```kotlin
private fun calculateStatModifier(context: DiceContext): Int {
    val luckMod = (context.luck / 20).toInt() // Every 20 points of luck = +1
    val intMod = when (context.actionType) { ... }
    // ...
    return luckMod + intMod + charismaMod + perceptionMod
}
```
Her 20 luck puanı = +1 bonus

**DiceSystem'de (Basit Sistem):**
```kotlin
fun luckCheck(luckStat: Int): DiceRoll {
    val difficulty = when {
        luckStat >= 15 -> Difficulty.EASY
        luckStat >= 10 -> Difficulty.MEDIUM
        else -> Difficulty.HARD
    }
    return skillCheck(luckStat, difficulty)
}
```
Luck puanına göre zorluk seviyeleri değişir.

### Badge Sistemi ile Dice Etkileşimi

Rozetler doğrudan dice sistemlerine çağrı yapmaz, ancak dolaylı etkileri vardır:

1. **Stat Bonusları Aracılığıyla:**
   - Rozetler `statBonuses` ile `LUCK_PERCENT`, `LUCK_FLAT`, `AGI_PERCENT`, `INT_PERCENT` gibi bonuslar sağlar
   - Bu bonuslar `DiceContext` yaratılırken `PlayerState`'ten alınır
   - Sonuç olarak dice rolları bu bonuslardan etkilenir

2. **Örnek:**
   - "Swift_Blade" rozeti: `AGI_PERCENT to 0.15f` bonusu sağlar
   - Bu bonus `DiceContext` oluşturulurken hesaplara eklenir
   - `EXPLORATION_STEALTH` gibi aksiyonlarda AGI bonusu dice rolları etkiler

### Karma (Morality) Sistemi ile Dice Etkileşimi

Ahlak sistemi doğrudan dice sistemleriyle etkileşime girmez, ancak dolaylı etkileri vardır:

1. **İçerik Kişiselleştirme:**
   - moralityScore değeri `KarmaBasedContentEngine` ile video/görsel içerik seçimi yapar
   - Bu içerikler oyunun atmosferini ve oyuncunun hislerini etkiler
   - Oyuncunun ruh hali dice rolları üzerinde psikolojik etki yaratabilir

2. **GM Sistemi Aracılığıyla:**
   - GameMasterEngine moralityScore'a göre NPC davranışlarını ve hikaye akışını değiştirir
   - Bu değişiklikler sonucunda ortaya çıkan farklı senaryolarda dice rolleri farklı önemler taşır

3. **Gelecekteki Geliştirmeler:**
   - moralityScore'a göre_dice_bias_ uygulanabilir (örneğin: ahlaklı karakterler için pozitif kritik şansı artırılabilir)
   - Etik olmayan aksiyonlar için ceza dice rolleri eklenebilir

### Üçlü Sistemin Ortak Etkileşimi

**Gerçek Zamanlı Senaryo:**

1. **Rozet Kazanımı:** Oyuncu "Archon_of_Fortune" rozeti alır (bonus: `LUCK_PERCENT to 0.20f`)

2. **Stat Güncellemesi:** Rozet bonusu `PlayerState`'e uygulanır, toplam luck artar

3. **Dice Roll Etkisi:** Artan luck değeri `DiceContext` yaratılırken kullanılır:
   - 20+ daha fazla luck = +%1 bonus (DiceEngine'de)
   - Daha yüksek luckCheck başarı oranı (DiceSystem'de)

4. **Karma Etki:** moralityScore etkisi:
   - Pozitif ahlak: daha güvenli, dikkatli oyun tarzı
   - Negatif ahlak: daha riskli, agresif oyun tarzı
   - Bu tarz farkı farklı dice rollerine ve sonuçlara neden olur

5. **Toplam Etki:** Rozet → Stat Bonusu → Dice Bonusu → Oyun Sonuçları
   - moralityScore → Hikaye Akışı → Dice Fırsatları → Genel Strateji

### Sistemlerin Güçlü ve Zayıf Noktaları

**Güçlü Yönler:**
- Her sistem kendi başına iyi tasarlanmış ve dengeli
- Rozetlerin stat bonus sistemleri mekanik etkiler sağlıyor
- Luck stat'ı hem basit hem karmaşık dice sistemlerine entegre
- moralityScore içerik kişiselleştirmesi sağlıyor

**Zayıf Yönler:**
- Sistemler arasında doğrudan, güçlü etkileşimler yok
- Karma ile dice arasında doğrudan mekanik bağlantı yok
- Rozetler dice rollerini sadece dolaylı olarak etkiliyor (stat yoluyla)

**Gelecekteki Entegrasyon Fırsatları:**
- moralityScore'la dice rolleri arasında doğrudan bonus/ceza mekanizması
- Rozet bazlı dice modifikatörleri (örneğin: "Chaos_Simulator" rozeti kritik şansı artırabilir)
- Karma durumuna göre dice rollerinin "theme" etkileri (olumlu aksiyonlarda pozitif eğilim)

---

## Bölüm 8: Karma-NPC Etkileşim Haritası

### 1. "Karakter Kataloğu" Sisteminin Tespiti

Kod tabanında, NPC'lerin oyuncu hakkındaki verilerini yöneten merkezi bir sistem bulunmaktadır:

**Ana Sınıflar ve Konumlar:**
- **`NPCRelationship.kt`**: `NPCRelationship` veri sınıfı ve `RelationshipLevel` enum'u
- **`GameState.kt`**: `val npcRelationships: Map<String, NPCRelationship> = emptyMap()` 
- **`GameStateManager.kt`**: `updateNpcRelationship(npcId: String, changeAmount: Int)` fonksiyonu
- **`DynamicNPCState.kt`**: `val loyaltyToPlayer: Int = 0` (-100'den +100'e) ve `currentMood`
- **`WorldUpdateEngine.kt`**: NPC evolüsyon ve loyalty bazlı mood değişimi

**Sistem Adı:** "Dinamik NPC İlişki Sistemi" - NPC'lerin oyuncuya olan sadakat ve duygusal durumunu izler.

### 2. Karma Puanının Kataloğa Etkisi

moralityScore doğrudan `GameStateManager`'da değil, ancak **AI destekli GameMasterEngine** üzerinden NPC ilişkilerini etkilemektedir:

**Doğrudan Bağlantı:**
- `MoralityEngine` sadece `moralityScore`'u günceller
- `GameMasterEngine` ise oyuncunun yazdığı metni analiz ederken moralityScore'u bağlam olarak alır
- AI, metin içeriğine göre `GMResponse` içinde `npcStateChange` komutları üretebilir
- Bu komutlar `loyaltyChange` içerir ve doğrudan NPC sadakatini değiştirir

**İşlevsel Bağlantı:**
- Oyuncu journal'da "yardım ettim" tarzı eylemler yazarsa:
  1. `MoralityEngine` moralityScore'u artırır (+0.05)
  2. `GameMasterEngine` aynı metni analiz eder ve AI, etkileşimli NPC'nin sadakatini artırmayı kararlaştırabilir
  3. `GMResponse` içinde `loyaltyChange: 5` gibi bir komut oluşturulur
  4. `executeGMActions` bu komutu işler ve NPC'nin `loyaltyToPlayer` değeri artar

**Formül/Reçete:**
- Sabit bir formül yoktur, AI'nın karar vermesine dayalıdır
- Ancak `WorldUpdateEngine` içinde sistematik bağlantılar var:
  - `loyaltyToPlayer <= -60` ve `level >= 3` ve `timesDefeated >= 1` → NPC Nemesis'e dönüşür
  - `loyaltyToPlayer` değerine göre `NPCMood.fromLoyalty()` ile mood otomatik güncellenir

### 3. Kataloğun Oyun Dünyasına Etkisi

NPC'lerin oyuncuya olan ilişkileri doğrudan oyun mekaniklerine etki etmektedir:

**Diyalog Farklılığı:**
- `GameMasterEngine` NPC context'i oluştururken `loyaltyToPlayer` değerini prompt'a dahil eder
- AI, bu sadakat seviyesine göre NPC'nin tutumunu ve davranış tarzını belirler
- Örnek: `loyaltyToPlayer > 60` → dostane, `loyaltyToPlayer < -60` → düşmanca diyalog

**Eylem Farklılığı:**
- Düşük loyalty (-60 ve altı) NPC'ler zamanla Nemesis'e dönüşebilir
- Loyalty seviyesine göre NPC'lerin yardım etme, saldırmama veya özel eylemler yapma olasılığı değişir
- `WorldUpdateEngine` içinde loyalty bazlı mood güncellemesi: `updateMoodBasedOnLoyalty()`

**Statik Etkileşimler:**
- `DiceEngine` içinde `val npcRelationship: Float` parametresi, -1.0 (düşman) ile +1.0 (en iyi arkadaş) arası değer alır
- Bu değer doğrudan dice rollerini etkiler:
  - `npcRelationship > 0.7f` → +3 bonus
  - `npcRelationship > 0.3f` → +1 bonus  
  - `npcRelationship < -0.7f` → -3 ceza
  - `npcRelationship < -0.3f` → -1 ceza

### 4. Tam Etkileşim Akış Şeması

Karma-NPC etkileşiminin tam işleyişi şu adımları içerir:

1. **Oyuncu Eylemi**: Oyuncu `JournalScreen`'de günlüğüne "Yerleşimdeki köylülere yemek dağıttım" yazar

2. **Çift Sistem Aktivasyonu**: `JournalViewModel` içinde:
   - **Basit Sistem**: `MoralityEngine.analyzeAndGetScore()` → "yemek" kelimesi iyilik kategorisinde değil, ama "yardım" gibi kelimeler +0.05 puan verir
   - **AI Sistem**: `GameMasterEngine.generateStoryWithContext()` → metni analiz eder, bağlamı değerlendirir, NPC'leri içerip içermediğini kontrol eder

3. **AI Kararı**: Gemini API, metni işlerken moralityScore, mevcut NPC durumu, lokasyon vs. gibi tüm bağlamı alır ve şu kararı verebilir:
   ```json
   {
     "journalEntry": "Köylülere yemek dağıttığında minnettar oldular...",
     "npcStateChange": {
       "npcId": "village_elder",
       "loyaltyChange": 10,
       "moodOverride": "GRATEFUL"
     }
   }
   ```

4. **Sistemsel Uygulama**: `GameStateManager.executeGMActions()` fonksiyonu:
   - `npcStateChange` komutunu alır
   - `DynamicNPCDao` üzerinden NPC'nin `loyaltyToPlayer` değerini 10 artırır (örneğin 20 → 30)
   - NPC'nin mood'unu `GRATEFUL` olarak günceller
   - Bu değişiklik veritabanına kaydedilir

5. **Dinamik Tepki**: Sonraki etkileşimde:
   - `GameMasterEngine.detectAndLoadNPCContext()` NPC'nin yeni durumunu okur (`loyaltyToPlayer: 30`)
   - Bu bilgi AI prompt'a dahil edilir: "[NPC DURUMU: village_elder] Sadakat Skoru: 30/100 (Nötr → Arkadaşça)"
   - AI, NPC'nin artık daha yardımsever ve dostane olduğunu yansıtan diyalog üretir

6. **Sürekli Evrim**: `WorldUpdateEngine.onDayPassed()` fonksiyonu:
   - Günlük olarak tüm NPC'lerin evrimini kontrol eder
   - `updateMoodBasedOnLoyalty()` ile loyalty değerine göre otomatik mood güncellemesi yapar
   - `checkNemesisPromotion()` ile çok düşük loyalty (-60 ve altı) NPC'lerin Nemesis'e dönüşüp dönüşmediğini kontrol eder

**Sonuç:** moralityScore doğrudan NPC ilişkilerini etkilemez, ancak journal entries üzerinden çalışan AI sistemi (GameMasterEngine) aracılığıyla dolaylı ama güçlü bir etki yaratır. Oyuncunun yazdığı eylemler hem ahlaki skorunu hem de NPC ile ilişkisini aynı anda etkileyebilen çok katmanlı bir sistem oluşur.