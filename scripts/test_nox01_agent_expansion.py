from pathlib import Path
import xml.etree.ElementTree as ET
base=Path(__file__).resolve().parents[1]
draw=base/'app/src/main/res/drawable'
for s in ('anthropic','openai','opencode','google','xai'):
 p=draw/f'nox_brand_{s}.xml'
 assert p.exists(),f'Missing official logo: {s}'
 root=ET.parse(p).getroot()
 assert any(el.tag.endswith('path') for el in root.iter()),s
ui=(base/'app/src/main/java/app/buddy/assistant/Ui.java').read_text()
assert 'R.drawable.nox_brand_anthropic' in ui and 'R.drawable.nox_brand_openai' in ui
assert 'setCompoundDrawables' in ui, 'Real logo must be rendered, not abbreviation'
b=(base/'app/src/main/java/app/buddy/assistant/Bubble.java').read_text()
assert 'doomEyebrowSide' in b and '4500 + doomRandom.nextInt(3500)' in b
et=ET.parse(base/'app/src/main/res/values-et/strings.xml')
ids={x.attrib.get('name') for x in et.getroot() if x.tag=='string'}
for key in ['setup_default_brain','setup_default_brain_info','setup_agent_not_installed','setup_agent_signed_in','setup_voice_input','setup_voice_output','setup_languages','setup_assist','setup_chat_bubble','setup_tap_panel','setup_tap_talk','setup_tap_app','setup_panel_full','setup_panel_compact','skills_title','mcp_title']:
 assert key in ids,key
print('PASS: true provider logos, Estonian UI, active eyebrow animation')

skills=(base/'app/src/main/java/app/buddy/assistant/PresetSkills.java').read_text()
assert all(k in skills for k in ['message-draft','compare-prices','notification-summary','web-research','github-review','phone-actions'])
skillui=(base/'app/src/main/java/app/buddy/assistant/SkillsActivity.java').read_text()
assert 'Skills.create(this, preset[0]' in skillui
mcp=(base/'app/src/main/java/app/buddy/assistant/McpServersActivity.java').read_text()
assert 'editor(null, integration[0])' in mcp and 'suggestedName' in mcp
setup=(base/'app/src/main/java/app/buddy/assistant/SetupActivity.java').read_text()
assert 'Models.options(available.agent, true)' in setup and 'Prefs.setAgentDefault(this, selectedAgent, key)' in setup
assert not any(fake in setup for fake in ['Prefs.setAgentDefault(this, backend, "gemini-4-carbon")'])
assert 'VERSION_CODE = 213' in (base/'app/src/main/java/app/buddy/assistant/BuildInfo.java').read_text()
print('PASS: dynamic models, editable skills, MCP templates, version 213')
