"""Visible branding must be NOX01, never Buddy, with internal paths untouched."""
from pathlib import Path
import re
from xml.etree import ElementTree as ET
ROOT = Path(__file__).resolve().parent.parent
res = ROOT / 'app/src/main/res'
bad=[]
for p in res.glob('values*/**/*.xml'):
    try:
        root=ET.parse(p).getroot()
    except ET.ParseError as exc:
        raise AssertionError(f'Invalid XML {p}: {exc}')
    for element in root.iter():
        if element.tag not in ('string', 'item', 'plurals'): continue
        value=''.join(element.itertext())
        if re.search(r'\bBuddy\b',value):
            bad.append((str(p.relative_to(ROOT)),element.attrib.get('name','item'),value[:90]))
assert not bad, 'Buddy still visible in resources:\n'+'\n'.join(map(str,bad[:20]))
expect={
    'Bubble.java':['face.setContentDescription("NOX01")','header.setText("NOX01")'],
    'TerminalActivity.java':['"nox01 linux · alpine"'],
    'SetupActivity.java':['"NOX01 " + BuildInfo.VERSION'],
    'Updates.java':['"nox01-mobile.apk".equals','"https://github.com/levomm/nox01-mobile"'],
    'ControlIndicator.java':['setShortCriticalText("NOX01")'],
}
for name,strings in expect.items():
    code=(ROOT/'app/src/main/java/app/buddy/assistant'/name).read_text()
    for term in strings:
        assert term in code, f'{name}: missing {term}'
assert 'static final int VERSION_CODE = 210' in (ROOT/'app/src/main/java/app/buddy/assistant/BuildInfo.java').read_text()
assert 'Buddy.pull(6400)' in (ROOT/'app/src/main/java/app/buddy/assistant/CodexLive.java').read_text(), 'do not rename JavaScript bridge contract'
assert 'Buddy.pull(6400)' in (ROOT/'app/src/main/java/app/buddy/assistant/CodexDictation.java').read_text(), 'do not rename JavaScript bridge contract'
assert 'root/.buddy/' in (ROOT/'app/src/main/java/app/buddy/assistant/Helper.java').read_text(), 'do not migrate internal files'
print('PASS: no user-facing Buddy name, release name, and app version updated')
