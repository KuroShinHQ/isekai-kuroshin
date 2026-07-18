#define SWITCH_PIN 2

void setup() {
  // INPUT_PULLUP sayesinde anahtar basılı değilken pin "1" (HIGH) okur.
  // Anahtarı kapattığında (devre tamamlandığında) pin "0" (LOW) okur.
  pinMode(SWITCH_PIN, INPUT_PULLUP); 
  
  Serial.begin(9600);
  Serial.println("--- ANAHTAR TEST OPERASYONU BASLADI ---");
}

void loop() {
  int durum = digitalRead(SWITCH_PIN);

  if (durum == LOW) {
    Serial.println("ANAHTAR KAPALI (AKIM GECIYOR) - [ON]");
  } else {
    Serial.println("ANAHTAR ACIK (DEVRE KESIK) - [OFF]");
  }

  delay(200); // Ekranın çok hızlı akmaması için
}