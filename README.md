# Paperspace Access (Android)

Android companion to [paperspaceBrowserAccess](https://github.com/Nun-z/paperspaceBrowserAccess): start, shutdown, or restart a [Paperspace](http://www.paperspace.com/) machine and see its current state.

## Setup
1. Open this folder in Android Studio and run the `app` configuration.
2. On first launch, enter your machine ID and a team API key from the Paperspace console, then tap **Save**. The **Find your machine ID** and **Create an API key** links open the matching Paperspace console pages. You can change them later with **Settings**.

The app uses the Paperspace v1 API (`api.paperspace.com/v1/machines`). The status refreshes every 5 seconds while the app is open. Credentials are stored in app-private storage and excluded from backups.
