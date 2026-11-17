"""NOX01 launcher brand regression tests; runs without Android SDK."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / 'app/src/main/res/drawable'
ICON = ROOT / 'app/src/main/res/mipmap-anydpi/ic_launcher.xml'
SOURCES = ROOT / 'branding'
AN = '{http://schemas.android.com/apk/res/android}'


def a(element, name):
    return element.attrib.get(AN + name)


def test_launcher():
    launcher = ET.parse(ICON).getroot()
    assert launcher.tag == 'adaptive-icon'
    assert {a(c, 'drawable') for c in launcher} == {
        '@drawable/ic_launcher_background',
        '@drawable/ic_launcher_foreground',
        '@drawable/ic_launcher_monochrome',
    }, 'All adaptive icon layers must be wired'

    background = ET.parse(DRAW / 'ic_launcher_background.xml').getroot()
    assert background.tag == 'vector'
    colors = [a(p, 'fillColor') for p in background.iter('path')]
    assert '#FF101216' in colors, 'NOX01 black background expected'
    assert not list(background.iter('{http://schemas.android.com/aapt}attr')), 'No Buddy gradient'

    foreground = ET.parse(DRAW / 'ic_launcher_foreground.xml').getroot()
    paths = list(foreground.iter('path'))
    assert len(paths) >= 4, 'Both brackets and 01 are needed'
    color_paths = [p for p in paths if a(p, 'fillColor') == '#FFF37656']
    white_paths = [p for p in paths if a(p, 'fillColor') == '#FFF5F4F0']
    assert len(color_paths) == 2, 'Two coral terminal brackets'
    assert len(white_paths) == 2, 'Two white characters: 0 and 1'
    assert any('M90,89' in a(p, 'pathData').replace(' ', '') for p in white_paths), 'Original 0 shape missing'
    groups = list(foreground.iter('group'))
    assert len(groups) == 1 and float(a(groups[0], 'scaleX')) <= 0.82, 'Keep emblem inside adaptive safe zone'

    monochrome = ET.parse(DRAW / 'ic_launcher_monochrome.xml').getroot()
    mono_paths = list(monochrome.iter('path'))
    assert len(mono_paths) == 4
    assert {a(p, 'fillColor') for p in mono_paths} == {'#FFFFFFFF'}, 'Themed icon must be pure white alpha'
    assert all(a(p, 'pathData') for p in mono_paths)

    for filename in ('NOX01-icon.svg', 'NOX01-wordmark.svg', 'NOX01-monochrome.svg'):
        source = SOURCES / filename
        assert source.is_file() and source.stat().st_size > 300, f'Brand source {filename} missing'
        ET.parse(source)

    print('PASS: B [01] launcher, monochrome, safe-area and sources')


if __name__ == '__main__':
    test_launcher()
