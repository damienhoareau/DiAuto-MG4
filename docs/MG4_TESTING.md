# DiAuto-MG4 — vehicle test checklist

Platform: MG4 Comfort SWI69 / Android 9 (AUTUS SAIC MT2712).  
APK: platform-signed `com.drivehub.diauto.mg4` (see `scripts/build_mg4.ps1`).

Do **not** run DiPlay-MG4 CarPlay and DiAuto-MG4 Android Auto at the same time.

## Build & install

1. Host smoke (JDK 17+, `local.properties` → `sdk.dir`):
   ```
   .\gradlew.bat :app:compileGithubCarDebugKotlin :app:testGithubCarDebugUnitTest --tests com.andrerinas.openheadunit.vehicle.VehicleEnergyModelEncoderTest
   ```
2. `$env:MG4_PLATFORM_KEYS_DIR = "<dir with platform.pk8 and platform.x509.pem>"`
3. `powershell -File scripts\build_mg4.ps1`
4. `adb install -r ..\DiAuto-MG4-v0.3.11-mg4.1.apk`

Phone desk APK (no platform key): `powershell -File scripts\build_phone.ps1` then
`adb install -r ..\DiAuto-MG4-phone-debug.apk` (package `com.drivehub.diauto.mg4.phone`).

EVHardware is vendored at `evhardware/` (no git submodule).

## Connection (Android 9 car hotspot)

1. Open DiAuto-MG4 on the head unit.
2. Settings → Connection setup → **Built-in car hotspot** (nativeApTransport = 1; default).
3. Save the car hotspot SSID/password exactly.
4. On the phone: Wi‑Fi + Bluetooth on, pair, connect Android Auto wirelessly.
5. Confirm projection video, touch, and audio.

## Battery → Google Maps (VehicleEnergyModel)

1. Settings → Navigation → **Battery for Google Maps** ON (default).
2. Optional desk test: enable **Demo battery values**, set SoC / range / capacity, Save.
   Demo skips EVHardware (works off the car). Still needs a real AA session for Maps.
3. Logcat filter: `DiAuto-MG4`
4. Expect before/during connect:
   - `DiAuto-MG4 battery XX% range YYkm (demo)` or `(car)`
4. After AA session starts and the phone requests sensor 23:
   - `DiAuto-MG4 phone requested VEM sensor type=23`
   - `DiAuto-MG4 vem tx capacity=...Wh current=...Wh range=...km battery=...%`
5. On the **Android Auto** Maps (head-unit projection): start a route — battery % /
   arrival charge should appear (this is the path Google uses for VEM).
6. Phone-screen Google Maps (outside AA) often does **not** show a permanent SoC badge;
   look for destination / EV route estimates while AA is connected, or use AA Maps.
7. If VEM is not requested: confirm Service Discovery logged  
   `Announcing EV energy sensors (23/25/26) + ELECTRIC fuel type`  
   (requires a successful SoC read **before** handshake — reconnect after battery log appears).

## Regression

- [ ] Music playback
- [ ] Touch / map pan
- [ ] Navigation voice prompts
- [ ] Disconnect / reconnect wireless AA
- [ ] USB AA (optional)

## Failure hints

| Symptom | Likely cause |
|---|---|
| No `battery XX%` log | Not platform-signed / CPM denied / firmware not SWI68/69 |
| EV sensors not announced | No snapshot yet — wait for poll, reconnect |
| `vem tx dropped` | Phone never started sensor 23 |
| Maps shows no SoC | Phone AA / Maps version; confirm `vem tx` lines exist |
