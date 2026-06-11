# Intentions

Android tool for poking at IPC. Think of it as a little workbench for Intents: build and fire them off, browse what other apps expose, capture what comes in, then replay or fuzz it. Built for a rooted phone (Magisk / KernelSU / APatch), Android 10+.

## What you can do with it

- Scan installed apps and list their exported activities, services, receivers and providers — with their permissions and deep links.
- Build any Intent by hand (action, data, categories, flags, typed extras) and send it either from inside the app or as a root `am` command.
- Export whatever you build as an adb command, a `su` command, Kotlin, or JSON, plus a markdown PoC for reports.
- Capture incoming stuff two ways: an in-app sink (links/shares routed to it through the Android chooser) and a root logcat observer for activity starts. It's honest about the limits — a normal app can't see *every* Intent on the device, so capture is partial unless you go the LSPosed route.
- Repeater and fuzzer for replaying and mutating deep links / extras, plus a provider lab and a raw root console.

Most of the discovery and building works without root. The shell execution and the logcat capture need it.

## Building it

You just need Android Studio and a phone with USB debugging on. JDK 17.

1. Clone it:

   ```bash
   git clone https://github.com/lautarovculic/intentions
   cd intentions
   ```

2. Open the folder in Android Studio and let Gradle sync (first sync pulls the SDK bits it needs, give it a minute).
3. Plug in the phone, pick it in the device dropdown, hit Run.

First launch it'll ask for root — grant it so the shell and capture stuff actually works.

If you don't feel like opening the IDE:

```bash
./gradlew installDebug
```

## Exporting

Anything you build can be copied out in a few formats. Same Intent, as an adb command:

```bash
adb shell am start -W -n com.target/.DeepLinkActivity \
  -a android.intent.action.VIEW \
  -d 'target://open?next=//attacker.example' \
  -c android.intent.category.BROWSABLE \
  --ez debug true
```

...or as a Kotlin snippet you can drop into a PoC app:

```kotlin
val intent = Intent().apply {
    component = ComponentName("com.target", "com.target.DeepLinkActivity")
    action = "android.intent.action.VIEW"
    data = Uri.parse("target://open?next=//attacker.example")
    addCategory("android.intent.category.BROWSABLE")
    putExtra("debug", true)
}
context.startActivity(intent)
```

There's also `su` and JSON if you need them.

## Stack

Kotlin, Jetpack Compose, MVVM + use cases + repositories, Room, Coroutines. minSdk 29.