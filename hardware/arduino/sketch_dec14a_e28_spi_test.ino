#include <SPI.h>

// --- PIN TANIMLAMALARI (Son konuştuğumuz düzen) ---
#define NSS_PIN   5
#define RST_PIN   14
#define BUSY_PIN  27
#define SCK_PIN   18
#define MISO_PIN  19
#define MOSI_PIN  23

// Eğer bağladıysan TXEN ve RXEN (Bağlamadıysan boşver, kod hata vermez)
#define TXEN_PIN  13 
#define RXEN_PIN  12

// --- TEST AYARLARI ---
// ExpressLRS genelde 10MHz üzeri kullanır. Biz kademe kademe artıracağız.
// Önce 4MHz, sonra 10MHz deneyeceğiz.
uint32_t spi_speeds[] = {1000000, 4000000, 10000000}; // 1MHz, 4MHz, 10MHz

SPIClass *vspi = NULL;

void setup() {
  Serial.begin(115200);
  while(!Serial);
  delay(1000);

  Serial.println("\n\n=== E28 (SX1280) HIZ VE STRES TESTI BAŞLIYOR ===");

  // Pin Ayarları
  pinMode(NSS_PIN, OUTPUT);
  pinMode(RST_PIN, OUTPUT);
  pinMode(BUSY_PIN, INPUT); 
  // (BUSY pini için PULLUP gerekmez, modül kendi sürer ama emin olmak için INPUT dedik)

  // PA/LNA Pinleri (Eğer varsa kontrol edelim)
  pinMode(TXEN_PIN, OUTPUT);
  pinMode(RXEN_PIN, OUTPUT);
  digitalWrite(TXEN_PIN, LOW);
  digitalWrite(RXEN_PIN, LOW);

  digitalWrite(NSS_PIN, HIGH);

  // SPI Başlat
  vspi = new SPIClass(VSPI);
  vspi->begin(SCK_PIN, MISO_PIN, MOSI_PIN, NSS_PIN);

  // MODÜLÜ RESETLE (Canlandırma)
  Serial.print(">> Modul Resetleniyor... ");
  digitalWrite(RST_PIN, LOW);
  delay(50);
  digitalWrite(RST_PIN, HIGH);
  delay(100); 

  // BUSY kontrolü (Reset sonrası BUSY Low'a düşmeli)
  if(digitalRead(BUSY_PIN) == HIGH) {
    Serial.println("UYARI: Reset sonrasi BUSY hala HIGH! (Modul kilitli veya BUSY pini bozuk)");
  } else {
    Serial.println("OK. (Busy Low)");
  }
}

// SX1280 Status Okuma Fonksiyonu
uint8_t readStatus(uint32_t speed) {
  vspi->beginTransaction(SPISettings(speed, MSBFIRST, SPI_MODE0));
  digitalWrite(NSS_PIN, LOW);
  
  // GetStatus komutu: 0xC0
  uint8_t status = vspi->transfer(0xC0); 
  
  digitalWrite(NSS_PIN, HIGH);
  vspi->endTransaction();
  return status;
}

void loop() {
  Serial.println("\n------------------------------------------------");
  
  // 3 Farklı Hızda Test Et
  for(int i=0; i<3; i++) {
    uint32_t currentSpeed = spi_speeds[i];
    Serial.print("TEST HIZI: "); 
    Serial.print(currentSpeed / 1000000); 
    Serial.println(" MHz");

    // BUSY Kontrolü: İşlem yapmadan önce modül meşgul mü?
    if(digitalRead(BUSY_PIN) == HIGH) {
      Serial.println("  ⚠️ HATA: Modul surekli MESGUL (BUSY High).");
    }

    // Veri Okuma Denemesi (10 kez üst üste)
    int successCount = 0;
    uint8_t lastVal = 0;
    
    for(int k=0; k<10; k++) {
      uint8_t val = readStatus(currentSpeed);
      // Geçerli değerler genelde 0 ve 255 değildir.
      // SX1280 Status byte'ında bitler değişken olabilir ama 0x00 ve 0xFF genelde hatadır.
      if(val != 0x00 && val != 0xFF) {
        successCount++;
        lastVal = val;
      }
      delay(2);
    }

    Serial.print("  Sonuç: 10 denemede ");
    Serial.print(successCount);
    Serial.println(" basarili okuma.");
    Serial.print("  Okunan Ornek Veri (HEX): 0x");
    Serial.println(lastVal, HEX);

    if(successCount < 8) {
       Serial.println("  ❌ KRİTİK: Bu hizda veri kaybi var! Kablolar uzun veya parazitli.");
    } else {
       Serial.println("  ✅ OK: Bu hız stabil.");
    }
  }

  Serial.println(">> Test döngüsü bitti. 3 sn bekle...");
  delay(3000);
}