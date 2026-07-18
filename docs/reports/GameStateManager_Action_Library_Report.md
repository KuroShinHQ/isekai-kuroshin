# GameStateManager - Eylem Kütüphanesi Raporu

Bu rapor, `ActionExecutorEngine` tarafından kullanılabilecek, `GameStateManager` içerisindeki tüm durum değiştirici fonksiyonları listeler.

## 1. Oyuncu Durum Eylemleri (Player State Actions)

---
### Fonksiyon Adı ve İmzası: `modifyHealth(amount: Int)`
* **Açıklama:** Oyuncunun mevcut canını belirtilen miktar kadar artırır veya azaltır. Canın, maksimum canı aşmasını veya sıfırın altına düşmesini engeller. Can sıfıra düştüğünde ölüm kontrolü tetiklenir.
* **Parametreler:**
    * `amount: Int` - Can miktarındaki değişiklik (+ veya -).
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_HEALTH`
---

---
### Fonksiyon Adı ve İmzası: `modifyMana(amount: Int)`
* **Açıklama:** Oyuncunun mevcut manasını belirtilen miktar kadar artırır veya azaltır. Mananın, maksimum manayı aşmasını veya sıfırın altına düşmesini engeller.
* **Parametreler:**
    * `amount: Int` - Mana miktarındaki değişiklik (+ veya -).
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_MANA`
---

---
### Fonksiyon Adı ve İmzası: `updateGold(amount: Int)`
* **Açıklama:** Oyuncunun altın miktarını belirtilen değer kadar artırır veya azaltır. Pozitif değerler altın kazançını, negatif değerler altın kaybını temsil eder.
* **Parametreler:**
    * `amount: Int` - Altın miktarındaki değişiklik (+ veya -).
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_GOLD`
---

---
### Fonksiyon Adı ve İmzası: `updateMoralityScore(scoreChange: Float)`
* **Açıklama:** Oyuncunun ahlaki puanını belirtilen değer kadar değiştirir. Ahlaki puan -1.0f ile 1.0f arasında sınırlandırılır.
* **Parametreler:**
    * `scoreChange: Float` - Ahlaki puandaki değişiklik (-1.0f ile 1.0f arası).
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_MORALITY`
---

---
### Fonksiyon Adı ve İmzası: `updatePlayerState(newState: PlayerState)`
* **Açıklama:** Oyuncunun tüm durumunu yeni bir PlayerState ile değiştirir. Ölüm kontrolü otomatik olarak yapılır.
* **Parametreler:**
    * `newState: PlayerState` - Yeni oyuncu durumu.
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_PLAYER_STATE`
---

---
### Fonksiyon Adı ve İmzası: `updatePlayerResources(newResources: ResourcesZ4)`
* **Açıklama:** Oyuncunun kaynaklarını (su, yemek vb.) yeni değerlerle günceller.
* **Parametreler:**
    * `newResources: ResourcesZ4` - Yeni kaynak durumu.
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_RESOURCES`
---

