package com.example.data

import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * Smart Genre & Category Engine for Navidrome / Subsonic libraries.
 *
 * Many personal music libraries have 80%+ of tracks tagged simply as "Rock" (or untagged),
 * leaving other genres (Metal, Power Metal, Pop, Soundtrack, Electronic, Punk, SK/CZ, Folk,
 * Spoken Word / Kids, Decades) sparse when querying raw ID3 genre tags alone.
 *
 * This catalog enriches both the genre list and `genreDetail` by combining:
 *  1. Synonym server genre tags (e.g., Soundtrack + OST + Score + Film + Game)
 *  2. Artist / band / composer recognition
 *  3. Album & track title keyword recognition (e.g., OST, Theme, Remix, Live, Acoustic)
 *  4. Slovak / Czech language & artist recognition for SK & CZ music
 *  5. Release decade classification (70s, 80s, 90s, 2000+)
 */
@Immutable
data class SmartGenreSpec(
    val canonicalName: String,
    val aliases: Set<String>,
    val serverGenreKeywords: Set<String>,
    val artistKeywords: Set<String>,
    val titleOrAlbumKeywords: Set<String> = emptySet(),
    val yearRange: IntRange? = null,
    val matchSkCzLanguage: Boolean = false,
    val subtitle: String = ""
) {
    val id: String get() = canonicalName
    val title: String get() = canonicalName
}

object SmartGenreCatalog {

