# OpenAstralis

OpenAstralis is the Astralis client (version 2.2, Fabric, Minecraft 26.2), reversed from a live process back into readable, buildable Java source.

**Backup / Mirror: https://deobf.su/yxzero/openastralis**
Please bookmark and use this mirror if you can — the main git repo might get hit with a DMCA at any time!

- Every class, field, method and local has a descriptive name.
- Nothing is encrypted or decrypted at runtime.
- No native code is loaded.
- No license check, backend session, tamper gate or anti-debug scanner remains.

The result is 988 classes and about 133,000 lines of Java. It covers 112 modules, 12 managers, 19 commands, 109 mixins, the scripting API, the original assets and the 66 recovered third-party libraries. It builds with Gradle and was tested in game.

## Credit

OpenAstralis was done fully by **Claude Opus 5.5** at **xhigh** effort: the recovery, the deobfuscation, the naming, the verification and the documentation.

## About this version

This isn't the latest Astralis, sadly. I got this version's source the day after it was released. Michael Stetson (Cioc), Khadow and Kawase threatened me not to leak it.

![Discord message from Cioc, display name "michaelstetson school"](img/cioc-message.png)

![Discord message from Khadow on 6 October 2026, telling me not to give anyone the source](img/khadow-message.png)

## Protections, and how each was defeated

Astralis stacks several layers of protection. The table gives the short version, and the sections below give the details.

| # | Protection | How it was defeated |
| --- | --- | --- |
| 1 | Serenity loader: no class files on disk, encrypted in-memory class store, JVM attach disabled, anti-memory-dump | Memory read over DMA to get past the anti-dump protection; classes rebuilt from that dump and the class store decrypted with its own key from the heap |
| 2 | ZKM string and constant encryption (DES) | Keys read from the heap or brute-checked against DES padding, then every value replaced by a literal |
| 3 | ZKM control-flow obfuscation | Proven-constant flags folded away, junk statements and stubs removed |
| 4 | Name obfuscation | Names recovered from runtime state and leaked metadata, the rest assigned by a symbol-aware renamer |
| 5 | Native code (radioegor146 native-obfuscator, packed DLL) | Native feature setup rebuilt in Java from captured heap state; protection natives removed |
| 6 | License authentication and backend session | Backend protocol removed; server-delivered metadata recovered and bundled |
| 7 | Tamper and integrity gates | Gates and the native violation state removed |
| 8 | Anti-debug and analysis-tool detection | All analysis done statically on a dump, then the scanner removed |
| 9 | Hardware fingerprinting | Removed; a local key replaces the native identity |

Each protection is described below: what it does in the original client, then how it was removed.

### 1. Serenity loader and the encrypted class store

Astralis is not shipped as a normal mod jar. The Serenity loader starts the game itself and hands the client its classes in memory, so the classes never exist as files on disk. Classes the client has not loaded yet sit in an in-memory class store encrypted with **ChaCha20-Poly1305**. The JVM is launched with `-XX:+DisableAttachMechanism -Djdk.attach.allowAttachSelf=false`, which blocks Java agents and attach-based tools. The client also has anti-memory-dump protection. The loader also drops a native library into `%TEMP%` under a random name and deletes it when the game exits.

**Defeated by dumping the running process over DMA.** To get past the anti-memory-dump protection, the game's memory was captured over DMA (direct memory access). A DMA device read it straight from physical RAM, without opening the game process or running anything inside it. MemProcFS turned that capture into a user-mode dump of the game process. All analysis after that worked on the dump, not on the live client.

- **Loaded classes:** 738 classes were rebuilt from HotSpot's metaspace: bytecode, constant pools, exception tables and annotations. HotSpot rewrites bytecodes into internal "fast" forms as a program runs, and those rewrites were undone.
- **Classes not yet loaded:** 649 were decrypted straight out of the class store, using the ChaCha20-Poly1305 key and associated data read from the heap. Every record passed authentication, so these are exact original class files. They include 33 scripting API classes that had never been loaded.
- **Runtime-generated classes:** the dump held 2,894 classes in total. The 482 lambda helpers and 1,058 type-switch helpers that the JVM generates at runtime were filtered out, which leaves 1,387 genuine classes.
- **Independent check:** 119 method bodies from 13 original class files carved out of the dump match the rebuilt bytecode exactly, down to stack and local limits and exception handlers.

### 2. Zelix KlassMaster string and constant encryption

Astralis is obfuscated with **Zelix KlassMaster (ZKM)**. Every string, and many `int` and `long` constants, is DES-encrypted with per-class keys taken from a key table. Values are decrypted either by static initializers that fill caches at class load, or by decoder calls that receive part of their key as a method parameter. The key material lived in a separate library of 16,899 lines.

**Defeated in three steps:**

