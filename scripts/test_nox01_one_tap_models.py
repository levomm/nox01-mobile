"""Regression: one tap changes to an explicitly picked provider/model in both chat surfaces."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/app/buddy/assistant'
def src(n): return (JAVA/n).read_text()
models=src('Models.java')
chat=src('ChatActivity.java')
ui=src('Ui.java')
bubble=src('Bubble.java')
assert 'static java.util.List<String[]> selectableModels(String backend)' in models
assert 'Providers.forAgent(backend)' in models.split('selectableModels(String backend)',1)[1]
assert 'Providers.ref(p,' in models.split('selectableModels(String backend)',1)[1]
assert 'Ui.simpleModelPicker(this, ses.backend, ses.model,' in chat
assert 'hub.setModel(ses, selected)' in chat
assert 'routePicker(this, ses.backend' not in chat
assert 'Models.selectableModels(ses.backend)' in bubble
assert 'hub.setModel(ses, o[0])' in bubble
assert 'static void simpleModelPicker(Activity a, String backend, String current, Pick l)' in ui
assert 'Models.selectableModels(backend)' in ui
assert 'l.onPick(o[0])' in ui
assert 'chat_model_pick_one' in ui
assert 'Models.pricedOptions(backend, true)' in ui  # legacy Top 10 remains expandable in same sheet
print('PASS: one-tap grouped service/model picker on both chat surfaces')