    val specs: List<SmartGenreSpec> = listOf(
        SmartGenreSpec(
            canonicalName = "Power Metal",
            aliases = setOf("power metal", "melodic power metal", "symphonic power metal", "speed metal"),
            serverGenreKeywords = setOf("power metal", "speed metal", "symphonic metal", "melodic metal", "neoclassical metal"),
            artistKeywords = setOf(
                "helloween", "sabaton", "blind guardian", "avantasia", "stratovarius", "nightwish",
                "rhapsody", "luca turilli", "dragonforce", "hammerfall", "sonata arctica", "gamma ray",
                "powerwolf", "edguy", "kamelot", "freedom call", "primal fear", "alestorm", "gloryhammer",
                "beast in black", "battle beast", "running wild", "iced earth", "angra", "firewind",
                "masterplan", "axel rudi pell", "orden ogan", "bloodbound", "majestica", "dynazty",
                "unisonic", "cain's offering", "symphony x", "vision divine", "labyrinth", "elvenking",
                "wind rose", "twilight force", "brothers of metal", "amaranthe", "epica", "within temptation",
                "delain", "xandria", "tarja", "after forever", "serenity", "visions of atlantis", "evergrey",
                "brainstorm", "iron savior", "grave digger", "rage", "sinbreed", "nocturnal rites",
                "falconer", "lost horizon", "celesty", "dreamtale", "insania", "dark moor", "opera magna",
                "heavenly", "fairyland", "galneryus", "versailles", "lovebites", "unleash the archers",
                "theocracy", "seventh wonder", "pagan's mind", "circus maximus", "symfobia", "eufory",
                "signum regis", "within silence", "anthology", "castaway"
            ),
            titleOrAlbumKeywords = setOf("power metal", "keeper of the seven keys", "primo victoria", "carolus rex")
        ),
        SmartGenreSpec(
            canonicalName = "Heavy & Thrash Metal",
            aliases = setOf("metal", "heavy metal", "thrash metal", "death metal", "black metal", "heavy & thrash metal"),
            serverGenreKeywords = setOf(
                "metal", "heavy metal", "thrash", "death metal", "black metal", "doom metal",
                "groove metal", "nu metal", "folk metal", "viking metal", "pagan metal", "gothic metal",
                "progressive metal", "industrial metal", "metalcore", "sludge"
            ),
            artistKeywords = setOf(
                "iron maiden", "judas priest", "metallica", "manowar", "accept", "u.d.o.", "udo",
                "black sabbath", "dio", "ozzy osbourne", "megadeth", "slayer", "anthrax", "testament",
                "kreator", "destruction", "sodom", "overkill", "exodus", "annihilator", "sepultura",
                "soulfly", "pantera", "motörhead", "motorhead", "saxon", "w.a.s.p.", "wasp",
                "king diamond", "mercyful fate", "queensrÿche", "queensryche", "savata", "savatage",
                "crimson glory", "metal church", "anvil", "diamond head", "venom", "bathory",
                "celtic frost", "candlemass", "paradise lost", "my dying bride", "type o negative",
                "moonspell", "tiama", "tiamat", "therion", "lacuna coil", "amophis", "amorphis",
                "sentenced", "children of bodom", "in flames", "dark tranquillity", "at the gates",
                "arch enemy", "amon amarth", "ensiferum", "korpiklaani", "finntroll", "turisas",
                "eluveitie", "wintersun", "kalmah", "insomnium", "be'lakor", "opeth", "katatonia",
                "dream theater", "pain of salvation", "meshuggah", "gojira", "mastodon", "lamb of god",
                "machine head", "fear factory", "static-x", "rob zombie", "white zombie", "ministry",
                "rammstein", "oomph", "megaherz", "eisbrecher", "lindemann", "korn", "slipknot",
                "stone sour", "disturbed", "system of a down", "deftones", "mudvayne", "godsmack",
                "drowning pool", "saliva", "papa roach", "linkin park", "limp bizkit", "p.o.d.",
                "evanescence", "five finger death punch", "avenged sevenfold", "trivium",
                "bullet for my valentine", "killswitch engage", "parkway drive", "architects",
                "bring me the horizon", "volbeat", "ghost", "halestorm", "jinjer", "spiritbox",
                "babymetal", "apocalyptica", "dimmu borgir", "cradle of filth", "behemoth",
                "immortal", "satyricon", "emperor", "mayhem", "burzum", "cannibal corpse", "death",
                "morbid angel", "deicide", "obituary", "carcass", "bolt thrower", "napalm death",
                "arakain", "citron", "kreyson", "vitacit", "titanic", "root", "master's hammer",
                "debustrol", "kryptor", "torr", "alkehol", "harlej", "škwor", "skwor", "dymytry",
                "traktor", "komunál", "komunal", "dogma art", "dereš", "deres", "makar čudra"
            ),
            titleOrAlbumKeywords = setOf("heavy metal", "master of puppets", "number of the beast", "painkiller")
        ),
        SmartGenreSpec(
            canonicalName = "Hard Rock & Classic Rock",
            aliases = setOf("hard rock", "classic rock", "arena rock", "glam rock", "blues rock", "hard rock & classic rock"),
            serverGenreKeywords = setOf(
                "hard rock", "classic rock", "arena rock", "glam", "southern rock", "blues rock",
                "prog rock", "progressive rock", "psychedelic rock", "art rock", "soft rock", "aor"
            ),
            artistKeywords = setOf(
                "ac/dc", "acdc", "deep purple", "led zeppelin", "queen", "scorpions", "guns n' roses",
                "guns n roses", "bon jovi", "europe", "pink floyd", "dire straits", "aerosmith",
                "kiss", "zz top", "whitesnake", "def leppard", "van halen", "rainbow", "uriah heep",
                "nazareth", "status quo", "creedence", "ccr", "john fogerty", "rolling stones",
                "the doors", "doors", "jimi hendrix", "cream", "eric clapton", "jeff beck", "yardbirds",
                "the who", "who", "kinks", "free", "bad company", "thin lizzy", "gary moore",
                "ufo", "msg", "michael schenker", "alice cooper", "billy idol", "bryan adams",
                "toto", "foreigner", "journey", "boston", "styx", "reo speedwagon", "kansas",
                "survivor", "heart", "joan jett", "pat benatar", "bonnie tyler", "meat loaf",
                "bruce springsteen", "tom petty", "bob seger", "eagles", "fleetwood mac",
                "lynyrd skynyrd", "allman brothers", "steppenwolf", "grand funk", "blue öyster cult",
                "blue oyster cult", "rush", "yes", "genesis", "king crimson", "jethro tull",
                "camel", "alan parsons", "elo", "electric light orchestra", "supertramp", "marillion",
                "manfred mann", "slade", "sweet", "t. rex", "david bowie", "iggy pop", "lou reed",
                "velvet underground", "motley crue", "mötley crüe", "poison", "skid row", "warrant",
                "cinderella", "ratt", "dokken", "twisted sister", "quiet riot", "great white",
                "tesla", "extreme", "mr. big", "ugly kid joe", "faith no more", "living colour",
                "lenny kravitz", "slash", "velvet revolver", "alter bridge", "airbourne",
                "the darkness", "gretavanfleet", "greta van fleet", "rival sons", "black stone cherry",
                "shinedown", "seether", "three days grace", "breaking benjamin", "skillet",
                "nickelback", "theory of a deadman", "daudhtry", "creed", "staind", "3 doors down",
                "puddle of mudd", "hinder", "foo fighters", "red hot chili peppers", "u2", "santana",
                "inxs", "midnight oil", "men at work", "chris rea", "joe cocker", "rod stewart",
                "Tina Turner", "tina turner", "golden earring", "smokie", "suzi quatro"
            ),
            titleOrAlbumKeywords = setOf("hard rock", "classic rock", "rock anthems", "guitar heroes")
        ),
        SmartGenreSpec(
            canonicalName = "Soundtrack (Filmy, Hry & Seriály)",
            aliases = setOf("soundtrack", "ost", "score", "film", "filmová hudba", "soundtrack (filmy, hry & seriály)"),
            serverGenreKeywords = setOf(
                "soundtrack", "ost", "score", "film", "movie", "cinema", "game", "video game",
                "musical", "stage", "tv", "series", "anime", "trailer"
            ),
            artistKeywords = setOf(
                "hans zimmer", "ennio morricone", "john williams", "howard shore", "ramin djawadi",
                "danny elfman", "james horner", "alan silvestri", "klaus badelt", "steve jablonsky",
                "brian tyler", "harry gregson-williams", "james newton howard", "alexandre desplat",
                "thomas newman", "david arnold", "john barry", "jerry goldsmith", "basil poledouris",
                "brad fiedel", "vangelis", "giorgio moroder", "harold faltermeyer", "bill conti",
                "vince dicola", "michael kamen", "eric serra", "yann tiersen", "ludovico einaudi",
                "clint mansell", "trent reznor", "atticus ross", "junkie xl", "tom holkenborg",
                "lorne balfe", "bear mccreary", "cristobal tapia", "hildur", "johann johannsson",
                "max richter", "craig armstrong", "rachel portman", "gabriel yared", "maurice jarre",
                "nino rota", "lalo schifrin", "henry mancini", "bernard herrmann", "elmer bernstein",
                "miklos rozsa", "max steiner", "vladimír godár", "svetopluk štúr", "petr hapka",
                "karel svoboda", "zdeněk liška", "luboš fišer", "jaroslav uhlíř", "angelo badalamenti",
                "two steps from hell", "thomas bergersen", "immediate music", "audiomachine",
                "es posthumus", "globus", "zack hemsey", "secrions", "ivan torrent",
                "jeremy soule", "marcin przybyłowicz", "marcin przybylowicz", "mikolai stroinski",
                "percival", "mick gordon", "jesper kyd", "inoon zur", "inon zur", "borislav slavov",
                "sarah schachner", "austin wintory", "gareth coker", "christopher larkin",
                "darren korb", "gustavo santaolalla", "akira yamaoka", "nobuo uematsu", "koji kondo",
                "yoko shimomura", "yasunori mitsuda", "keiichi okabe", "masayoshi soken",
                "martin o'donnell", "jack wall", "clint bajakian", "frank klepacki", "matt uelmen",
                "russell brower", "neal acree", "jason hayes", "kai rosenkranz", "paul romero",
                "rob king", "ambient", "orchestra", "philharmonic", "london symphony", "prague film"
            ),
            titleOrAlbumKeywords = setOf(
                "soundtrack", "o.s.t", "ost", "original score", "motion picture", "film music",
                "main theme", "main title", "end title", "end credits", "opening theme",
                "witcher", "zaklínač", "zaklinac", "skyrim", "elder scrolls", "doom", "halo",
                "assassin's creed", "assassins creed", "warcraft", "world of warcraft", "diablo",
                "starcraft", "cyberpunk", "gothic", "mafia", "fallout", "bioshock", "mass effect",
                "dragon age", "baldur's gate", "baldurs gate", "divinity", "heroes of might",
                "red dead", "gta", "grand theft auto", "need for speed", "mortal kombat",
                "final fantasy", "silent hill", "resident evil", "metal gear", "castlevania",
                "dark souls", "elden ring", "bloodborne", "god of war", "last of us", "uncharted",
                "horizon zero", "ghost of tsushima", "minecraft", "terraria", "undertale", "hollow knight",
                "star wars", "lord of the rings", "pán prsteňov", "pan prstenov", "hobbit", "gladiator",
                "interstellar", "inception", "dark knight", "batman", "joker", "spider-man", "avengers",
                "marvel", "guardians of the galaxy", "iron man", "thor", "matrix", "terminator",
                "blade runner", "alien", "predator", "robocop", "rocky", "rambo", "conan", "top gun",
                "pulp fiction", "kill bill", "django", "tarantino", "godfather", "krstný otec",
                "scarface", "good the bad", "once upon a time", "profesionál", "le professionnel",
                "pirates of the caribbean", "piráti z karibiku", "harry potter", "indiana jones",
                "jurassic park", "jurský park", "back to the future", "návrat do budúcnosti",
                "ghostbusters", "krotitelia duchov", "dirty dancing", "hriešny tanec", "grease",
                "pomáda", "bodyguard", "titanic", "braveheart", "last of the mohicans", "posledný mohykán",
                "schindler", "amelie", "intouchables", "nedotknuteľní", "requiem for a dream",
                "tron", "dune", "avatar", "mad max", "007", "james bond", "mission impossible",
                "fast and furious", "transformers", "shrek", "lion king", "leví kráľ", "frozen",
                "tarzan", "aladdin", "mulan", "hercules", "toy story", "cars", "wall-e", "up",
                "how to train your dragon", "ako si vycvičiť draka", "kung fu panda", "arcane",
                "stranger things", "game of thrones", "hra o tróny", "house of the dragon",
                "mandalorian", "breaking bad", "peaky blinders", "vikings", "vikingovia",
                "narcos", "chernobyl", "x-files", "akt x", "twin peaks", "knight rider",
                "macgyver", "a-team", "miami vice", "baywatch", "mortal kombat", "winnetou",
                "poklad na striebornom", "četník", "cetnik", "gendarme", "fantomas", "perinbaba",
                "tri oriešky", "tři oříšky", "popoluška", "popelka", "mrázik", "mrazik",
                "noc na karlštejně", "šakalí léta", "rebelové", "fontána pre zuzanu"
            )
        ),
        SmartGenreSpec(
            canonicalName = "SK & CZ Hudba",
            aliases = setOf("sk & cz", "sk & cz hudba", "slovenské", "české", "československé", "sk/cz", "cz/sk"),
            serverGenreKeywords = setOf("slovak", "czech", "czechoslovak", "slovenské", "české", "československé", "sk", "cz"),
            artistKeywords = setOf(
                "elán", "elan", "team", "paľo habera", "pavol habera", "tublatanka", "maťo Ďurinda",
                "modus", "marika gombitová", "marika gombitova", "miro žbirka", "miro zbirka", "meky",
                "richard müller", "richard muller", "banket", "peter nagy", "indigo", "pavol hammel",
                "prúdy", "prudy", "marián varga", "marian varga", "collegium musicum", "fermáta",
                "dežo ursiny", "dezo ursiny", "jaro filip", "milan lasica", "július satinský",
                "imt smile", "ivan tásler", "desmod", "kuly", "no name", "horkýže slíže",
                "horkyze slize", "iné kafe", "ine kafe", "inekafe", "gladiátor", "gladiator",
                "peha", "katarína knechtová", "katarina knechtova", "jana kirschner", "zuzana smatanová",
                "zuzana smatanova", "kristína", "kristina", "celeste buckingham", "emma drobná",
                "sima", "ego", "rytmus", "kontrafakt", "separ", "dms", "kali", "peter pann",
                "majk spirit", "h16", "moja reč", "strapo", "gleb", "vec", "zverina", "čistychov",
                "názov stavby", "para", "hex", "polemic", "vidiek", "lojzo", "slniečko", "slniecko",
                "zóna a", "zona a", "slobodná európa", "slobodna europa", "konflikt", "davová psychóza",
                "karpina", "chiki liki tu-a", "heľenine oči", "helenine oci", "bijouterrier",
                "kandráčovci", "kandracovci", "kollárovci", "kollarovci", "ščamba", "scamba",
                "drišľak", "drislak", "senzus", "otto weiter", "karol duchoň", "karol duchon",
                "michal dočolomanský", "eva kostolányiová", "jana kocianová", "marcela laiferová",
                "robo grigorov", "beáta dubasová", "beata dubasova", "vašo patejdl", "vaso patejdl",
                "ján lehocký", "peter lipa", "robo opatovský", "adam Ďurica", "adam durica",
                "peter bič project", "peter bic", "billy barman", "korben dallas", "fallgrapp",
                "le payaco", "obd", "aya", "metalinda", "money factor", "maduar", "mc erik",
                "lobby", "exil", "salco", "dara rolins", "darina rolincová", "tina", "misha",
                "karel gott", "olympic", "petr janda", "kabát", "kabat", "chinaski", "kryštof",
                "krystof", "lucie", "david koller", "robert kodym", "wanastowi vjecy", "pražský výběr",
                "stromboli", "michaela pavlíčková", "katapult", "oldřich říha", "arakain", "aleš brichta",
                "citron", "kreyson", "ladislav křížek", "vitacit", "miloš dodo doležal", "daniel landa",
                "orlík", "orlik", "tři sestry", "tri sestry", "visací zámek", "visaci zamek",
                "divokej bill", "čechomor", "cechomor", "fleret", "jaromír nohavica", "nohavica",
                "karel kryl", "vladimír mišík", "blue effect", "radim hladík", "jiří schelinger",
                "františek ringo čech", "waldemar matuška", "václav neckář", "helena vondráčková",
                "hana zagorová", "marie rottrová", "věra špinarová", "petra janů", "iveta bartošová",
                "petr sepeši", "michal david", "kroky františka janečka", "dalibor janda",
                "stanislav hložek", "petr kotvald", "jiří korn", "miroslav žbirka", "karel černoch",
                "pavel novák", "petr novák", "karel zich", "pavel bobek", "michal tučný",
                "wabi daněk", "honza nedvěd", "jan nedvěd", "františek nedvěd", "nedvědi",
                "brontosauři", "spirituál kvintet", "kamélie", "turbo", "argema", "premier",
                "butly", "buty", "mňága a žďorp", "mnaga a zdorp", "tata bojs", "mig 21",
                "monkey business", "j.a.r.", "support lesbiens", "udg", "rybičky 48", "rybicky 48",
                "harlej", "alkehol", "škwor", "skwor", "dymytry", "traktor", "komunál", "forrest jump",
                "ewa farna", "tomáš klus", "tomas klus", "xindl x", "pokáč", "pokac", "vox",
                "marek ztracený", "mirai", "mandrage", "slza", "sebastian", "jelen", "poletíme",
                "vypsaná fixa", "wohnout", "gaia mesiah", "krucipüsk", "cocotte minute", "horkýže",
                "ivan mládek", "banjo band", "maxim turbulenc", "verona", "holki", "lunetic",
                "chaozz", "psh", "indy & wich", "prago union", "rest", "paulie garand", "marpo",
                "troubleGang", "yzomandias", "viktor sheen", "calin", "stein27", "ben cristovao"
            ),
            matchSkCzLanguage = true
        ),
        SmartGenreSpec(
            canonicalName = "Pop, Disco & Hitovky",
            aliases = setOf("pop", "disco", "pop & disco", "pop, disco & hitovky", "synthpop", "funk", "soul", "r&b"),
            serverGenreKeywords = setOf(
                "pop", "disco", "synthpop", "synth-pop", "electropop", "indie pop", "dance pop",
                "europop", "schlager", "italo", "funk", "soul", "r&b", "rnb", "motown", "ballad",
                "adult contemporary", "easy listening", "chanson", "vocal"
            ),
            artistKeywords = setOf(
                "modern talking", "dieter bohlen", "thomas anders", "abba", "boney m", "depeche mode",
                "alphaville", "roxette", "ace of base", "eurythmics", "annie lennox", "a-ha", "aha",
                "pet shop boys", "erasure", "duran duran", "tears for fears", "simple minds",
                "spandau ballet", "ultravox", "orchestral manoeuvres", "omd", "human league",
                "soft cell", "visage", "camouflage", "talk talk", "new order", "yazoo",
                "phil collins", "peter gabriel", "sting", "police", "michael jackson", "madonna",
                "whitney houston", "george michael", "wham", "elton john", "bee gees", "sandra",
                "c.c. catch", "cc catch", "bad boys blue", "fancy", "silent circle", "joy",
                "falco", "opus", "modern romance", "gazebo", "ryan paris", "baltimora", "sabrina",
                "samantha fox", "kylie minogue", "jason donovan", "rick astley", "bananarama",
                "dead or alive", "kim wilde", "cyndi lauper", "laura branigan", "belinda carlisle",
                "bangles", "pointer sisters", "gloria estefan", "cher", "celine dion", "mariah carey",
                "janet jackson", "prince", "lionel richie", "stevie wonder", "earth wind",
                "kool & the gang", "village people", "gloria gaynor", "donna summer", "chaka khan",
                "james brown", "ray charles", "aretha franklin", "tina turner", "sade", "simply red",
                "ub40", "bob marley", "inner circle", "eddy grant", "shaggy", "sean paul",
                "britney spears", "christina aguilera", "justin timberlake", "nsync", "backstreet boys",
                "westlife", "take that", "robbie williams", "spice girls", "destiny's child",
                "beyonce", "beyoncé", "rihanna", "shakira", "jennifer lopez", "enrique iglesias",
                "ricky martin", "nelly furtado", "gwen stefani", "no doubt", "pink", "p!nk",
                "kelly clarkson", "avril lavigne", "katy perry", "lady gaga", "kesha", "taylor swift",
                "miley cyrus", "selena gomez", "ariana grande", "dua lipa", "billie eilish",
                "olivia rodrigo", "lana del rey", "lorde", "sia", "adele", "amy winehouse",
                "duffy", "lily allen", "ellie goulding", "rita ora", "jessie j", "zara larsson",
                "ava max", "camila cabello", "shawn mendes", "ed sheeran", "sam smith",
                "george ezra", "lewis capaldi", "harry styles", "one direction", "bruno mars",
                "weeknd", "the weeknd", "justin bieber", "charlie puth", "jason derulo",
                "pitbull", "flo rida", "usher", "chris brown", "ne-yo", "akon", "black eyed peas",
                "fergie", "coldplay", "imagine dragons", "onerepublic", "maroon 5", "twenty one pilots",
                "panic! at the disco", "fall out boy", "bastille", "hozier", "rag'n'bone man",
                "stromae", "indila", "zaz", "eros ramazzotti", "zucchero", "laura pausini",
                "adriano celentano", "toto cutugno", "ricchi e poveri", "al bano", "nek"
            ),
            titleOrAlbumKeywords = setOf("disco", "greatest hits", "pop hits", "dance hits", "megamix", "hitparáda")
        ),
        SmartGenreSpec(
            canonicalName = "Elektronika, Dance & 90s",
            aliases = setOf("electronic", "dance", "techno", "trance", "house", "eurodance", "elektronika", "elektronika, dance & 90s"),
            serverGenreKeywords = setOf(
                "electronic", "electronica", "dance", "techno", "trance", "house", "eurodance",
                "edm", "drum & bass", "drum and bass", "dnb", "dubstep", "ambient", "chillout",
                "downtempo", "trip-hop", "synthwave", "retrowave", "electro", "industrial", "rave",
                "hardstyle", "happy hardcore", "club", "garage", "big beat", "breakbeat"
            ),
            artistKeywords = setOf(
                "scooter", "the prodigy", "prodigy", "daft punk", "faithless", "chemical brothers",
                "fatboy slim", "moby", "underworld", "orbital", "aphex twin", "massive attack",
                "portishead", "tricky", "jean-michel jarre", "vangelis", "kraftwerk", "tangerine dream",
                "mike oldfield", "enigma", "era", "gregorian", "deep forest", "schiller",
                "robert miles", "gigi d'agostino", "gigi dagostino", "atb", "tiësto", "tiesto",
                "armin van buuren", "paul van dyk", "ferry corsten", "above & beyond", "paul oakenfold",
                "sash", "sash!", "darude", "alice deejay", "barthezz", "mauro picotto", "kai tracid",
                "blank & jones", "scooter", "brooklyn bounce", "starsplash", "special d",
                "cascada", "groove coverage", "sylver", "lasgo", "ian van dahl", "milk inc",
                "basshunter", "e-type", "dj bobo", "2 unlimited", "haddaway", "snap!", "snap",
                "dr. alban", "culture beat", "la bouche", "corona", "ice mc", "masterboy",
                "captain hollywood", "twenty 4 seven", "fun factory", "mr. president", "vengaboys",
                "aqua", "eiffel 65", "prezioso", "gabry ponte", "floorfilla", "bomfunk mc",
                "music instructor", "flying steps", "westbam", "marusha", "members of mayday",
                "dune", "charly lownoise", "mental theo", "blümchen", "blumchen", "u96", "klf",
                "the klf", "technotronic", "black box", "c+c music factory", "reel 2 real",
                "whigfield", "rednex", "los del rio", "scatma", "scatman john", "lou bega",
                "benny benassi", "eric prydz", "fedde le grand", "bob sinclar", "david guetta",
                "calvin harris", "avicii", "swedish house mafia", "axwell", "ingrosso", "alesso",
                "martin garrix", "hardwell", "afrojack", "tiesto", "dimitri vegas", "nicky romero",
                "oliver heldens", "don diablo", "robin schulz", "felix jaehn", "lost frequencies",
                "kygo", "alan walker", "marshmello", "chainsmokers", "zedd", "major lazer",
                "diplo", "dj snake", "galantis", "clean bandit", "disclosure", "rudimental",
                "sigma", "netsky", "sub focus", "wilkinson", "chase & status", "pendulum",
                "knife party", "skrillex", "deadmau5", "nero", "flux pavilion", "noisia",
                "modestep", "infected mushroom", "astrix", "vini vici", "boris brejcha",
                "charlotte de witte", "amelie lens", "carl cox", "solomun", "paul kalkbrenner",
                "röyksopp", "royksopp", "justice", "kavinsky", "carpenter brut", "perturbator",
                "the midnight", "gunship", "odesza", "flume", "bonobo", "four tet", "bicep",
                "purple disco machine", "meduza", "topic", "imanbek", "tiësto"
            ),
            titleOrAlbumKeywords = setOf("eurodance", "trance", "techno", "club mix", "extended mix", "remix", "rave")
        ),
        SmartGenreSpec(
            canonicalName = "Punk & Alternatíva",
            aliases = setOf("punk", "punk rock", "alternative", "grunge", "indie", "punk & alternatíva", "ska"),
            serverGenreKeywords = setOf(
                "punk", "pop punk", "skate punk", "hardcore", "post-punk", "new wave", "ska",
                "grunge", "alternative", "indie", "emo", "garage", "britpop", "shoegaze"
            ),
            artistKeywords = setOf(
                "the offspring", "offspring", "green day", "blink-182", "blink 182", "sum 41",
                "bad religion", "nofx", "rancid", "pennywise", "millencolin", "lagwagon",
                "descendents", "social distortion", "rise against", "anti-flag", "alkaline trio",
                "billy talent", "simple plan", "good charlotte", "new found glory", "yellowcard",
                "all time low", "my chemical romance", "afI", "the used", "ramones", "sex pistols",
                "the clash", "clash", "dead kennedys", "black flag", "minor threat", "bad brains",
                "misfits", "danzig", "the damned", "buzzcocks", "generation x", "stiff little fingers",
                "sham 69", "exploited", "the exploited", "gbh", "discharge", "crass", "cock sparrer",
                "toy dolls", "stranglers", "dropkick murphys", "flogging molly", "real mckenzies",
                "rumjacks", "pogue", "the pogues", "gogol bordello", "reel big fish", "less than jake",
                "sublime", "mighty mighty bosstones", "madness", "specials", "rancid",
                "die toten hosen", "die ärzte", "die arzte", "broilers", "wizo", "slime",
                "iné kafe", "ine kafe", "inekafe", "horkýže slíže", "horkyze slize", "zóna a",
                "zona a", "slobodná európa", "slobodna europa", "konflikt", "davová psychóza",
                "ex-tip", "extip", "karpina", "bijouterrier", "tři sestry", "tri sestry",
                "visací zámek", "visaci zamek", "plexis", "sps", "e!e", "nvú", "nvu", "znouzectnost",
                "houba", "totální nasazení", "zakázanÝ ovoce", "nirvana", "kurt cobain", "pearl jam",
                "soundgarden", "alice in chains", "stone temple pilots", "smashing pumpkins",
                "hole", "silverchair", "bush", "live", "weezer", "pixies", "sonic youth",
                "jane's addiction", "rage against the machine", "audioslave", "incubus", "muse",
                "placebo", "radiohead", "the cure", "joy division", "the smiths", "morrissey",
                "depeche mode", "echo & the bunnymen", "siouxsie", "bauhaus", "sisters of mercy",
                "nick cave", "pj harvey", "björk", "bjork", "beck", "blur", "oasis", "pulp",
                "suede", "verve", "the verve", "kasabian", "franz ferdinand", "kaiser chiefs",
                "arctic monkeys", "the strokes", "the killers", "kings of leon", "white stripes",
                "jack white", "black keys", "interpol", "editors", "bloc party", "yeah yeah yeahs",
                "arcade fire", "vampire weekend", "mgmt", "foster the people", "tame impala",
                "gorillaz", "Florence", "alt-j", "nothing but thieves", "royal blood", "måneskin", "maneskin"
            ),
            titleOrAlbumKeywords = setOf("punk", "grunge", "unplugged", "indie")
        ),
        SmartGenreSpec(
            canonicalName = "Hip-Hop & Rap",
            aliases = setOf("hip-hop", "hip hop", "rap", "trap", "hip-hop & rap"),
            serverGenreKeywords = setOf("hip-hop", "hip hop", "hiphop", "rap", "trap", "gangsta", "boom bap", "grime"),
            artistKeywords = setOf(
                "eminem", "2pac", "tupac", "notorious b.i.g.", "biggie", "dr. dre", "snoop dogg",
                "ice cube", "n.w.a", "eazy-e", "50 cent", "g-unit", "the game", "jay-z", "nas",
                "mobb deep", "wu-tang", "method man", "redman", "busta rhymes", "dmx", "ja rule",
                "nelly", "ludacris", "t.i.", "lil wayne", "rick ross", "kanye west", "kid cudi",
                "drake", "kendrick lamar", "j. cole", "travis scott", "post malone", "future",
                "migos", "21 savage", "metro boomin", "asap rocky", "a\$ap rocky", "tyler the creator",
                "mac miller", "wiz khalifa", "logic", "nf", "machine gun kelly", "macklemore",
                "cypress hill", "house of pain", "beastie boys", "run-d.m.c.", "public enemy",
                "ll cool j", "coolio", "warren g", "nate dogg", "outkast", "missy elliott",
                "timbaland", "pharrell", "fugees", "lauryn hill", "wyclef", "roots", "common",
                "mos def", "talib kweli", "gang starr", "a tribe called quest", "de la soul",
                "krs-one", "rakim", "big l", "big pun", "fat joe", "xzibit", "chamillionaire",
                "flo rida", "t-pain", "lil jon", "tech n9ne", "hopsin", "yelawolf", "hollywood undead",
                "fort minor", "mike shinoda", "body count", "ice-t", "onyx", "insane clown posse",
                "die antwoord", "bbno\$", "ghostemane", "suicideboys", "\$uicideboy\$", "juice wrld",
                "xxxtentacion", "lil peep", "pop smoke", "central cee", "stormzy", "skepta", "dave",
                "iam", "ntm", "mc solaar", "booba", "pnl", "stromae", "gims", "sido", "bushido",
                "cro", "apache 207", "bonez mc", "raf camora", "gzuz", "kollegah", "kontra k",
                "kontrafakt", "rytmus", "ego", "separ", "dms", "kali", "peter pann", "majk spirit",
                "h16", "otis", "cigo", "moja reč", "supa", "delik", "strapo", "gleb", "vec",
                "zverina", "čistychov", "názov stavby", "lúza", "drvivá menšina", "momo", "pil c",
                "fobia kid", "samey", "haha crew", "dalyb", "saul", "porsche boy", "similivinlife",
                "chaozz", "psh", "peners", "orion", "vladimir 518", "indy & wich", "la4",
                "pio squad", "dejavu", "prago union", "kato", "rest", "paulie garand", "lipa",
                "tafrob", "idea", "boy wonder", "boyband", "ty nikdy", "marpo", "troublegang",
                "ektor", "sergei barracuda", "smack", "yzomandias", "milion+", "nik tendo",
                "viktor sheen", "calin", "stein27", "kojo", "hasan", "robin zoot", "ptk", "řáhol"
            ),
            titleOrAlbumKeywords = setOf("hip-hop", "hip hop", "rap", "freestyle")
        ),
        SmartGenreSpec(
            canonicalName = "Folk, Country & Ľudovky",
            aliases = setOf("folk", "country", "ľudovky", "folk, country & ľudovky", "trampské", "acoustic", "world"),
            serverGenreKeywords = setOf(
                "folk", "country", "acoustic", "singer-songwriter", "bluegrass", "americana",
                "world", "celtic", "irish", "polka", "ethno", "ludov", "ľudov", "dechov", "tramp"
            ),
            artistKeywords = setOf(
                "johnny cash", "bob dylan", "simon & garfunkel", "paul simon", "leonard cohen",
                "neil young", "cat stevens", "jim croce", "gordon lightfoot", "john denver",
                "don mclean", "america", "crosby stills", "byrds", "mamas & the papas",
                "joan baez", "joni mitchell", "tracy chapman", "suzanne vega", "passenger",
                "mumford & sons", "lumineers", "of monsters and men", "vance joy", "edward sharpe",
                "fleet foxes", "bon iver", "iron & wine", "sufjan stevens", "elliott smith",
                "nick drake", "jeff buckley", "jack johnson", "jason mraz", "james blunt",
                "damien rice", "glen hansard", "dubliners", "chieftains", "clannad", "enya",
                "loreena mckennitt", "blackmore's night", "faun", "wardruna", "heilung", "danheim",
                "skald", "omnia", "santiano", "dArtagnan", "kelly family", "the kelly family",
                "willie nelson", "waylon jennings", "kris kristofferson", "merle haggard",
                "hank williams", "patsy cline", "dolly parton", "kenny rogers", "glen campbell",
                "johnny horton", "marty robbins", "charlie daniels", "alabama", "garth brooks",
                "george strait", "alan jackson", "brooks & dunn", "shania twain", "faith hill",
                "tim mcgraw", "keith urban", "brad paisley", "blake shelton", "luke bryan",
                "chris stapleton", "zack bryan", "morgan wallen", "luke combs", "billy ray cyrus",
                "jaromír nohavica", "nohavica", "karel kryl", "jaroslav hutka", "vladimír merta",
                "vlasta redl", "slávek janoušek", "samson lenk", "karel plíhal", "pavel dobeš",
                "jiří schmitzer", "xindl x", "pokáč", "tomáš klus", "radůza", "traband",
                "čechomor", "cechomor", "fleret", "jarmila šuláková", "bratři ebenové", "marsyas",
                "nerez", "zuzana navarová", "nedvědi", "jan nedvěd", "honza nedvěd", "františek nedvěd",
                "brontosauři", "spirituál kvintet", "poutníci", "robert křesťan", "druhá tráva",
                "cop", "javory", "hana a petr ulrychovi", "žalman", "zalman", "hop trop",
                "nezmaři", "kamarádi táborových ohňů", "k.t.o.", "kto", "greenhorns", "zelenáči",
                "michal tučný", "michal tucny", "fešáci", "fesaci", "pavel bobek", "wabi daněk",
                "wabi danek", "miki ryvola", "wabi ryvola", "kapitán kid", "pacifik", "rangers",
                "plavci", "schovanky", "věra martinová", "pavel žák", "jakub smolík",
                "františek nedvěd", "waldemar matuška", "ivan mládek", "kollárovci", "kollarovci",
                "kandráčovci", "kandracovci", "ščamba", "scamba", "drišľak", "drislak", "senzus",
                "progres", "duchoňovci", "fs ", "folklór", "folklor", "ľudová", "ludova", "cimbál",
                "moravanka", "čarovné ostrohy", "slniečko", "lojzo", "bukasový masív", "ploštín punk",
                "zuzana homolová", "soňa horňáková", "jednofázové kvasenie", " sľuk", "lúčnica"
            ),
            titleOrAlbumKeywords = setOf("folk", "country", "ľudov", "ludov", "polka", "valčík", "čardáš", "tramp")
        ),
        SmartGenreSpec(
            canonicalName = "Rozprávky, Detské & Hovorené",
            aliases = setOf("rozprávky", "detské", "hovorené slovo", "audiobook", "humor", "rozprávky, detské & hovorené"),
            serverGenreKeywords = setOf(
                "spoken", "audiobook", "speech", "comedy", "humor", "humour", "cabaret",
                "kids", "children", "child", "fairy", "story", "podcast", "radio drama",
                "rozprav", "rozpráv", "pohad", "pohád", "dets", "hovor"
            ),
            artistKeywords = setOf(
                "pumuckl", "meister eder", "gustl bayrhammer", "hans clarin", "ellis kaut",
                "rozprávka", "rozprávky", "rozpravka", "rozpravky", "pohádka", "pohádky",
                "pohadka", "pohadky", "detské", "detske", "deťom", "detom", "pre deti",
                "spievankovo", "mária podhradská", "richard čanaky", "fíha tralala", "fiha tralala",
                "smejko a tanculienka", "miro jaroš", "miro jaros", "pacipacifik", "paci pac",
                "notový danny", "malinyjam", "bambuľka", "bambulka", "ujo ľubo", "ujo lubo",
                "pa a pi", "macko uško", "včielka maja", "vcielka maja", "šmolkovia", "smolkovia",
                "šmoulové", "gumkáči", "káčerovo", "disney", "krteček", "krtko", "pat a mat",
                "bob a bobek", "křemílek a vochomůrka", "rumburak", "arabela", "maxipes fík",
                "mach a šebestová", "spejbl", "hurvínek", "miloš kirschner", "helena štáchová",
                "jan werich", "jiří voskovec", "miroslav horníček", "milan lasica", "július satinský",
                "lasica & satinský", "lasica a satinský", "jaro filip", "radošinské naivné divadlo",
                "rnd", "stanislav štepka", "stanislav stepka", "katarína kolníková", "ander z košíc",
                "ján písančin", "pišta lakatoš", "uragán", "uragan", "susedia", "kredenc",
                "marcin", "kraus", "skrúcaný", "skrucany", "noga", "televízia", "rozhlas",
                "felix holzmann", "miloslav šimek", "jiří grossmann", "šimek & grossmann",
                "luděk sobota", "petr nárožný", "jiří krampol", "oldřich kaiser", "jiří lábus",
                "kaiser & lábus", "jára cimrman", "cimrman", "zdeněk svěrák", "ladislav smoljak",
                "vladimír menšík", "miroslav donutil", "bolek polívka", "karel šíp", "zdeněk izer",
                "ivan mládek", "zdeněk pohlreich", "audiokniha", "čítanie", "povesti"
            ),
            titleOrAlbumKeywords = setOf(
                "pumuckl", "rozprávk", "rozpravk", "pohádk", "pohadk", "bambuľk", "spievankov",
                "fíha", "detské", "pre deti", "audiokniha", "hovorené slovo", "scénka", "scénky",
                "humor", "vtipy", "kabaret", "divadlo", "cimrman", "hurvínek"
            )
        ),
        SmartGenreSpec(
            canonicalName = "Klasika, Jazz & Inštrumentálne",
            aliases = setOf("classical", "klasika", "jazz", "blues", "instrumental", "klasika, jazz & inštrumentálne"),
            serverGenreKeywords = setOf(
                "classic", "klasik", "orchestra", "symphon", "opera", "chamber", "piano",
                "violin", "cello", "choral", "baroque", "renaissance", "romantic", "sonata",
                "concerto", "jazz", "swing", "bebop", "fusion", "smooth jazz", "big band",
                "blues", "acoustic blues", "delta blues", "instrumental", "new age", "celtic"
            ),
            artistKeywords = setOf(
                "mozart", "beethoven", "bach", "johann sebastian bach", "vivaldi", "antonio vivaldi",
                "chopin", "tchaikovsky", "čajkovskij", "cajkovskij", "dvořák", "dvorak",
                "bedřich smetana", "smetana", "leoš janáček", "bohuslav martinů", "eugen suchoň",
                "ján cikker", "brahms", "wagner", "verdi", "puccini", "rossini", "handel", "händel",
                "haydn", "schubert", "schumann", "liszt", "mendelssohn", "mahler", "bruckner",
                "richard strauss", "johann strauss", "debussy", "ravel", "satie", "bizet",
                "saint-saëns", "fauré", "berlioz", "rachmaninoff", "rachmaninov", "prokofiev",
                "shostakovich", "šostakovič", "stravinsky", "mussorgsky", "rimsky-korsakov",
                "khachaturian", "borodin", "grieg", "sibelius", "elgar", "holst", "orff", "carl orff",
                "pachelbel", "albinoni", "corelli", "telemann", "purcell", "monteverdi", "paganini",
                "enrico caruso", "luciano pavarotti", "pavarotti", "plácido domingo", "jose carreras",
                "andrea bocelli", "bocelli", "sarah brightman", "maria callas", "peter dvorský",
                "edita gruberová", "šmíd", "andré rieu", "andre rieu", "yo-yo ma", "itzhak perlman",
                "anne-sophie mutter", "david garrett", "lindsey stirling", "2cellos", "hauser",
                "vanessa-mae", "richard clayderman", "yiruma", "ludovico einaudi", "max richter",
                "karajan", "berliner philharmoniker", "wiener philharmoniker", "slovenská filharmónia",
                "česká filharmonie", "miles davis", "john coltrane", "louis armstrong",
                "duke ellington", "count basie", "glenn miller", "benny goodman", "charlie parker",
                "dizzy gillespie", "thelonious monk", "bill evans", "dave brubeck", "herbie hancock",
                "chick corea", "keith jarrett", "pat metheny", "weather report", "stan getz",
                "chet baker", "ella fitzgerald", "billie holiday", "nina simone", "sarah vaughan",
                "dinah washington", "nat king cole", "frank sinatra", "dean martin", "bing crosby",
                "tony bennett", "michael bublé", "michael buble", "norah jones", "diana krall",
                "jamie cullum", "gregory porter", "amy winehouse", "ray charles", "b.b. king",
                "bb king", "muddy waters", "howlin' wolf", "john lee hooker", "albert king",
                "freddie king", "buddy guy", "stevie ray vaughan", "srv", "rory gallagher",
                "peter green", "gary moore", "joe bonamassa", "beth hart", "keb' mo'", "taj mahal",
                "robert johnson", "eric clapton", "jj cale", "mark knopfler", "carlos santana",
                "jeff beck", "joe satriani", "steve vai", "eric johnson", "yngwie malmsteen",
                "paul gilbert", "buckethead", "guthrie govan", "polyphia", "animals as leaders",
                "kenny g", "candy dulfer", "chuck mangione", "george benson", "al jarreau",
                "paco de lucia", "al di meola", "john mclaughlin", "rodrigo y gabriela",
                "peter lipa", "milan lasica", "bratislava hot serenaders"
            ),
            titleOrAlbumKeywords = setOf(
                "symphony", "symfónia", "concerto", "koncert", "sonata", "sonáta", "nocturne",
                "requiem", "overture", "philharmonic", "orchestra", "instrumental", "jazz", "blues"
            )
        ),
        SmartGenreSpec(
            canonicalName = "80. roky (80s Hits)",
            aliases = setOf("80s", "80. roky", "80. roky (80s hits)", "1980s", "eighties"),
            serverGenreKeywords = setOf("80s", "80's", "1980", "eighties", "new wave", "synthpop", "italo disco"),
            artistKeywords = setOf(
                "modern talking", "alphaville", "sandra", "c.c. catch", "bad boys blue", "a-ha",
                "roxette", "eurythmics", "pet shop boys", "erasure", "duran duran", "tears for fears",
                "simple minds", "omd", "human league", "falco", "kim wilde", "cyndi lauper",
                "laura branigan", "rick astley", "europe", "bon jovi", "wham"
            ),
            titleOrAlbumKeywords = setOf("80s", "80's", "80.", "1980", "1981", "1982", "1983", "1984", "1985", "1986", "1987", "1988", "1989"),
            yearRange = 1980..1989
        ),
        SmartGenreSpec(
            canonicalName = "90. roky (90s Hits)",
            aliases = setOf("90s", "90. roky", "90. roky (90s hits)", "1990s", "nineties"),
            serverGenreKeywords = setOf("90s", "90's", "1990", "nineties", "eurodance", "grunge", "britpop"),
            artistKeywords = setOf(
                "ace of base", "2 unlimited", "haddaway", "snap", "dr. alban", "culture beat",
                "la bouche", "corona", "dj bobo", "masterboy", "fun factory", "mr. president",
                "vengaboys", "aqua", "eiffel 65", "robert miles", "gigi d'agostino", "sash",
                "darude", "prodigy", "scooter", "backstreet boys", "spice girls", "nirvana",
                "offspring", "green day", "oasis", "blur", "maduar", "mc erik"
            ),
            titleOrAlbumKeywords = setOf("90s", "90's", "90.", "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999"),
            yearRange = 1990..1999
        ),
        SmartGenreSpec(
            canonicalName = "70. roky & Oldies",
            aliases = setOf("70s", "60s", "oldies", "70. roky", "70. roky & oldies", "retro"),
            serverGenreKeywords = setOf("70s", "60s", "50s", "oldies", "retro", "rock and roll", "rock 'n' roll", "motown", "disco"),
            artistKeywords = setOf(
                "abba", "boney m", "bee gees", "smokie", "sweet", "slade", "suzi quatro",
                "middle of the road", "christie", "mungo jerry", "t. rex", "status quo",
                "creedence", "ccr", "beatles", "the beatles", "elvis presley", "chuck berry",
                "little richard", "buddy holly", "roy orbison", "beach boys", "monkees",
                "animals", "troggs", "kinks", "hollies", "searchers", "procol harum", "mamas"
            ),
            titleOrAlbumKeywords = setOf("70s", "60s", "oldies", "retro", "1970", "1971", "1972", "1973", "1974", "1975", "1976", "1977", "1978", "1979"),
            yearRange = 1950..1979
        )
    )