1. **Folding with real keys.** Strings and constants were first folded back in using the decrypted caches and per-class keys found in the heap. For classes that had never been initialized, so that no cache existed, the class key was recovered by trying every entry of ZKM's key table and keeping the one that produced valid DES padding.
2. **Evaluating the rest in a sandbox.** Some decoder calls get part of their key through a method parameter, so caches alone could not resolve them (about 770 call sites in the first restoration). Their arguments were recomputed from bytecode by constant propagation, and the original decoders were run in an isolated JDK-only sandbox. The last 340 runtime-decrypted constants became literals this way. A comparator confirmed that each call was replaced by exactly its value and nothing else changed.
3. **Rewriting the initializers.** 125 static initializers decrypted whole tables at class load. 88 were rewritten to their real statements. The last 37 fed decrypted values into real setup (shader uniforms, vertex formats, resource paths, colours, switch maps), and they were rewritten with the values as literals. A recording sandbox ran each old and new initializer and proved that their observable effects and final static field values are identical.

After that, the decoders, decode helpers, key fields and the key library were deleted. Nothing in the client decrypts anything any more.

### 3. ZKM control-flow obfuscation

ZKM also rewrites the control flow:

- **Opaque predicates:** fake `== null` and `!= 0` checks on static flag fields wrap the real code.
- **Self-toggling flags** in mixins.
- **Flow keys:** values such as `long k = a ^ 26773368877928L` threaded through methods.
- **Fake exception handlers:** more than 600 identity "passthrough" exception stubs.
- **String-switch keys:** dead stores of random `long` and `short` constants.

**Defeated by proving the flags constant, then folding them away.**

- **Loaded classes:** each flag's real runtime value was read from the dump and folded in.
- **Whole program:** `OpaqueFlow` runs an abstract interpretation over constants and null-ness to prove which static fields can never change from their initial state. `OpaqueFold` folds those invariants into both the old and the new build, deletes the code that becomes dead, and compares the instruction streams. All 988 classes compare equal.
- **Junk statements:** dead flow-key statements, unused stubs, key getters and setters, and the leftover key constants were removed. Each removal was checked against reaching-definition analysis of the compiled bytecode.

### 4. Name obfuscation

Classes and members are renamed to single Unicode code points, shown in source as `u0268`, `f_0427` or `m_040b`. Some are Cyrillic look-alikes such as `В`. They live in packages such as `pub.astralis.obfuscated` and `astralis.obf`. Module and setting IDs in constructors are 32-character hex hashes rather than names, and the display text for them came from the server and the native layer. After decompilation, every local was also a `varN`.

**Defeated by recovering names, then renaming by symbol.** Names came from several sources:

- **Live objects in the heap:** module names, and setting display names read from the live module objects.
- **Leaked lambda names:** original method names leaked through synthetic `lambda$<name>$N` methods.
- **Mixin injection points:** event classes were identified from the mixins that post them.
- **Reading the code:** everything left was named from its behaviour.

**Applying the names.**

- A javac-attributed renamer applied 7,684 renames to 7,086 obfuscated declarations as symbol-bound edits, covering override families, record components and file moves. A second pass named all 12,963 decompiler-named locals.
- The implementation classes were moved out of `astralis.obf` into packages named by role.
- **Proof:** the old build, remapped through the JVM rename manifest, is strictly identical to the new build, 988 of 988 classes. All 1,366 method-override relations are preserved.

### 5. Native code: native-obfuscator and a packed DLL

The most sensitive logic was moved out of Java entirely with **radioegor146's native-obfuscator**, which translates Java methods to C and compiles them to a JNI library.

- **Translated code:** 85 methods plus 22 class initializers, 107 entries across 22 classes. They are bound at startup through `Loader.registerNativesForClass(N, …)` and `Hidden0.special_clinit_N_M` stubs.
- **What runs natively:** construction of all modules, managers and commands; module and setting metadata; hardware fingerprinting; the backend WebSocket; the anti-debug kill path.
- **The library:** internally named `x64-windows.dll`, it is packed. Its code and data sections are empty on disk and are filled by a custom loader stub. A blob of about 293 MB sits at 8.00 bits per byte of entropy, and the PE entry point points into that blob, apparently as a decoy.

**Defeated by not needing the native code.** The native module was not unpacked or run. The client's state after native initialization was already in the heap, so the native feature code was rebuilt in Java from that captured state:

- the 112 module constructors, 12 manager constructors and 19 command constructors, in their original registry order;
- every module's name, description, category and tags;
- the recursive setting names, descriptions and parameter defaults.

The native protection helpers have no feature body: they are the fatal-exit and authentication paths. They were removed, and nothing replaced them.

The dumped module itself was also analysed. Its export table is intact, `JNI_OnLoad` sits in readable code at RVA `0x2f20`, and each native class's strings were matched to it through the `special_clinit_N` markers in the string pool.

The final jar contains **zero native declarations**.

### 6. License authentication and the backend session

The client authenticates against `wss://ws.serenityutils.club`:

- an encrypted handshake and encrypted frames;
- a keepalive watchdog thread that also sends the hardware fingerprint hash;
- a startup that waits for the session before the client initializes.

Every ordinary feature packet is gated on an authenticated session; only the handshake, keepalive, ping and tamper-report packets are exempt. The server and the native layer also supplied the module and setting metadata.

