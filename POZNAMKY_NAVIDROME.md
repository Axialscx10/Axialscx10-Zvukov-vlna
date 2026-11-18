# SoundWave – napojenie na Navidrome

Táto verzia je tvoj pôvodný dizajn z AI Studia + reálna logika.

## Čo je reálne
- Prihlásenie na tvoj server (Subsonic API, token = md5(heslo + soľ)); heslo sa neukladá, pri odhlásení sa mažú aj história, fronta a lokálna cache; verejné domény bez schémy používajú predvolene `https://`
- Home, Explore, Knižnica a Detail (rýchly štart z lokálnej diskovej cache + obnova zo servera na pozadí)
- Prehrávanie na pozadí (Media3 ExoPlayer + OkHttp + 256 MB `SimpleCache` na disku + automatické obnovenie po výpadku siete v aute)
- Reálny 10-pásmový DSP ekvalizér, BassBoost, Spatial Virtualizer, ReplayGain (čítanie track/album gain tagov z OpenSubsonic), AutoEQ profily a časovač spánku v službe
- Kvalita streamu (predvolene Originál / FLAC Direct Play aj na mobilných dátach, s voliteľnou úsporou dát)
- Skenovanie knižnice na serveri, diagnostika latencie a streamu, offline sťahovanie skladieb

## Build
1. Otvor priečinok v Android Studiu alebo spusti `./gradlew assembleDebug` (priložený Gradle wrapper).
2. Debug build používa štandardný Android debug keystore (`~/.android/debug.keystore`), nie súbor v repozitári.
3. Pri prvom spustení zadaj adresu servera (napr. `hudba.beamvid.com` alebo lokálnu IP `192.168.1.10:4533`), meno a heslo.

Povolené je aj `http://` (v manifeste `usesCleartextTraffic`), lebo domáce LAN / Tailscale servery často nemajú HTTPS.