    private val skCzCharRegex = Regex("[ľščťžýáíéúäôňďŕĺřěůĽŠČŤŽÝÁÍÉÚÄÔŇĎŔĹŘĚŮ]")
    private val skCzWordsRegex = Regex(
        "(?i)\\b(som|si|sme|ste|sú|nie|len|keď|ked|ako|prečo|preco|láska|laska|život|zivot|" +
            "srdce|noc|deň|den|slzy|oči|oci|domov|cesta|svet|nebo|slnko|dážď|vietor|" +
            "mám|mam|máš|mas|chcem|nechcem|viem|neviem|bude|bolo|ľúbim|lubim|nikdy|navždy|" +
            "pieseň|piesen|skladba|rozprávka|rozpravka|pohádka|pohadka|česk|slovensk|" +
            "jsem|jsi|jsme|nejsem|když|kdyz|lásko|život|svět|slunce|déšť|vítr|píseň)\\b"
    )
    private val yearRegex = Regex("\\b(19\\d{2}|20\\d{2})\\b")

    /**
     * Finds the matching [SmartGenreSpec] for any requested genre string (whether a smart genre
     * name like "Power Metal" or a raw server genre like "Pop", "Metal", "Soundtrack", "Punk").
     */
    fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun findSpec(genreName: String): SmartGenreSpec? {
        val norm = genreName.trim().lowercase(Locale.ROOT)
        specs.firstOrNull {
            it.canonicalName.lowercase(Locale.ROOT) == norm || norm in it.aliases
        }?.let { return it }

        return specs.firstOrNull { spec ->
            spec.serverGenreKeywords.any { kw ->
                norm == kw || (kw.length >= 4 && norm.contains(kw))
            }
        }
    }