**Defeated by removing the backend and bundling what it supplied.**

- **Removed code:** 63 protection and backend classes, then 349 classes that became unreachable once they were gone.
- **Startup:** the callback that ran after authentication now runs directly at its original injection point in Minecraft's constructor, right after the bundled metadata loads. There is no backend wait and no session.
- **Bundled metadata:** the server-supplied metadata was recovered from the initialized heap and bundled with the mod: 119 module definitions, 1,061 settings, 133 parameter defaults and 25 shader sources. Text settings whose defaults arrived as IDs are resolved locally.

Services that only ever existed on Astralis's server are unavailable (see [Limitations](#limitations)).

### 7. Tamper and integrity gates

An identical inlined check, which reports "Tampering 0x19" or "Tampering 0x20", sits in `Module.setEnabled` and in every `EventBus` dispatch. In other words, it runs on every module toggle and every event. It compares violation-state fields that the native code writes into a central session holder. When the check fails, the client sends a tamper report and calls native fatal helpers.

**Defeated by removing the gates and the state behind them.**

- The session holder and the inlined gates are gone.
- The native class initializers of `Module` and `EventBus` were replaced with their real work: the lookup tables, and a `ConcurrentHashMap` for the bus. One opaque `Setting[]` sentinel is reproduced as a non-null array of length 5.
- The cipher caches and authentication sentinels were removed.

### 8. Anti-debug and analysis-tool detection

A dedicated scanner checks for a running debugger in several ways:

- debugger flags;
- JDWP and JMX system properties;
- `-javaagent`;
- the attach mechanism;
- the process list, read through `tasklist` or `ps`, matched against about 129 tool names, including IDA, x64dbg, System Informer, Wireshark, Fiddler, Cheat Engine, dnSpy and Frida.

A hit sends "Tampering 0x5E" with a reason code from `0xE511` to `0xE51F`, then calls a native shutdown path.

**Defeated by working statically.** The reverse engineering worked on the DMA memory dump, not on a live client under a debugger. The scanner, its report packet and its native shutdown calls were then removed.

### 9. Hardware fingerprinting

The client builds a hashed hardware ID from WMI serials on Windows (through JNA), `/sys` files on Linux, and CPU and RAM details. It sends the hash with every keepalive. Native identity code also protected the locally saved alt accounts.

**Defeated by removal.** The fingerprinting class is gone, and a local account-encryption key replaces the native identity, so saved accounts still work.

## How behaviour was kept intact

Every pass was checked against the previous build's bytecode, not just compiled:

- **Constant substitution:** each replaced call must equal its sandbox-evaluated value.
- **Dead-code removal:** both builds are canonicalized, with locals named by reaching definitions, and their instruction streams compared.
- **Initializer rewrites:** compared by their recorded observable effects.
- **Renames:** the old build, remapped through the rename manifest, must match the new build exactly.
- **Dispatch:** override relations are compared across builds.
- **Negative controls:** deliberately wrong edits must be rejected.

Each pass was then tested in game before it was kept.

## Building

Use **JDK 25**. The Gradle wrapper downloads Gradle 9.7.1, and the first build also downloads Minecraft, Fabric and the Maven dependencies.

Windows:

```powershell
.\gradlew.bat clean build
```

Other platforms:

```bash
./gradlew clean build
```

The mod jar is written to `build/libs/astralis-2.2.jar`. Fabric Loader and Fabric API are needed at runtime. The recovered libraries are bundled as nested jars, and the scripting API is compiled from this project's sources.

For an isolated development launch, which uses this project's `run/` folder and needs no launcher or loader:

```powershell
.\gradlew.bat runClient
```

The sources use four-space Google Java Format (AOSP style). Run `.\gradlew.bat formatJava` to format them and `.\gradlew.bat checkJavaFormat` to check them.

## Limitations

These services only ever existed on Astralis's backend, and no client-side route to them exists, so they are unavailable:

- Astralis account social features: IRC, friends and presence
- cloud config sharing (local configs work)
- timed lyrics
- the resource-pack catalogue and its download links
- Astralis account avatars (the Minecraft username is shown instead)

Everything client-side is kept: Minecraft and Microsoft account login, multiplayer, local configs, scripting and all 112 modules.

Native initialization was rebuilt from captured state rather than from the native code itself. Any native side effects that never showed up in the heap cannot be proven identical.

## Source layout

- `src/main/java/astralis/`: modules, settings, events, rotation, UI, config, accounts, scripting runtime, networking, utilities and mixins
- `src/main/java/com/astralis/scripting/`: the recovered public scripting API
- `src/main/java/astralis/metadata/`: loads the bundled module and setting metadata that replaces the backend
- `src/main/resources/`: mod metadata, mixin config, assets, shaders and `astralis-metadata.json`, the bundled module and setting metadata
- `libs/exact/`, `libs/compile-only/` and `libs/manifest.json`: the bundled library jars, with their checksums and inclusion roles
