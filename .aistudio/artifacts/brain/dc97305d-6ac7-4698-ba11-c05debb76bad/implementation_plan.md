# SoundWave: Systémová integrácia (Widgety, Android Auto) a dynamický audiovizuálny zážitok

Komplexný plán rozšírenia hudobného prehrávača SoundWave o domovské widgety Material You, natívnu podporu Android Auto v aute, dynamické farebné prechody podľa obalu albumu v obrazovke Práve hrá a architektúru adaptívnej inteligentnej vyrovnávacej pamäte (Smart Cache).

### User Review & Critical Decisions

> [!IMPORTANT]
> Potvrdené rozhodnutia na základe vašich odpovedí:
> - **Priorita 1**: Začíname systémovou integráciou — **Material You widgety na plochu** a **Android Auto podpora pre vodičov**.
> - **Vizuálny štýl**: Obrazovka Práve hrá dostane **dynamické farebné podfarbenie extrahované z obalu albumu s jemným gradientom**.
> - **Správa úložiska pre Smart Cache**: Automatická adaptácia a strop podľa percenta voľného miesta v telefóne, aby používateľ nemusel manuálne nastavovať gigabajty.

---

### 1. Overview & Core Concept

- **Čo to prináša**:
  1. **Material You Widget na ploche**: Pohodlné ovládanie hudby (Play/Pause, Skip, Like) priamo z domovskej obrazovky telefónu bez nutnosti otvárať aplikáciu. Widget automaticky preberá systémové farby Androidu (Dynamic Color) a obal hranej piesne.
  2. **Android Auto**: Po pripojení telefónu k autu (kábrom alebo bezdrôtovo) sa SoundWave objaví priamo na displeji infotainmentu vozidla s bezpečným rozhraním pre šoférovanie (kategórie, playlisty, obľúbené skladby a veľké ovládacie prvky).
  3. **Dynamický vizuál Práve hrá**: Atmosférické pozadie s jemným viacfarebným gradientom, ktorý plynule reaguje na dominantné tóny z obalu aktuálne prehrávaného albumu.
  4. **Smart Offline Cache (Základ)**: Inteligentné automatické ukladanie najobľúbenejších skladieb do lokálnej pamäte podľa voľného miesta v zariadení.

- **Cieľová skupina**:
  - Každodenní poslucháči, ktorí chcú ovládať prehrávanie bleskovo z plochy telefónu.
  - Vodiči hľadajúci bezplatnú, stabilnú a prémiovú alternatívu k drahým plateným aplikáciám (napr. Symfonium) pre Android Auto.

---

### 2. User Experience & Visual Design

#### A. Material You Widget (Domovská obrazovka)
- **Kompaktný a stredný rozmer**:
  - Zaoblené tvary Material 3 (plne adaptívne podľa zaoblenia ikon launcheru).
  - Vľavo zmenšenina obalu albumu s jemným tieňom.
  - V strede názov skladby, interpret a formát (napr. FLAC / 24-bit).
  - Vpravo interaktívne tlačidlá: Predchádzajúca, Prehrať/Pauza (s výrazným akcentovým pill kontajnerom), Nasledujúca.
  - Klik na widget okamžite otvorí SoundWave priamo v obrazovke Práve hrá.

#### B. Android Auto zážitok (V aute)
- **Bezpečné zobrazenie**: Žiadne zložité nastavovania počas jazdy.
- **Hlavné položky v menu auta**:
  - **Rýchly výber (Quick Picks)** – okamžité spustenie obľúbených mixov na jedno ťuknutie.
  - **Skladby s hviezdičkou (Liked Tracks)** – najobľúbenejšie piesne offline aj online.
  - **Zoznamy skladieb (Playlists)** – prehľad vytvorených playlistov.
  - **Nedávno prehrávané (Recently Played)** – pokračovanie tam, kde ste skončili.

#### C. Dynamické pozadie obrazovky Práve hrá (Now Playing)
- Namiesto statickej tmavej plochy sa pod obalom albumu a ovládacími prvkami zobrazí jemná, difúzna farebná aura.
- Farby sa plynule extrahujú z obalu albumu a blendujú do tmavého pozadia `Surface` (alebo AMOLED čiernej, ak má používateľ zvolený AMOLED motív).
- Zachováva 100% čitateľnosť textov a ovládacích prvkov s vysokým kontrastom (M3 OnSurface).

