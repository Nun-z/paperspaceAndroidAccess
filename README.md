# Paperspace Access (Android)

Android companion to [paperspaceBrowserAccess](https://github.com/Nun-z/paperspaceBrowserAccess): start, shutdown, or restart a [Paperspace](http://www.paperspace.com/) machine and see its current state.

## Setup
1. Download and install the APK file on your Android device.
2. On first launch, enter your machine ID and a team API key from the Paperspace console, then tap **Save**. The **Find your machine ID** and **Create an API key** links open the matching Paperspace console pages. You can change them later with **Settings**.

## Home-screen widget
Long-press the home screen, choose **Widgets**, and add **Paperspace Access**. The widget has Start, Shutdown and Restart buttons and shows the machine status. Shutdown and Restart ask for confirmation first. After a command, the widget keeps checking the status until the machine finishes starting or stopping (up to 3 minutes). Tap the status line to refresh it; otherwise Android refreshes it every 30 minutes.

## Notes
The app uses the Paperspace v1 API (`api.paperspace.com/v1/machines`). The status refreshes every 5 seconds while the app is open. Credentials are stored in app-private storage and excluded from backups.

This project was created with AI assistance, using Claude Code.

## License
Released under the [MIT License](LICENSE).
