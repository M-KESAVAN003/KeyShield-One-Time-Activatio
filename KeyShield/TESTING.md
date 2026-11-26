# Testing
## Automated
`./gradlew testDebugUnitTest`: classification, digit integrity, session stability, toggles,
Enter action. These do not exercise the Android service or views.

## Manual device checklist (none run yet)
- [ ] KeyShield appears in system keyboard settings and can be selected
- [ ] Text field shows QWERTY; shift, caps lock, symbols, space work
- [ ] Number field shows ten distinct digits
- [ ] Layout is unchanged while typing and after rotating
- [ ] Leaving and refocusing a field gives a new layout
- [ ] Number password, decimal and phone fields behave as in the README table
- [ ] Backspace (tap, hold, with a selection), Clear, Next/Done work
- [ ] Adaptive toggle off gives the 1-9, 0 order
- [ ] Works in other apps with KeyShield closed, and after a reboot
- [ ] Bottom row is not covered by the navigation bar
- [ ] TalkBack announces keys