    /**
     * Checks if a [TrackItem] belongs to [spec] based on artist, album, title, year, or SK/CZ language.
     * Optimized to avoid redundant regex cleaning inside large catalog loops.
     */
    fun matchesTrack(track: TrackItem, spec: SmartGenreSpec): Boolean {
        if (spec.yearRange != null && track.year in spec.yearRange) {
            return true
        }
        val artistLower = track.artist.lowercase(Locale.ROOT)
        val titleLower = track.title.lowercase(Locale.ROOT)

        if (spec.artistKeywords.isNotEmpty()) {
            if (artistLower in spec.artistKeywords) return true
            for (kw in spec.artistKeywords) {
                if (artistLower.contains(kw) || titleLower.contains(kw)) {
                    return true
                }
            }
        }

        if (spec.titleOrAlbumKeywords.isNotEmpty()) {
            val albumLower = track.album.lowercase(Locale.ROOT)
            for (kw in spec.titleOrAlbumKeywords) {
                if (albumLower.contains(kw) || titleLower.contains(kw)) {
                    return true
                }
            }
        }

        if (spec.matchSkCzLanguage) {
            if (skCzCharRegex.containsMatchIn(track.title) ||
                skCzCharRegex.containsMatchIn(track.artist)
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if an [AlbumItem] belongs to [spec] based on title or subtitle (artist + year).
     */
    fun matchesAlbum(album: AlbumItem, spec: SmartGenreSpec): Boolean {
        val titleLower = album.title.lowercase(Locale.ROOT)
        val subLower = album.subtitle.lowercase(Locale.ROOT)

        if (spec.yearRange != null) {
            val yearMatch = yearRegex.find(subLower)?.value?.toIntOrNull()
                ?: yearRegex.find(titleLower)?.value?.toIntOrNull()
            if (yearMatch != null && yearMatch in spec.yearRange) return true
        }

        if (spec.artistKeywords.isNotEmpty() && spec.artistKeywords.any { kw ->
                subLower.contains(kw) || titleLower.contains(kw)
            }
        ) {
            return true
        }
        if (spec.titleOrAlbumKeywords.isNotEmpty() && spec.titleOrAlbumKeywords.any { kw ->
                titleLower.contains(kw) || subLower.contains(kw)
            }
        ) {
            return true
        }
        if (spec.matchSkCzLanguage) {
            if (skCzCharRegex.containsMatchIn(album.title) ||
                skCzCharRegex.containsMatchIn(album.subtitle) ||
                skCzWordsRegex.containsMatchIn(album.title)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Given raw server genres from `getGenres`, returns a list of server genre names that match [spec]
     * or the requested [rawGenreName].
     */
    fun matchingServerGenres(rawGenreName: String, serverGenres: List<GenreItem>, spec: SmartGenreSpec?): List<String> {
        val result = linkedSetOf<String>()
        val normReq = rawGenreName.trim().lowercase(Locale.ROOT)

        // Exact or case-insensitive match first
        serverGenres.filter { it.name.trim().lowercase(Locale.ROOT) == normReq }
            .forEach { result += it.name }

        if (spec != null) {
            serverGenres.forEach { g ->
                val gLower = g.name.trim().lowercase(Locale.ROOT)
                if (gLower in spec.aliases ||
                    spec.serverGenreKeywords.any { kw ->
                        gLower == kw || (kw.length >= 4 && gLower.contains(kw))
                    }
                ) {
                    result += g.name
                }
            }
        } else if (normReq.length >= 3) {
            serverGenres.forEach { g ->
                val gLower = g.name.trim().lowercase(Locale.ROOT)
                if (gLower.contains(normReq) || normReq.contains(gLower)) {
                    result += g.name
                }
            }
        }

        if (result.isEmpty()) {
            result += rawGenreName
        }
        return result.toList()
    }
}
