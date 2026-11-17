"""Check current public-facing NOX01 repository copy and documentation assets."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re, struct

ROOT = Path(__file__).resolve().parents[1]
readme = (ROOT / 'README.md').read_text('utf-8')
def check(label, condition):
    assert condition, label
    print('PASS:', label)

check('Only NOX01 in the public README', not re.search(r'\bBuddy\b|buddy-android|ghorbelhamdi',readme,re.I))
check('README points to its own GitHub releases',
    'https://github.com/levomm/nox01-mobile/releases/latest/download/nox01-mobile.apk' in readme)
check('README is Estonian and offers concrete examples',
    'Näidisülesanded' in readme and 'Telegram' in readme and 'Paigaldamine Androidis' in readme)
check('No obsolete remote badges or media links', not re.search(r'producthunt|f-droid\.org|docs/(?:banner\.png|screens/|buddy-demo)',readme,re.I))
refs = re.findall(r'<img\s+src="(docs/[^"]+)"',readme)
check('README images are local and exist', len(refs)==4 and all((ROOT/r).is_file() for r in refs))
for p in (ROOT/'docs/graphics').glob('*.svg'):
    ET.parse(p)
check('New SVG illustrations parse successfully',len(list((ROOT/'docs/graphics').glob('*.svg')))==4)
check('Old image assets are gone', all(not (ROOT/p).exists() for p in [
    'docs/banner.png','docs/buddy-demo.mp4','docs/screens/chat.jpg',
    'docs/screens/chats.jpg','docs/screens/settings.jpg','docs/screens/sub-agents.gif'
]))
for p, expected in [('featureGraphic.png',(1024,500)),('icon.png',(512,512))]:
    b=(ROOT/'fastlane/metadata/android/en-US/images'/p).read_bytes()
    check(f'{p} replaced by valid-sized NOX01 PNG', b[:8]==b'\x89PNG\r\n\x1a\n'
          and struct.unpack('>II',b[16:24])==expected)
for p in ['docs/GUIDE.md','docs/PRIVACY.md','SECURITY.md','CONTRIBUTING.md','branding/README.md']:
    t=(ROOT/p).read_text('utf-8')
    check(f'{p} contains NOX01 instead of old branding', 'NOX01' in t and
          not re.search(r'\bBuddy\b',t))
for p in ['.github/ISSUE_TEMPLATE/config.yml','.github/ISSUE_TEMPLATE/bug_report.yml',
          '.github/ISSUE_TEMPLATE/feature_request.yml']:
    check(f'{p} is updated', not re.search(r'Buddy|ghorbelhamdi', (ROOT/p).read_text('utf-8')))
print('PUBLIC BRAND TESTS PASSED')
