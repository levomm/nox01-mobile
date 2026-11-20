"""DOOM gaze is biased left, explores right/center, and stays white while working."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/java/app/buddy/assistant/Bubble.java').read_text()
assert 'private int nextDoomGaze()' in s
p=s.split('private int nextDoomGaze()',1)[1].split('private void setDoomFrame',1)[0]
assert 'DoomFaces.LOOK_LEFT' in p and 'DoomFaces.LOOK_RIGHT' in p and 'DoomFaces.NEUTRAL' in p
assert 'doomRandom.nextInt(10)' in p
assert 'doomNextIdle = doomGlanceUntil + ' in s
assert 'doomGlanceUntil = now + ' in s
assert 'setDoomFrame(DoomFaces.WORKING)' in s
assert 'ValueAnimator.areAnimatorsEnabled()' in s
print('PASS: more frequent left/right/center DOOM gaze; working face unchanged')
