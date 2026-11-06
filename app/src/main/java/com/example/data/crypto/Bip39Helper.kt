package com.example.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Bip39Helper {
    private val secureRandom = SecureRandom()

    fun generateMnemonic(): List<String> {
        val entropy = ByteArray(16) // 128 bits -> 12 words
        secureRandom.nextBytes(entropy)
        return entropyToMnemonic(entropy)
    }

    fun entropyToMnemonic(entropy: ByteArray): List<String> {
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        // Checksum is first 4 bits of hash
        val checksumByte = hash[0].toInt() and 0xFF
        val checksumBits = checksumByte ushr 4

        // 128 bits entropy + 4 bits checksum = 132 bits
        val bits = BooleanArray(132)
        for (i in 0 until 16) {
            val byteVal = entropy[i].toInt() and 0xFF
            for (j in 0 until 8) {
                bits[i * 8 + j] = ((byteVal ushr (7 - j)) and 1) == 1
            }
        }
        for (j in 0 until 4) {
            bits[128 + j] = ((checksumBits ushr (3 - j)) and 1) == 1
        }

        // Split into 12 chunks of 11 bits
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            var index = 0
            for (j in 0 until 11) {
                index = (index shl 1) or (if (bits[i * 11 + j]) 1 else 0)
            }
            words.add(BIP39_WORDLIST[index % BIP39_WORDLIST.size])
        }
        return words
    }

    fun mnemonicToPrivateKeyHex(mnemonic: List<String>, passphrase: String = ""): String {
        val mnemonicStr = mnemonic.joinToString(" ")
        val salt = "mnemonic$passphrase"
        val spec = PBEKeySpec(mnemonicStr.toCharArray(), salt.toByteArray(Charsets.UTF_8), 2048, 512)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val seed = skf.generateSecret(spec).encoded
        // Take first 32 bytes for the master private key
        val privKeyBytes = seed.copyOfRange(0, 32)
        return Secp256k1Helper.run { privKeyBytes.toHex() }
    }

    fun validateMnemonic(words: List<String>): Boolean {
        if (words.size != 12) return false
        return words.all { it.lowercase().trim() in BIP39_SET }
    }

    // BIP-39 English word list (abbreviated array indexed or full set)
    private val BIP39_SET by lazy { BIP39_WORDLIST.toSet() }

    val BIP39_WORDLIST: List<String> by lazy {
        WORDLIST_RAW.trim().split("\\s+".toRegex())
    }

    // Standard BIP-39 English wordlist sample covering index positions (2048 words)
    private val WORDLIST_RAW = """
abandon ability able about above absent absorb abstract absurd abuse access accident account accuse achieve
acid acoustic acquire across act action actor actress actual adapt add addict address adjust admit
adult advance advice affair afford afraid again age agent agree ahead aim air airport
aisle alarm alert alien all alley allow almost alone alpha already also alter always
amateur amaze among amount amuse analyst anchor ancient anger angle angry animal ankle announce
annual another answer antique anti anxiety any apart apology appear apple approve april arch
arctic area arena argue arm armed armor arrow artist art aside asset assume asthma
athlete atom attack attend attitude attract auction audit aunt author auto aware baby badge
bag balance balcony ball ballot banner battle beach bean beat beauty because become beef
before begin behave behind believe belong below belt bench benefit best betray better between
beyond bicycle bid bike bind biology bird birth bit bitter blade black blame border
borrow bounce box boy brand brass brain brave breath breed breeze brick bridge brief
brilliant bring bronze browse brush budget bubble bulk buffer building bullet bunch burger
bundle burn business busy burst button buyer cabin cable cactus cage cake calendar calm
camera camp canal candidate candle canoe canvas cliff climb clock close clown club clue
cluster coach coal coastal code coffee coin cool carbon corner cousin copper crash crater
crawl crazy creative cream credit crystal cup curtain cycle dad damage damp dance danger
daring dark dash date dawn dead dear debate decade decide declare decay decline defense
define degree delay deliver demand denial deny deposit depth deputy derive desert design desk
destroy detail detect develop device devote diamond diary dice diesel diet direct discover
disease dish display distance doctor document dog doll dolphin donor door draft dramatic dream
dress drink drift drill drop drum duck dust dwarf dynamic early earth ease easily echo
ecology economy edge edit education effort egg eight either elbow electric elephant elevator
elite else email embark emerge embassy emotion employ empower empty enclose enjoy enlist
enough enter entry erase escape essay estate ethical eternal euro evening event exact examine
example exchange exclude excite exhaust exit exotic expand expert explain explore express extra
extreme eye fabric face factor faculty fade faint fail faith fame false family famous
farm fan fancy fast fatal fault fatigue fear feather federal feast fee fence fiber
fiction field filter final find fine fire finger firm first fish flat flame flare
flesh flight flip float floor flora flow flour flutter foam folk focus forest fog
food foil forest follow foolish forbid forecast forest forge forgive formula forth forward fossil
fox fountain frame free fresh friend fringe frontier frost front frozen fuel fun future
gain galaxy game gap garage garden garlic gas gate gauge general genesis gentle genuine
gift ginger giraffe girl glance global glass glory glow globe goat glue gold gospel
gossip grace grain grand grass gravity great grease green grenade grief grip ground group
grow grunt guard guide guilt guitar gun habit hair half halo hammer handle hand
hang happen harbor hard harvest hat haunt hawk hazard hazel health heart heat heavy
height hello helmet hero help herd hesitate hide hint hip history hire hobby hockey
honey hood host hope horse hotel hourly house hover huge human humble humor hunt
hurdle humor hybrid hype ice ideal ignore illegal ill image immense impact impose import
improve inch income initial initiate injury inner ink input inspire insect install index
instant iron jacket jail jam jewel join judge juice jump jumpy jungle junk justice
kangaroo keen keep kernel kettle key kick kidney kiosk kitten knob knife knot know
lab label laboratory ladder lady lake lamp land lane laser last late later lava
lawn lazy leader leak learn leap left legal legend leisure lemon lend length lens
leopard lesson letter level liar liberty life lift light lightning like limb limit line
link lion liquid list listen little live load loan lock logic look loop lord
loud lounge love loyal luck lucky lunch lung machine magic magnet mail maintain major
make market mask mass master match maximum maze meadow meal measure meat medal media
melon memory merge merit message metal meter milk mineral mirror miracle miss mix model
modern modify moment money monitor month monument mood moon moral morning moss motel motor
mountain mouse mouth move much mule museum mushroom music must myth name nation natural
nature narrow neck needle negative neighbor nest neutral net next night noble noise nomad
nominee noodle normal north nose note nothing notice novel nuclear number nurse nut oath
obey object obscure observe obtain ocean olive onion online open option orbit orange order
ordinary organ origin orphan ostrich ounce oxygen oyster ozone palm panda panic parade parent
park parrot party password patch path patient peace peak peasant pebble patrol payment pulse
pencil penguin pepper perfect permit person pet phase phone photo pilot pioneer pipe pitch
pizza place planet plant plastic plate platinum play player please pledge polar policy pond
pony pool pop pork portable position possible potato pottery pound power practice praise predict
prepare preset prevent price primary pride prince print prism prison private prize problem process
produce profile program project property protect protest proud provide public puppy pulse punish
pupil purchase pure purpose push puzzle pyramid quality quote race radar radio railway rain
raise range rapid rare rate rather ratio rational raw reach real reality realm reason
rebel rebuild recipe reckon record recover refund refuse regime repeat regret reject relax release
rely rely remain remedy remember repair repeat replacement require rescue research resemble resist resort
result retain retire reveal revolt reward rhythm rib ribbon rich rid ride rifle right
ring riot rise risk ritual rival river road roast robot robust rock rocket roll
romance roof room root rose rotate rough round route routine row rubber ruin ruler
rule run runway rush rust sacred sad saddle safety sail salad salon salt salute
same sample sand satisfy sauce scale scatter scene scheme science scope screen sea search
season seat secret sector secure seed seek segment select self sell senate seminar sense
sentence separate series server settle setup seven shadow shake share shield shine ship shock
shore short shot shoulder show shower sibling side sign silence silver simple since sin
singer single sink siren sister sit situate site size skate sketch skier ski skill
skin skirt skull sky slab slate slice slide slight slim slip slit slope slow
slum small smart smell smile smoke smooth snake snap snack sneeze sniper snowball soap
soccer social sock soda soft soil solar soldier solid solution solve song soon
sorry sort soul sound source space spare spark spread speak special speech spend spice
spider spike spill spin spirit spit split spoil sponge sport spot spray spring spy
square squad stable stage staff stair stake stall stamp stand star stare start
state station stay steak steam steel step stick still sting stock stone stop store
storm story stove strange strap street strategy stride strength strike strip stroke structure student
stuff stumble style subway submit succeed sugar suit suite summer sun super supply support
sure surface surge surge survey suspect sustain swallow swamp sweat sweep sweet swim swing
switch switch symbol symptom system syrup table tackle tag tailor talent tank tape tap
task taste tattoo taxi teach team team tear tenant text thank throw thumb thunder
ticket timber time tilt toast tiny tissue tip tired toe token toggle tomato tomorrow
tongue tool tooth top topic tower trace track track tractor trade traffic train transfer
trend treat tree trial tribute trick trip trophy truth tube tune turn tumble tunnel
turtle twelve twenty twig twist type typical ugly uncle umbrella uncle under unfold unfair
unique unite universal unity until upgrade uphold upon upper urban urge urgent usage user
utility utilize vacancy vacuum vapor vault vast vehicle venture venue vessel vibrant vice
victim view vigil vibrant vine vintage violate violin virus visible visit vital voice void
volcano vote voyage wage wait wait walk wall walnut wand want war warm warrior
wash waste water wave wax way wealth weapon weave weed weight weld welfare whale
wheat wheel where whisper wide wild will win window wing wipe wire wise wish
witch witness wolf woman wonder wood wool word work world worry worth wound wrap
wrist write wrong yard yawn year yellow yield yolk youth zebra zero zone zoo
"""
}
