# ICY-VPN
Real Android VLESS VPN client using Kotlin/Jetpack Compose + Rust + Xray/libXray.

- Android VpnService + TUN, not localhost SOCKS.
- Telegram and Android applications use the system VPN.
- Source: https://icy-person.github.io/ir/vless.txt
- Foreground polling every 10 seconds by default.
- Rust parses the first valid vless:// line.
- Xray validates the candidate before activation.
- Unchanged nodes are not restarted; failed updates restore the known-good node.
- DNS defaults to 8.8.8.8 and 8.8.4.4.
- Xray sockets are protected with libXray's Android dialer controller.
- Physical NOT_VPN network is preferred for subscription fetching.
- libXray v26.9.30 is pinned and SHA-256 verified.

Build: gradle :app:assembleDebug
Requirements: JDK 17, Android SDK/NDK, Rust and cargo-ndk.

Android may stop background work after the foreground VPN service is killed; the 10-second cadence is tied to the foreground VPN service.