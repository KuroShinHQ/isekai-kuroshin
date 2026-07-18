# Hardware — Custom ESP32 Wrist-Controller ("Kolluk")

This directory contains the hardware design files for the custom ESP32-based wrist-controller
that pairs with the IsekaiKuroshin Android app over Bluetooth Low Energy (BLE).

## Overview

The "Kolluk" (Turkish for "arm-brace") is a wearable motion-controller built around an
**ESP32 DevKit V1** microcontroller. It reads hand/wrist orientation from an **MPU6050**
gyroscope/accelerometer, reads throttle from a potentiometer (ADC), and sends CRSF-formatted
commands to an **ELRS TX module** (E28 / SX1280 2.4 GHz) for drone control — all over BLE
from the Android app.

## Directory Structure

```
hardware/
├── kicad/              # KiCad PCB project (kuroshin.kicad_pcb/sch/pro)
├── easyeda/            # EasyEDA project (Kolluk Devresi.eprj)
├── arduino/            # Arduino test sketches
│   ├── blink_mpu6050_test.ino       — MPU6050 connectivity test
│   ├── sketch_dec14a_e28_spi_test.ino — E28/SX1280 SPI speed stress test
│   └── sketch_mar10b_switch_test.ino  — GPIO switch test
├── firmware/           # Main ESP32 firmware (production)
│   └── bluetooth_esp32_kolluk_controller.ino — v2.0 BLE + CRSF + MPU6050 + ELRS
├── fritzing/           # Fritzing breadboard schematics (.fzz)
├── elrs_config/        # ExpressLRS build configuration
│   ├── e28_force.json
│   └── user_defines.txt  ← edit binding phrase before flashing
└── prints/             # 3D print G-code for controller enclosure
    └── CE3S1_kapaklar.gcode
```

## Components

| Component | Model | Role |
|-----------|-------|------|
| MCU | ESP32 DevKit V1 | Main controller — BLE, I2C, ADC, GPIO, UART |
| IMU | MPU6050 | Wrist motion → roll/pitch (I2C: SDA=21, SCL=22) |
| RF Module | E28 (SX1280) 2.4 GHz | ELRS TX — drone control link (SPI) |
| Potentiometer | — | Throttle input (ADC) |
| Battery | LiPo + divider | Power + voltage monitoring |

## Firmware Features (v2.0)

- MPU6050 safe init + retry
- Arming / kill-switch safety block (1.5 s hold)
- ADC noise filter (throttle smoothing)
- MPU jitter damping (roll/pitch)
- Non-blocking millis() scheduler (no delay())
- Real battery measurement + low-battery alarm
- CRSF protocol with CRC8
- Button debouncing
- Watchdog Timer (10 s freeze protection)
- EEPROM calibration storage

## Safety Warning

> **This firmware controls a physical drone.** Always test with motors disconnected
> first. The binding phrase in `elrs_config/user_defines.txt` must be set to a unique,
> private value before flashing. Never share your binding phrase.
