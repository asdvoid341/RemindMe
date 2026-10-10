# RemindMe — reminders with WhatsApp forwarding

A simple Android reminder app (Kotlin, min Android 8.0).

- Add a task with a date and time
- Repeat: one time, every day, every week, every month
- Optional WhatsApp number (type it or pick from Contacts) plus an optional custom message
- When the reminder fires you get a notification. If a number is set, tapping the notification (or its **Send on WhatsApp** button) opens that WhatsApp chat with the message already typed, and you tap **Send**.
- Turn reminders on or off from the list, and tap one to edit or delete it
- Reminders come back after a reboot

## Build and install

1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** and choose this `ReminderApp` folder. Wait for Gradle sync to finish (the first sync downloads Gradle and the SDK).
3. On your phone, turn on **Developer options → USB debugging**, then plug it in.
4. Click **Run ▶**. You can also use **Build → Build App Bundle(s) / APK(s) → Build APK(s)** and copy `app/build/outputs/apk/debug/app-debug.apk` to the phone.

## Phone settings for reliable alerts

- Allow notifications when the app asks.
- On Xiaomi, Oppo, Vivo, Samsung and similar phones, set **Battery → RemindMe → Unrestricted / No restrictions**, and turn on **Autostart** if your phone has it. Otherwise the system may delay or block alarms.

## Why WhatsApp needs one tap

Android and WhatsApp don't let any app send a WhatsApp message by itself in the background. The app opens the chat with the message filled in, and you confirm with one tap. Fully automatic sending would need either:
- the **WhatsApp Business Cloud API** (a Meta business account, a server and approved message templates), or
- an Accessibility-service "auto-clicker". This is fragile, only works while the phone is unlocked, and breaks Play Store policy.
