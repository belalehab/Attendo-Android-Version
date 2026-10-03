# Design System Specification

## Colors
- **BackgroundMain**: #0f172a (Slate 900)
- **BackgroundSecondary**: #0a0f1c (Darker Slate)
- **Primary**: #2dd4bf (Teal 400)
- **Secondary**: #818cf8 (Indigo 400)
- **Error/Danger**: #f43f5e (Rose 500)
- **Warning**: #fbbf24 (Amber 400)
- **TextPrimary**: #ffffff
- **TextSecondary**: #94a3b8

## Typography
- Main font: Inter or Roboto.
- Monospace font for National IDs: JetBrains Mono or Roboto Mono.

## Arabic / RTL Gate
- The desktop uses Amiri-Regular.ttf for Arabic PDF rendering.
- Android Jetpack Compose natively supports RTL layout direction. We must ensure LocalLayoutDirection flips to RTL if the locale is Arabic, but the primary language of the app UI appears to be English. Arabic text in the student roster will render automatically via Android's Skia text shaping engine.

## Components
- **TopAppBar**: Customized to resemble the desktop title bar, but tailored for touch.
- **BottomNavigation**: Replaces the desktop sidebar.
- **FloatingActionButton**: Replaces global keyboard shortcuts for primary actions.
