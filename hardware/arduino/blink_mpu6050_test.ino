/*
 * ADIM 3: "MPU650 Sağlam mı?" Testi
 * Hedef: MPU650 (A4/A5) sensöründen veri okumak.
 * Kütüphane: "MPU6050 by Electronic Cats"
 */
 
#include <MPU6050.h> // "MPU6050 by Electronic Cats" kütüphanesi
#include <Wire.h>

MPU6050 mpu; // Sensör için bir nesne oluştur

void setup() {
  // Serial Monitor'ü başlat (ESP32 ile aynı hızda)
  Serial.begin(115200); 
  
  Wire.begin(); // I2C (A4/A5 pinleri) başlat
  
  mpu.initialize(); // MPU650'yi başlat
  
  Serial.println("====================================");
  Serial.println("ADIM 3: MPU650 Test İstasyonu Başlatıldı");
  Serial.println("MPU650 bağlantısı test ediliyor...");
  
  if (mpu.testConnection()) {
    Serial.println(">>> SONUÇ: BAŞARILI!");
    Serial.println(">>> MPU650 (Kolluk Gözü) SAĞLAM.");
    Serial.println("====================================");
  } else {
    Serial.println(">>> SONUÇ: BAŞARISIZ!");
    Serial.println(">>> MPU650 bulunamadı. Bağlantıyı (A4/A5) veya kabloları kontrol et.");
    Serial.println("====================================");
  }
}

void loop() {
  // Eğer bağlantı başarılıysa, verileri oku
  if (mpu.testConnection()) {
    int16_t ax, ay, az;
    int16_t gx, gy, gz;

    // Ham ivme ve gyro verilerini oku
    mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

    // Verileri Serial Monitor'e yazdır
    Serial.print("a/g:\t");
    Serial.print(ax); Serial.print("\t");
    Serial.print(ay); Serial.print("\t");
    Serial.print(az); Serial.print("\t");
    Serial.print(gx); Serial.print("\t");
    Serial.print(gy); Serial.print("\t");
    Serial.println(gz);
    
    delay(100); // 0.1 saniye bekle
  }
}