---
### Fonksiyon Adı ve İmzası: `updatePlayerProfile(newProfile: PlayerProfile)`
* **Açıklama:** Oyuncunun profilini (arketip puanları vb.) günceller ve sınıf görevlerini kontrol eder.
* **Parametreler:**
    * `newProfile: PlayerProfile` - Yeni oyuncu profili.
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_PROFILE`
---

## 2. İlerleme ve Gelişim Eylemleri (Progression & Development Actions)

---
### Fonksiyon Adı ve İmzası: `addExperience(amount: Int)`
* **Açıklama:** Oyuncuya belirtilen miktar kadar tecrübe puanı ekler ve gerekirse `levelUp()` fonksiyonunu tetikler.
* **Parametreler:**
    * `amount: Int` - Eklenecek tecrübe puanı.
* **AI Eylem Tipi Önerisi (`actionType`):** `ADD_EXPERIENCE`
---

---
### Fonksiyon Adı ve İmzası: `applyStatAllocations(allocations: Map<StatType, Int>)`
* **Açıklama:** Oyuncunun sahip olduğu stat puanlarını belirtilen dağılıma göre oyuncunun temel istatistiklerine dağıtır.
* **Parametreler:**
    * `allocations: Map<StatType, Int>` - İstatistik türü ve eklenecek puanların haritası.
* **AI Eylem Tipi Önerisi (`actionType`):** `ALLOCATE_STATS`
---

---
### Fonksiyon Adı ve İmzası: `trainStat(statType: StatType)`
* **Açıklama:** Belirtilen istatistiği 1 puan artırmak için altın harcar, zaman ilerletir ve tehdit sayacını artırır.
* **Parametreler:**
    * `statType: StatType` - Antrenman yapılacak istatistik türü.
* **AI Eylem Tipi Önerisi (`actionType`):** `TRAIN_STAT`
---

---
### Fonksiyon Adı ve İmzası: `decrementStatPoints(pointsToSpend: Int)`
* **Açıklama:** Oyuncunun sahip olduğu stat puanlarını belirtilen miktar kadar azaltır.
* **Parametreler:**
    * `pointsToSpend: Int` - Harcanacak stat puanı miktarı.
* **AI Eylem Tipi Önerisi (`actionType`):** `SPEND_STAT_POINTS`
---

## 3. Envanter ve Ekipman Eylemleri (Inventory & Equipment Actions)

**Not:** GameStateZ7 data class'ında envanter ve ekipman alanları mevcut (`equippedItems`, `inventory`, `collectedItems`) ancak henüz bu alanları değiştiren public fonksiyonlar implement edilmemiştir. Gelecekteki implementasyon için potansiyel actionType'lar:

* `ADD_ITEM_TO_INVENTORY` - Envantere eşya ekleme
* `REMOVE_ITEM_FROM_INVENTORY` - Envanterden eşya çıkarma
* `EQUIP_ITEM` - Eşya kuşanma
* `UNEQUIP_ITEM` - Eşya çıkarma
* `UPDATE_ITEM_DURABILITY` - Eşya dayanıklılığını güncelleme

## 4. Görev ve Hikaye Eylemleri (Quest & Story Actions)

---
### Fonksiyon Adı ve İmzası: `addStoryPage(content: String)`
* **Açıklama:** Hikaye sayfalarına yeni bir sayfa ekler ve mevcut sayfa numarasını günceller.
* **Parametreler:**
    * `content: String` - Eklenecek hikaye içeriği.
* **AI Eylem Tipi Önerisi (`actionType`):** `ADD_STORY_PAGE`
---

---
### Fonksiyon Adı ve İmzası: `updateLastStoryPage(newContent: String)`
* **Açıklama:** Son hikaye sayfasının içeriğini yeni içerikle değiştirir.
* **Parametreler:**
    * `newContent: String` - Yeni hikaye içeriği.
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_LAST_STORY_PAGE`
---

**Not:** GameStateZ7'de quest alanları mevcut (`activeQuests`, `completedQuests`) ancak bu alanları değiştiren public fonksiyonlar henüz implement edilmemiştir. Gelecekteki implementasyon için potansiyel actionType'lar:

* `ADD_QUEST` - Yeni görev ekleme
* `COMPLETE_QUEST` - Görevi tamamlama
* `UPDATE_QUEST_PROGRESS` - Görev ilerlemesini güncelleme
* `FAIL_QUEST` - Görevi başarısız yapma

## 5. Dünya ve Etkileşim Eylemleri (World & Interaction Actions)

---
### Fonksiyon Adı ve İmzası: `advanceTime(): Boolean`
* **Açıklama:** Oyun zamanını bir dönem ilerletir (sabah→öğlen→akşam→gece→yeni gün). Yeni güne geçtiğinde true döner.
* **Parametreler:** Yok
* **Dönüş Değeri:** `Boolean` - Yeni güne geçip geçmediği
* **AI Eylem Tipi Önerisi (`actionType`):** `ADVANCE_TIME`
---

**Not:** GameStateZ7'de dünya etkileşim alanları mevcut (`npcRelationships`, `knownLocations`, `currentLocationId`, `currentDirection`, `currentWeather`, `currentSeason`) ancak bu alanları değiştiren public fonksiyonlar henüz implement edilmemiştir. Gelecekteki implementasyon için potansiyel actionType'lar:

* `UPDATE_NPC_RELATIONSHIP` - NPC ilişkisini güncelleme
* `DISCOVER_LOCATION` - Yeni lokasyon keşfetme
* `CHANGE_LOCATION` - Lokasyon değiştirme
* `UPDATE_WEATHER` - Hava durumunu değiştirme
* `CHANGE_SEASON` - Mevsim değiştirme