---

### 3. Key Product Decisions & Trade-Offs

- **Rozhodnutie 1: Implementácia Android Auto cez Media3 `MediaLibraryService`**
  - *Zvolený prístup*: Rozšírenie existujúceho `PlaybackService` na `MediaLibraryService` (podporujúce `MediaLibrarySession.Callback`).
  - *Prečo*: Android Auto oficiálne vyžaduje `MediaLibraryService` z balíka AndroidX Media3 pre prehliadanie stromu médií (`onGetLibraryRoot`, `onGetChildren`). Keďže SoundWave už používa Media3, je to architektonicky najčistejšie a najstabilnejšie riešenie bez zdvojených služieb.
  - *Alternatíva*: Staré `MediaBrowserServiceCompat` (zastarané, vyžaduje spätnú kompatibilitu a legacy kód).

- **Rozhodnutie 2: Widget na báze Android `AppWidgetProvider` / RemoteViews**
  - *Zvolený prístup*: Klasický vysoko optimalizovaný `AppWidgetProvider` s `RemoteViews` a `PendingIntent`.
  - *Prečo*: Okamžitá odozva pri ťuknutí na Play/Pause bez pamäťového oneskorenia, plná kompatibilita so všetkými launchermi od Androidu 8 až po Android 15.

- **Rozhodnutie 3: Extrakcia farieb obalu (Palette)**
  - *Zvolený prístup*: Použitie štandardnej `androidx.palette` / Coil bitmap analýzy pre dominantnú a sekundárnu farbu.
  - *Prečo*: Rýchle, hardvérovo nenáročné a zaručuje dokonale zladenú farebnú schému pre každý album.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID AUTO INFOTAINMENT                       │
│        (Zobrazenie stromu médií, veľké ovládanie, bezpečný režim)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ MediaBrowser IPC
┌───────────────────────────────┐   │   ┌────────────────────────────────┐
│   DOMOVSKÁ OBRAZOVKA (OS)     │   │   │     HLAVNÁ APLIKÁCIA (UI)      │
│  Material You Widget (M3)     │   │   │  NowPlaying s Dynamic Gradient │
└───────────────┬───────────────┘   │   └───────────────┬────────────────┘
                │ PendingIntent     │                   │ StateFlow
                ▼                   ▼                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     PlaybackService (MediaLibraryService)              │
│       - ExoPlayer Core + DSP AudioEffectsManager                       │
│       - MediaLibrarySession (Root, Liked, Playlists, Quick Picks)      │
│       - Broadcast updates do WidgetProvider                            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA & STORAGE LAYER                            │
│   - SettingsStore (Predvoľby, smart cache limity, história)            │
│   - OfflineManager (Automatické pred-sťahovanie na Wi-Fi)              │
│   - SubsonicApi (Streamovanie, metadata, obaly albumov)                │
└────────────────────────────────────────────────────────────────────────┘
```

#### Súčasti implementácie:
1. **Android Auto Manifest & Konfigurácia**:
   - `res/xml/automotive_app_desc.xml` s deklaráciou `<automotiveApp><uses name="media"/></automotiveApp>`.
   - Pridanie meta-data do `AndroidManifest.xml` (`com.google.android.gms.car.application`).
2. **MediaLibraryService rozšírenie**:
   - V `PlaybackService.kt` implementovať `onGetLibraryRoot`, `onGetChildren` a `onGetItem` pre ponuku hudby na displeji vozidla.
3. **SoundWaveWidgetProvider & XML rozloženie**:
   - `res/xml/soundwave_widget_info.xml`.
   - Rozloženie widgetu `soundwave_widget_layout.xml` s dynamickým podfarbením podľa systémových farieb.
   - Odozva na broadcasty z `PlaybackService` pri zmene skladby a stavu prehrávania.
4. **Dynamic Color Gradient na Now Playing**:
   - Extrakcia dominantnej farby z aktuálneho obalu albumu.
   - Plynulý gradientový filter na pozadí prehrávača.

---

### Overenie a testovanie
1. Overenie zostavenia aplikácie cez `compile_applet`.
2. Spustenie testov `:app:testDebugUnitTest`.
3. Overenie správnej registrácie v manifestoch a bezpečných intentov pre widget a Android Auto.
