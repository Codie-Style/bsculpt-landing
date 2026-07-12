# Fold Clock Widget

A native Android home-screen widget (Kotlin + Jetpack Glance) built for the
Samsung Galaxy Fold family. It renders two layouts from a single composable
and lets Glance pick the closest match whenever the widget is placed,
resized, or the phone is folded/unfolded:

- **Cover screen (narrow)** — compact time-only view, sized to fit next to
  other widgets on the Fold's outer display.
- **Unfolded (wide)** — adds the full date line and larger type, taking
  advantage of the inner display's extra width when the widget is resized up.

## Project layout

```
samsung-fold-widget/
  app/src/main/java/com/foldclock/widget/
    FoldClockWidget.kt          # Glance composable + responsive size logic
    FoldClockWidgetReceiver.kt  # AppWidgetProvider entry point
    WidgetUpdateScheduler.kt    # alarm that ticks the widget every minute
    WidgetTickReceiver.kt       # receives the tick, triggers a re-render
    MainActivity.kt             # simple host-app screen with add instructions
  app/src/main/res/xml/fold_clock_widget_info.xml   # widget metadata/sizing
```

## How the responsive sizing works

`FoldClockWidget.sizeMode` is `SizeMode.Responsive` with two reference sizes:

- `130dp x 70dp` — cover-screen footprint
- `380dp x 180dp` — unfolded-screen footprint

Inside `provideGlance`, `LocalSize.current` tells us which one Glance picked
for the current placement, and the composable switches layouts at a `240dp`
width threshold. The widget's `minResizeWidth`/`maxResizeWidth` in
`fold_clock_widget_info.xml` (110dp–720dp) are wide enough to span both the
Fold's cover screen and its unfolded grid, so users can resize the same
widget instance across both postures instead of needing two separate widgets.

## Building

This was authored in a sandboxed environment without access to
`dl.google.com` (Google's Maven repo, needed for the Android Gradle Plugin
and AndroidX/Glance artifacts), so it hasn't been compiled here. To build:

1. Open the `samsung-fold-widget/` folder in Android Studio (Iguana or
   newer). It will generate the Gradle wrapper and sync dependencies
   automatically.
2. Run the `app` configuration on a Fold device or the **Fold-in
   emulator skin** (Device Manager → Create Device → search "Fold").
3. Long-press the home screen → Widgets → **Fold Clock** → drag it on.
4. Fold/unfold the device (or drag the widget's resize handles) to see the
   layout switch.

If you don't have a physical Fold, Android Studio's emulator has built-in
Fold device definitions that simulate the fold/unfold posture change and
screen-size switch.

## Known limitations

- Glance widgets don't repaint on their own, so a lightweight `AlarmManager`
  tick (`WidgetUpdateScheduler`) re-renders the widget once a minute to keep
  the clock current. This is started in `onEnabled` and cancelled in
  `onDisabled`.
- Not yet compiled/tested against a real SDK — see "Building" above.
