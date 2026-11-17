package app.buddy.assistant;

/** Locally created and editable SKILL.md recipes. Never auto-enabled or given extra permissions. */
final class PresetSkills {
    static final String[][] ALL = {
        {"message-draft", "Sõnumi mustand", "Koosta vastus sõnumirakenduses ja küsi enne saatmist kinnitust.",
         "When replying to a message, identify the recipient and conversation first. Draft the answer in the user's language. Show exact recipient and text to the user. Do not tap Send, publish or forward without explicit approval. Stop if the recipient is ambiguous."},
        {"compare-prices", "Hinnavõrdlus", "Võrdle kaupluste hindu ja salvesta allikad.",
         "Search at least three independent current sources when available. State price, currency, shipping, date and exact product version. Include working source URLs. Flag expired or unverifiable listings. Never purchase without explicit approval."},
        {"notification-summary", "Teavituste kokkuvõte", "Tee lubatud teavitustest lühike kokkuvõte.",
         "Summarize only notifications explicitly available for this task. Group by sender, urgency and next action. Do not reply, mark read or delete unless the user explicitly requests it. Do not reveal passwords or one-time codes."},
        {"web-research", "Veebiuuring", "Kontrolli väiteid mitmest sõltumatust allikast.",
         "For research tasks verify facts from multiple reputable sources, prioritize primary material, add source URLs and dates, distinguish observed facts from assumptions and flag uncertainty."},
        {"github-review", "GitHubi ülevaatus", "Kontrolli hoidla muudatusi enne nende rakendamist.",
         "Inspect repository status, target branch, diff and tests before suggesting any edit. Prefer a branch and pull request. Never push secrets, overwrite unrelated user changes or merge destructive changes without explicit approval."},
        {"phone-actions", "Telefoni toimingud", "Täida Androidi samme kontrollitult.",
         "Confirm the target app, screen and account. Prefer small reversible steps; verify each action from the visible screen. Ask before sending, posting, deleting, paying or sharing confidential information. Never bypass Android permissions."}
    };
    private PresetSkills() {}
}
