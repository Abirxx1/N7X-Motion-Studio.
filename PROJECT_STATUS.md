# N7X Motion Studio - project status

Version: 0.8.1 (export fixes, step 1 of the bug-fix plan)

## Honest state
Nothing in this file has been tested on a phone by the developer assistant. Items marked "untested" need a real export.

## Done in 0.8.0 (untested)
- Export now renders keyframe animation, transitions (fade, flash, slide x4, zoom x2, spin, flip) and
  text/sticker animations (start, stop, overall) with the same functions as the preview
  (files: export/FrameEffects.kt, export/ExportEngine.kt, export/OverlayRenderer.kt).
- Typewriter text is rendered in export. Rainbow text colour is NOT.
- Export errors now carry a full report (plan + stack trace) with a "Copy details" button.
- "Safe mode" (settings + retry button after a failure): no custom audio processing, no frame dropping,
  clips with changed speed lose their sound.

## 0.8.1
- First real error report: "Codec exception ... VideoDecoder c2.qti.avc.decoder, start failed" on a
  3840x1728 60 fps H.264 clip. Suspected cause (not confirmed): the editor preview player kept its
  hardware decoder while the export needed one. Fix: the preview player and music players are released
  when an export starts, and the preview is rebuilt when the export dialog closes.

## Known problems, root cause not confirmed
- Slow-motion export errors: cause unknown, needs the copied error report. Suspects: audio speed
  processing (SonicAudioProcessor) or timestamp mismatch between the speed change effect and the audio.
- Music export errors: cause unknown, needs the copied error report. Suspects: the silent WAV used to
  delay a music layer, sample-rate mismatch between items, or the volume processor.
- Slow motion made from a 30 fps clip is choppy: Media3 does not create in-between frames.

## Not built yet (editing features)
- Drag handles for trimming clips (trim exists only in the Trim panel).
- Audio: drag on the timeline, split, fade in/out, waveform, snapping.
- Text/sticker: trim handles and drag on the timeline (selection and the Start/End here buttons exist).
- Audio fade in/out, voiceover, ducking.
- Platform presets and auto reframing for export.

## Build results
- 0.7.1: tell the developer assistant whether GitHub Actions was green.
- 0.8.0: not built yet.

## Next task
1. Build 0.8.0. Export each case below and send the copied error report for any failure:
   normal, slow motion, text, animated text, transitions, music, music + original audio,
   trim + split, filters + transitions, many layers.
2. After export is stable: drag trim handles, audio timeline editing, text timeline handles.
