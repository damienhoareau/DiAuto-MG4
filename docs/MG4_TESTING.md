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
4. `adb install -r ..\DiAuto-MG4-v0.3.11-mg4.3.apk`

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
5. On **Android Auto Maps on the head unit**: pick a destination and start — charging
   stops + arrival SoC should appear. That projected AA Maps surface is the path Google
   wires to VehicleEnergyModel.
6. **Phone Google Maps app** (unlock phone → open Maps → destination → Start) is a
   different UI. Even with a live AA session and working VEM (`vem tx` in logs), that
   phone chrome often shows a normal route **without** charging stops / arrival SoC.
   That is Google Maps behaviour, not a missing DiAuto sensor: if the head-unit AA Maps
   already shows the EV plan, the energy model is reaching the phone.
7. If VEM is not requested: confirm Service Discovery logged  
   `Announcing EV energy sensors (23/25/26) + ELECTRIC fuel type`  
   then reconnect after battery settings / first SoC log.

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
| HU AA Maps has EV plan, phone Maps does not | Expected Google split: use head-unit AA Maps |
| Neither surface shows SoC / charge stops | Confirm `vem tx` + EV announce; Maps/AA versions |
