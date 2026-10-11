package com.shsh8428822.yiddishlearner

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

private data class Word(
    val id: Int,
    val yiddish: String,
    val pronunciation: String,
    val hebrew: String,
    val example: String
)

private data class DictionaryEntry(
    val id: Int,
    val yiddish: String,
    val partOfSpeech: String,
    val gloss: String
)

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val forest = Color.rgb(32, 86, 63)
    private val forestDark = Color.rgb(23, 63, 47)
    private val cream = Color.rgb(249, 247, 240)
    private val ink = Color.rgb(38, 44, 40)
    private val muted = Color.rgb(100, 108, 101)
    private val paleGreen = Color.rgb(231, 239, 231)

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var currentPage: LinearLayout
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var quizQuestions: List<Word> = emptyList()
    private var quizPosition = 0
    private var quizScore = 0
    private var reviewQueue: List<Word> = emptyList()
    private var reviewPosition = 0
    private val chatLines = mutableListOf<Pair<String, String>>()
    private var dictionaryEntries: List<DictionaryEntry> = emptyList()

    private val words = listOf(
        Word(1, "שלום", "שאָלעם", "שלום", "שלום!"),
        Word(2, "אַ דאַנק", "אַ דאַנק", "תודה", "אַ דאַנק פֿאַר דער הילף — תודה על העזרה"),
        Word(3, "ביטע", "ביטע", "בבקשה", "ביטע, זעץ זיך — בבקשה, שב"),
        Word(4, "יאָ", "יאָ", "כן", "יאָ, איך פֿאַרשטיי — כן, אני מבין"),
        Word(5, "ניין", "ניין", "לא", "ניין, אַ דאַנק — לא, תודה"),
        Word(6, "איך", "איך", "אני", "איך לערן — אני לומד"),
        Word(7, "דו", "דו", "אתה / את", "דו ביסט דאָ — אתה כאן"),
        Word(8, "איר", "איר", "אתם / אתן; פנייה מנומסת", "ווי זײַט איר? — מה שלומכם?"),
        Word(9, "גוט", "גוט", "טוב", "דאָס איז גוט — זה טוב"),
        Word(10, "הײַנט", "היינט", "היום", "הײַנט לערן איך — היום אני לומד"),
        Word(11, "לערנען", "לערנען", "ללמוד", "איך וויל לערנען — אני רוצה ללמוד"),
        Word(12, "פֿאַרשטיין", "פארשטיין", "להבין", "איך פֿאַרשטיי — אני מבין")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("yiddish_learner", MODE_PRIVATE)
        loadDictionary()
        tts = TextToSpeech(this, this)
        showHome()
    }

    private fun showScreen(title: String, subtitle: String? = null, content: LinearLayout.() -> Unit) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(cream)
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
            layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
        }
        val brand = TextView(this).apply {
            text = "יידיש • צעד אחר צעד"
            textSize = 13f
            setTextColor(forest)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        page.addView(brand, matchWrap())
        val heading = TextView(this).apply {
            text = title
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(forestDark)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        page.addView(heading, matchWrap())
        if (!subtitle.isNullOrBlank()) {
            val sub = TextView(this).apply {
                text = subtitle
                textSize = 15f
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(18))
            }
            page.addView(sub, matchWrap())
        }
        currentPage = page
        page.content()
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun showHome() {
        showScreen("לערנט יידיש", "לימוד יומי למתחילים • התוכן הבסיסי זמין גם ללא אינטרנט") {
            panel("ברוכים הבאים!", "נתחיל מ־12 מילים שימושיות, כלל דקדוק אחד, שיחה קצרה וחמש שאלות תרגול.")
            panel("מילון יידיש מורחב", "${dictionaryEntries.size} ערכים לא מקוונים זמינים לחיפוש. את ההגדרות במילון המורחב מציגים באנגלית.")
            actionButton("📖  מילון מורחב — 10,000 ערכים") { showDictionary() }
            actionButton("📚  שיעור 1 — שלום והיכרות") { showLesson() }
            actionButton("🧠  חזרה חכמה על מילים (SRS)") { startReview() }
            actionButton("✅  מבחן קצר — 5 שאלות") { startQuiz() }
            actionButton("💬  תרגול שיחה ביידיש") { showChat() }
            actionButton("📈  ההתקדמות שלי") { showProgress() }
            actionButton("🔊  בדיקת שמע") { speak("שלום") }
            smallText("הערה: השמע תלוי בקול יידיש המותקן במכשיר. תרגול השיחה בגרסה זו מקומי ואינו מתחבר לשירות GPT.")
        }
    }

    private fun showLesson() {
        showScreen("שיעור 1: שלום והיכרות", "12 מילים • דקדוק • שיחה • תרגול") {
            sectionTitle("1. אוצר מילים")
            words.forEach { word ->
                panel(word.yiddish, "הגייה: ${word.pronunciation}\nפירוש: ${word.hebrew}\nדוגמה: ${word.example}")
                compactButton("🔊 שמע: ${word.yiddish}") { speak(word.yiddish) }
            }

            sectionTitle("2. דקדוק: הפועל לערנען — ללמוד")
            panel("שינוי פשוט לפי הגוף", "איך לערן — אני לומד\nדו לערנסט — אתה לומד\nמיר לערנען — אנחנו לומדים")
            bodyText("שלוש דוגמאות:")
            bodyText("• איך לערן יידיש. — אני לומד יידיש.")
            bodyText("• דו לערנסט הײַנט. — אתה לומד היום.")
            bodyText("• מיר לערנען צוזאַמען. — אנחנו לומדים יחד.")

            sectionTitle("3. שיחה קצרה")
            val dialogue = listOf(
                "א: שלום! ווי הייסטו? — שלום! איך קוראים לך?",
                "ב: איך הייס דוד. — קוראים לי דוד.",
                "א: זייער אָנגענעם! — נעים מאוד!",
                "ב: ווי גייט עס? — מה שלומך?",
                "א: גוט, אַ דאַנק. און בײַ דיר? — טוב, תודה. ואצלך?",
                "ב: אויך גוט, אַ דאַנק. — גם טוב, תודה."
            )
            dialogue.forEach { bodyText(it) }

            sectionTitle("4. חמש שאלות תרגול")
            bodyText("1. מה פירוש אַ דאַנק? — תודה")
            bodyText("2. איך אומרים ״כן״ ביידיש? — יאָ")
            bodyText("3. השלימו: איך ___ יידיש. — לערן")
            bodyText("4. מה פירוש הײַנט? — היום")
            bodyText("5. מה פירוש פֿאַרשטיין? — להבין")
            actionButton("התחל את המבחן האינטראקטיבי") { startQuiz() }
            actionButton("סיימתי את השיעור") {
                prefs.edit().putBoolean("lesson_1_done", true).apply()
                Toast.makeText(this@MainActivity, "ההתקדמות נשמרה במכשיר.", Toast.LENGTH_SHORT).show()
                showHome()
            }
            secondaryButton("חזרה למסך הבית") { showHome() }
        }
    }

    private fun startQuiz() {
        quizQuestions = learningWords().shuffled().take(5)
        quizPosition = 0
        quizScore = 0
        showQuizQuestion()
    }

    private fun showQuizQuestion() {
        if (quizPosition >= quizQuestions.size) {
            showScreen("סיימת את המבחן!", "הציון שלך: $quizScore מתוך ${quizQuestions.size}") {
                val oldBest = prefs.getInt("quiz_best", 0)
                if (quizScore > oldBest) prefs.edit().putInt("quiz_best", quizScore).apply()
                panel("כל הכבוד", "פתרונות המבחן:")
                quizQuestions.forEachIndexed { i, w ->
                    bodyText("${i + 1}. ${w.yiddish} = ${w.hebrew}")
                }
                actionButton("נסה שוב") { startQuiz() }
                secondaryButton("חזרה למסך הבית") { showHome() }
            }
            return
        }

        val word = quizQuestions[quizPosition]
        showScreen("שאלה ${quizPosition + 1} מתוך ${quizQuestions.size}", "בחר את הפירוש הנכון") {
            panel(word.yiddish, "הגייה: ${word.pronunciation}")
            val options = (listOf(word.hebrew) + learningWords().filter { it.id != word.id }.shuffled().take(3).map { it.hebrew }).distinct().shuffled()
            options.forEach { option ->
                actionButton(option) {
                    if (option == word.hebrew) quizScore++
                    quizPosition++
                    showQuizQuestion()
                }
            }
            secondaryButton("בטל מבחן") { showHome() }
        }
    }

    private fun startReview() {
        val now = System.currentTimeMillis()
        reviewQueue = learningWords()
            .filter { prefs.getLong("due_${it.id}", 0L) <= now }
            .sortedBy { prefs.getLong("due_${it.id}", 0L) }
            .take(30)
        reviewPosition = 0
        showReviewCard()
    }

    private fun showReviewCard() {
        if (reviewQueue.isEmpty()) {
            showScreen("הכול מעודכן!", "אין כרגע כרטיסיות שמועד החזרה שלהן הגיע") {
                panel("מצוין!", "אפשר להמשיך ללמוד או לחזור בהמשך. המועדים נשמרים במכשיר.")
                actionButton("עבור לשיעור") { showLesson() }
                secondaryButton("חזרה למסך הבית") { showHome() }
            }
            return
        }
        if (reviewPosition >= reviewQueue.size) {
            showScreen("סיימת את החזרה", "נבדקו ${reviewQueue.size} כרטיסיות") {
                panel("עבודה טובה!", "החזרות הבאות נקבעו ונשמרו במכשיר.")
                actionButton("חזרה למסך הבית") { showHome() }
                actionButton("בדוק אם יש עוד כרטיסיות") { startReview() }
            }
            return
        }
        val w = reviewQueue[reviewPosition]
        showScreen("חזרה חכמה", "כרטיסייה ${reviewPosition + 1} מתוך ${reviewQueue.size}") {
            panel(w.yiddish, "תעתיק להגייה: ${w.pronunciation}")
            actionButton("הצג פירוש") { showReviewRevealed(w) }
            secondaryButton("חזרה למסך הבית") { showHome() }
        }
    }

    private fun showReviewRevealed(w: Word) {
        showScreen("האם זכרת?", "כרטיסיית חזרה חכמה") {
            panel(w.yiddish, "פירוש / הגדרה: ${w.hebrew}" + if (w.example.isNotBlank()) "\nדוגמה: ${w.example}" else "")
            actionButton("זכרתי ✅") {
                val stageKey = "stage_${w.id}"
                val stage = prefs.getInt(stageKey, 0)
                val nextStage = (stage + 1).coerceAtMost(4)
                val days = when (nextStage) { 1 -> 1; 2 -> 3; 3 -> 7; else -> 14 }
                prefs.edit()
                    .putInt(stageKey, nextStage)
                    .putLong("due_${w.id}", System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L)
                    .apply()
                reviewPosition++
                showReviewCard()
            }
            secondaryButton("לא זכרתי — לחזור בקרוב") {
                prefs.edit()
                    .putInt("stage_${w.id}", 0)
                    .putLong("due_${w.id}", System.currentTimeMillis() + 20L * 60L * 1000L)
                    .apply()
                reviewPosition++
                showReviewCard()
            }
        }
    }

    private fun showChat() {
        showScreen("מתרגלים שיחה", "תרגול מקומי פשוט • ללא חיבור לאינטרנט") {
            panel("הנושא: היכרות", "כתוב משפט קצר. המתרגל יציע תגובה ודוגמה לשימוש.")
            val transcript = TextView(this@MainActivity).apply {
                textSize = 16f
                setTextColor(ink)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.WHITE)
                text = chatLines.joinToString("\n\n") { "אתה: ${it.first}\nמורה: ${it.second}" }
            }
            addView(transcript, matchWrap())
            val input = EditText(this@MainActivity).apply {
                hint = "כתוב משפט ביידיש או בעברית"
                textSize = 16f
                setSingleLine(false)
                minLines = 1
                maxLines = 3
            }
            addView(input, matchWrap())
            actionButton("שלח תשובה") {
                val message = input.text.toString().trim()
                if (message.isBlank()) {
                    Toast.makeText(this@MainActivity, "כתוב תחילה מילה או משפט.", Toast.LENGTH_SHORT).show()
                } else {
                    chatLines.add(message to localTutorReply(message))
                    showChat()
                }
            }
            actionButton("הצג משפט לדוגמה") {
                chatLines.add("שלום, ווי גייט עס?" to "גוט, אַ דאַנק! (טוב, תודה!)")
                showChat()
            }
            secondaryButton("נקה שיחה") { chatLines.clear(); showChat() }
            secondaryButton("חזרה למסך הבית") { showHome() }
        }
    }

    private fun localTutorReply(message: String): String {
        val m = message.lowercase(Locale.ROOT)
        return when {
            m.contains("שלום") || m.contains("שאָלעם") -> "שלום! ווי גייט עס? (שלום! מה שלומך?)"
            m.contains("דאַנק") || m.contains("תודה") -> "זייער גוט! אַ דאַנק מיינט ״תודה״. (מצוין!)"
            m.contains("איך הייס") || m.contains("איך הייסט") -> "זייער אָנגענעם! (נעים מאוד!) אפשר לשאול: ווי הייסטו? — איך קוראים לך?"
            m.contains("גוט") -> "גוט! אפשר להוסיף: גוט, אַ דאַנק. — טוב, תודה."
            else -> "גוטע פּרוּוו! (ניסיון טוב!) נסה משפט קצר: איך לערן יידיש. — אני לומד יידיש."
        }
    }


    private fun loadDictionary() {
        dictionaryEntries = try {
            assets.open("yiddish_dictionary.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 4) return@mapNotNull null
                    val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val term = parts[1].trim()
                    val pos = parts[2].trim()
                    val gloss = parts[3].trim()
                    if (term.isBlank() || gloss.isBlank()) null
                    else DictionaryEntry(id, term, pos, gloss)
                }.toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun learningWords(): List<Word> {
        val selected = dictionaryEntries
            .filter { prefs.getBoolean("dict_selected_${it.id}", false) }
            .map { entry ->
                Word(
                    id = 100000 + entry.id,
                    yiddish = entry.yiddish,
                    pronunciation = "",
                    hebrew = "הגדרה באנגלית: ${entry.gloss}",
                    example = ""
                )
            }
        return words + selected
    }

    private fun showDictionary(query: String = "", page: Int = 0) {
        val needle = query.trim().lowercase(Locale.ROOT)
        val filtered = if (needle.isBlank()) dictionaryEntries else dictionaryEntries.filter {
            it.yiddish.lowercase(Locale.ROOT).contains(needle) ||
                it.gloss.lowercase(Locale.ROOT).contains(needle) ||
                it.partOfSpeech.lowercase(Locale.ROOT).contains(needle)
        }
        val pageSize = 25
        val pageCount = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(0, pageCount - 1)
        val pageEntries = filtered.drop(safePage * pageSize).take(pageSize)

        showScreen("מילון יידיש", "${dictionaryEntries.size} ערכים לא מקוונים • תוצאות ${filtered.size}") {
            panel("חיפוש במאגר", "אפשר לחפש מילה ביידיש או מילה מתוך ההגדרה באנגלית. מאגר זה כולל 10,000 ערכים; ההגדרות אינן תרגום לעברית.")
            val search = EditText(this@MainActivity).apply {
                hint = "חיפוש ביידיש או באנגלית"
                textSize = 16f
                setSingleLine(true)
                setText(query)
            }
            addView(search, matchWrap())
            actionButton("חפש") { showDictionary(search.text.toString(), 0) }
            if (dictionaryEntries.isEmpty()) {
                panel("המילון אינו זמין בגרסה זו", "המאגר נוצר בשלב בניית ה־APK. הורד את הגרסה המעודכנת לאחר שהבנייה ב־GitHub תסתיים.")
            } else if (filtered.isEmpty()) {
                panel("לא נמצאו תוצאות", "נסה לכתוב חלק מהמילה או מההגדרה באנגלית.")
            } else {
                bodyText("עמוד ${safePage + 1} מתוך $pageCount")
                pageEntries.forEach { entry ->
                    panel(entry.yiddish, "הגדרה באנגלית: ${entry.gloss}\nסוג מילה: ${entry.partOfSpeech.ifBlank { "לא צוין" }}")
                    secondaryButton(
                        if (prefs.getBoolean("dict_selected_${entry.id}", false)) "✓ נוספה לרשימת הלימוד" else "הוסף לרשימת החזרות"
                    ) {
                        prefs.edit().putBoolean("dict_selected_${entry.id}", true).apply()
                        Toast.makeText(this@MainActivity, "המילה נוספה לרשימת הלימוד.", Toast.LENGTH_SHORT).show()
                        showDictionary(query, safePage)
                    }
                }
                if (safePage > 0) secondaryButton("העמוד הקודם") { showDictionary(query, safePage - 1) }
                if (safePage + 1 < pageCount) actionButton("העמוד הבא") { showDictionary(query, safePage + 1) }
            }
            secondaryButton("חזרה למסך הבית") { showHome() }
        }
    }

    private fun showProgress() {
        val deck = learningWords()
        val learned = deck.count { prefs.getInt("stage_${it.id}", 0) > 0 }
        val selected = dictionaryEntries.count { prefs.getBoolean("dict_selected_${it.id}", false) }
        val lessonDone = prefs.getBoolean("lesson_1_done", false)
        val best = prefs.getInt("quiz_best", 0)
        showScreen("ההתקדמות שלי", "הנתונים נשמרים במכשיר שלך") {
            panel("השיעורים", if (lessonDone) "שיעור 1 הושלם ✅" else "שיעור 1 עדיין לא סומן כהושלם")
            panel("כרטיסיות שנלמדו", "$learned מתוך ${deck.size} מילים ברשימת הלימוד קיבלו לפחות חזרה אחת.")
            panel("מילים שנוספו מהמילון", "$selected ערכים מתוך ${dictionaryEntries.size} זמינים במאגר.")
            panel("שיא במבחן", "$best מתוך 5 תשובות נכונות.")
            actionButton("המשך לשיעור") { showLesson() }
            secondaryButton("חזרה למסך הבית") { showHome() }
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Toast.makeText(this, "מנוע הדיבור עדיין נטען או אינו זמין.", Toast.LENGTH_LONG).show()
            return
        }
        val result = tts?.setLanguage(Locale("yi")) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "לא נמצא קול יידיש במכשיר. ניתן להשתמש בכרטיסיות או להוסיף הקלטות יידיש לא מקוונות בגרסה הבאה.", Toast.LENGTH_LONG).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yiddish_word")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun panel(title: String, body: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val heading = TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(forest)
        }
        box.addView(heading, matchWrap())
        val detail = TextView(this).apply {
            text = body
            textSize = 16f
            setTextColor(ink)
            setPadding(0, dp(5), 0, 0)
        }
        box.addView(detail, matchWrap())
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(5), 0, dp(8))
        currentPage.addView(box, lp)
    }

    private fun sectionTitle(value: String) {
        val tv = TextView(this).apply {
            text = value
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(forestDark)
            setPadding(0, dp(18), 0, dp(8))
        }
        currentPage.addView(tv, matchWrap())
    }

    private fun bodyText(value: String) {
        val tv = TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(ink)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        currentPage.addView(tv, matchWrap())
    }

    private fun smallText(value: String) {
        val tv = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(18), 0, dp(8))
        }
        currentPage.addView(tv, matchWrap())
    }

    private fun actionButton(label: String, action: () -> Unit) = addStyledButton(label, forest, Color.WHITE, action)
    private fun secondaryButton(label: String, action: () -> Unit) = addStyledButton(label, paleGreen, forestDark, action)
    private fun compactButton(label: String, action: () -> Unit) = addStyledButton(label, paleGreen, forestDark, action, 46)

    private fun addStyledButton(label: String, background: Int, foreground: Int, action: () -> Unit, height: Int = 54) {
        val button = Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(foreground)
            setBackgroundColor(background)
            setOnClickListener { action() }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(height))
        lp.setMargins(0, dp(5), 0, dp(5))
        currentPage.addView(button, lp)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}