## 6. Savaş ve Düşman Eylemleri (Combat & Enemy Actions)

**Not:** GameStateZ7'de savaş alanları mevcut (`inCombat`, `currentEnemies`) ancak bu alanları değiştiren public fonksiyonlar henüz implement edilmemiştir. Gelecekteki implementasyon için potansiyel actionType'lar:

* `START_COMBAT` - Savaş başlatma
* `END_COMBAT` - Savaşı bitirme
* `ADD_ENEMY` - Düşman ekleme
* `REMOVE_ENEMY` - Düşman çıkarma
* `UPDATE_ENEMY_HEALTH` - Düşman canını güncelleme

## 7. Sistem ve Genel Eylemleri (System & General Actions)

---
### Fonksiyon Adı ve İmzası: `updateUmbrosContract(contract: UmbrosContract?)`
* **Açıklama:** Umbros sözleşmesini günceller veya kaldırır (null atama ile).
* **Parametreler:**
    * `contract: UmbrosContract?` - Yeni sözleşme veya null.
* **AI Eylem Tipi Önerisi (`actionType`):** `UPDATE_UMBROS_CONTRACT`
---

---
### Fonksiyon Adı ve İmzası: `incrementThreatCounter(amount: Int)`
* **Açıklama:** Tehdit sayacını belirtilen miktar kadar artırır.
* **Parametreler:**
    * `amount: Int` - Artırılacak tehdit miktarı.
* **AI Eylem Tipi Önerisi (`actionType`):** `INCREMENT_THREAT`
---

---
### Fonksiyon Adı ve İmzası: `resetThreatCounter()`
* **Açıklama:** Tehdit sayacını sıfırlar.
* **Parametreler:** Yok
* **AI Eylem Tipi Önerisi (`actionType`):** `RESET_THREAT`
---

---
### Fonksiyon Adı ve İmzası: `resetGame()`
* **Açıklama:** Tüm oyun durumunu sıfırlar ve yeni bir oyuna başlar.
* **Parametreler:** Yok
* **AI Eylem Tipi Önerisi (`actionType`):** `RESET_GAME`
---

## 8. Placeholder/Gelecek Eylemler (Future Actions)

---
### Fonksiyon Adı ve İmzası: `craftItem()`
* **Açıklama:** Placeholder fonksiyon. Eşya üretimi yapar, hikaye ekler, tehdit sayacını artırır ve zamanı ilerletir.
* **Parametreler:** Yok
* **AI Eylem Tipi Önerisi (`actionType`):** `CRAFT_ITEM`
---

---
### Fonksiyon Adı ve İmzası: `restInTavern()`
* **Açıklama:** Placeholder fonksiyon. Hantte dinlenir, hikaye ekler, tehdit sayacını artırır ve zamanı ilerletir.
* **Parametreler:** Yok
* **AI Eylem Tipi Önerisi (`actionType`):** `REST_IN_TAVERN`
---

## Özet

**Toplam Aktif Fonksiyon Sayısı:** 19
**Kategori Dağılımı:**
- Oyuncu Durum Eylemleri: 6 fonksiyon
- İlerleme ve Gelişim Eylemleri: 4 fonksiyon
- Envanter ve Ekipman Eylemleri: 0 fonksiyon (henüz implement edilmemiş)
- Görev ve Hikaye Eylemleri: 2 fonksiyon
- Dünya ve Etkileşim Eylemleri: 1 fonksiyon
- Savaş ve Düşman Eylemleri: 0 fonksiyon (henüz implement edilmemiş)
- Sistem ve Genel Eylemleri: 4 fonksiyon
- Placeholder/Gelecek Eylemler: 2 fonksiyon

**Önemli Notlar:**
1. Tüm fonksiyonlar `_gameState.update` kullanarak durum değişikliği yapar
2. Çoğu fonksiyon `saveGameStateToDatabase()` çağırarak değişiklikleri kalıcı hale getirir
3. Bazı önemli sistem alanları (envanter, görevler, savaş, NPC ilişkileri) henüz tam implement edilmemiştir
4. ActionExecutorEngine bu fonksiyonları actionType'a göre çağırarak AI komutlarını uygulayabilir