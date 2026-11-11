-- Second LearnX course: the Arabic alphabet. Each letter is a lesson, so progress, review, streaks and squads work
-- unchanged. Hints and meanings need review by a native Uzbek speaker, and sounds by an Arabic teacher.

ALTER TABLE users ADD COLUMN course text NOT NULL DEFAULT 'ENGLISH' CHECK (course IN ('ENGLISH', 'ARABIC_LETTERS'));
ALTER TABLE topics ADD COLUMN course text NOT NULL DEFAULT 'ENGLISH' CHECK (course IN ('ENGLISH', 'ARABIC_LETTERS'));

CREATE TABLE letters (
    lesson_id     uuid PRIMARY KEY REFERENCES lessons (id) ON DELETE CASCADE,
    glyph         text NOT NULL UNIQUE,
    name          text NOT NULL,
    latin         text NOT NULL,
    hint          text NOT NULL,
    -- The letter's shape alone, at the start, in the middle and at the end of a word. Six letters never join the
    -- letter after them, so their start and middle shapes repeat the alone and end shapes.
    isolated_form text NOT NULL,
    initial_form  text NOT NULL,
    medial_form   text NOT NULL,
    final_form    text NOT NULL,
    -- What speech synthesis reads aloud: the letter with a short "a" vowel, as in "ba".
    say           text NOT NULL,
    word          text NOT NULL,
    word_latin    text NOT NULL,
    word_meaning  text NOT NULL,
    -- Letters learners confuse with this one, by shape or by sound. Quizzes offer them as wrong answers.
    -- "similar" is reserved in PostgreSQL: queries must quote it.
    "similar"       text[]  DEFAULT '{}',
    -- A native recording. Until one exists, the app uses the device's speech synthesis.
    audio_url     text
);

INSERT INTO topics (slug, title, position, course) VALUES
    ('arabic-1', 'Harflar 1: ا ب ت ث', 1001, 'ARABIC_LETTERS'),
    ('arabic-2', 'Harflar 2: ج ح خ د ذ ر ز', 1002, 'ARABIC_LETTERS'),
    ('arabic-3', 'Harflar 3: س ش ص ض ط ظ', 1003, 'ARABIC_LETTERS'),
    ('arabic-4', 'Harflar 4: ع غ ف ق ك ل', 1004, 'ARABIC_LETTERS'),
    ('arabic-5', 'Harflar 5: م ن ه و ي', 1005, 'ARABIC_LETTERS');

CREATE TEMPORARY TABLE letter_seed (
    pos int, topic text, glyph text, name text, latin text, joins boolean, say text, hint text,
    word text, word_latin text, word_meaning text, "similar" text[]
) ;

INSERT INTO letter_seed VALUES
    (1, 'arabic-1', 'ا', 'alif', 'a', false, 'أَ', 'Uzun «a» tovushi. Soʻz boshida hamza (أ) bilan yoziladi.', 'أسد', 'asad', 'sher', '{ل}'),
    (2, 'arabic-1', 'ب', 'bāʼ', 'b', true, 'بَ', 'Oʻzbekcha «b» kabi.', 'باب', 'bāb', 'eshik', '{ت,ث,ن,ي}'),
    (3, 'arabic-1', 'ت', 'tāʼ', 't', true, 'تَ', 'Oʻzbekcha «t» kabi.', 'تمر', 'tamr', 'xurmo', '{ب,ث,ط}'),
    (4, 'arabic-1', 'ث', 'thāʼ', 'th', true, 'ثَ', 'Til uchi tishlar orasida: inglizcha «think» soʻzidagi «th».', 'ثوب', 'thawb', 'kiyim', '{ت,س,ب}'),
    (5, 'arabic-2', 'ج', 'jīm', 'j', true, 'جَ', 'Oʻzbekcha «j» kabi, «joʻja» soʻzidagidek.', 'جمل', 'jamal', 'tuya', '{ح,خ}'),
    (6, 'arabic-2', 'ح', 'ḥāʼ', 'ḥ', true, 'حَ', 'Boʻgʻizdan chiqadigan kuchli, xirillamaydigan «h».', 'حليب', 'ḥalīb', 'sut', '{ه,خ,ج}'),
    (7, 'arabic-2', 'خ', 'khāʼ', 'kh', true, 'خَ', 'Oʻzbekcha «x» kabi, «xat» soʻzidagidek.', 'خبز', 'khubz', 'non', '{ح,ج,غ}'),
    (8, 'arabic-2', 'د', 'dāl', 'd', false, 'دَ', 'Oʻzbekcha «d» kabi.', 'درس', 'dars', 'dars', '{ذ,ض}'),
    (9, 'arabic-2', 'ذ', 'dhāl', 'dh', false, 'ذَ', 'Inglizcha «this» soʻzidagi «th» kabi.', 'ذهب', 'dhahab', 'oltin', '{د,ز,ظ}'),
    (10, 'arabic-2', 'ر', 'rāʼ', 'r', false, 'رَ', 'Oʻzbekcha «r» kabi.', 'رجل', 'rajul', 'erkak', '{ز,و}'),
    (11, 'arabic-2', 'ز', 'zāy', 'z', false, 'زَ', 'Oʻzbekcha «z» kabi.', 'زيت', 'zayt', 'moy', '{ر,ذ,ظ}'),
    (12, 'arabic-3', 'س', 'sīn', 's', true, 'سَ', 'Oʻzbekcha «s» kabi.', 'سمك', 'samak', 'baliq', '{ش,ص,ث}'),
    (13, 'arabic-3', 'ش', 'shīn', 'sh', true, 'شَ', 'Oʻzbekcha «sh» kabi.', 'شمس', 'shams', 'quyosh', '{س}'),
    (14, 'arabic-3', 'ص', 'ṣād', 'ṣ', true, 'صَ', 'Qalin, ogʻir «s»: til orqasi koʻtariladi.', 'صبر', 'ṣabr', 'sabr', '{س,ض}'),
    (15, 'arabic-3', 'ض', 'ḍād', 'ḍ', true, 'ضَ', 'Qalin, ogʻir «d».', 'ضيف', 'ḍayf', 'mehmon', '{ص,د,ظ}'),
    (16, 'arabic-3', 'ط', 'ṭāʼ', 'ṭ', true, 'طَ', 'Qalin, ogʻir «t».', 'طالب', 'ṭālib', 'talaba', '{ت,ظ}'),
    (17, 'arabic-3', 'ظ', 'ẓāʼ', 'ẓ', true, 'ظَ', 'Qalin, ogʻir «dh»: til uchi tishlar orasida.', 'ظل', 'ẓill', 'soya', '{ط,ذ,ض,ز}'),
    (18, 'arabic-4', 'ع', 'ʿayn', 'ʿ', true, 'عَ', 'Boʻgʻizni siqib chiqariladigan tovush. Oʻzbek tilida yoʻq.', 'عين', 'ʿayn', 'koʻz', '{غ,ح}'),
    (19, 'arabic-4', 'غ', 'ghayn', 'gh', true, 'غَ', 'Oʻzbekcha «gʻ» kabi, «gʻoz» soʻzidagidek.', 'غرفة', 'ghurfa', 'xona', '{ع,خ}'),
    (20, 'arabic-4', 'ف', 'fāʼ', 'f', true, 'فَ', 'Oʻzbekcha «f» kabi.', 'فيل', 'fīl', 'fil', '{ق}'),
    (21, 'arabic-4', 'ق', 'qāf', 'q', true, 'قَ', 'Oʻzbekcha «q» kabi, «qalam» soʻzidagidek.', 'قلم', 'qalam', 'qalam', '{ك,ف}'),
    (22, 'arabic-4', 'ك', 'kāf', 'k', true, 'كَ', 'Oʻzbekcha «k» kabi.', 'كتاب', 'kitāb', 'kitob', '{ق}'),
    (23, 'arabic-4', 'ل', 'lām', 'l', true, 'لَ', 'Oʻzbekcha «l» kabi.', 'ليمون', 'laymūn', 'limon', '{ا,ك}'),
    (24, 'arabic-5', 'م', 'mīm', 'm', true, 'مَ', 'Oʻzbekcha «m» kabi.', 'ماء', 'māʼ', 'suv', '{ن}'),
    (25, 'arabic-5', 'ن', 'nūn', 'n', true, 'نَ', 'Oʻzbekcha «n» kabi.', 'نور', 'nūr', 'nur', '{ب,ت,ي}'),
    (26, 'arabic-5', 'ه', 'hāʼ', 'h', true, 'هَ', 'Yengil, nafasli «h», inglizcha «hat» soʻzidagidek.', 'هلال', 'hilāl', 'hilol', '{ح}'),
    (27, 'arabic-5', 'و', 'wāw', 'w / ū', false, 'وَ', 'Inglizcha «w» yoki choʻziq «u».', 'ورد', 'ward', 'atirgul', '{ر}'),
    (28, 'arabic-5', 'ي', 'yāʼ', 'y / ī', true, 'يَ', 'Oʻzbekcha «y» yoki choʻziq «i».', 'يد', 'yad', 'qoʻl', '{ب,ن,ت}');

INSERT INTO lessons (topic_id, slug, title, can_do, scenario, ai_persona, opening_line, position, est_minutes)
SELECT t.id, 'arabic-letter-' || s.pos, s.glyph || ' — ' || s.name,
       '«' || s.glyph || '» harfini taniysiz va uning tovushini bilasiz.', '', '', '', 1000 + s.pos, 3
FROM letter_seed s JOIN topics t ON t.slug = s.topic;

INSERT INTO letters (lesson_id, glyph, name, latin, hint, isolated_form, initial_form, medial_form, final_form, say,
                     word, word_latin, word_meaning, "similar")
SELECT l.id, s.glyph, s.name, s.latin, s.hint,
       s.glyph,
       CASE WHEN s.joins THEN s.glyph || 'ـ' ELSE s.glyph END,
       CASE WHEN s.joins THEN 'ـ' || s.glyph || 'ـ' ELSE 'ـ' || s.glyph END,
       'ـ' || s.glyph,
       s.say, s.word, s.word_latin, s.word_meaning, s.similar
FROM letter_seed s JOIN lessons l ON l.slug = 'arabic-letter-' || s.pos;

-- One review card per letter: the shape on the front, its name and sound on the back.
INSERT INTO phrases (lesson_id, english, translation, example, position)
SELECT l.id, s.glyph, s.name || ' — «' || s.latin || '»', s.word || ' (' || s.word_latin || ') — ' || s.word_meaning, 1
FROM letter_seed s JOIN lessons l ON l.slug = 'arabic-letter-' || s.pos;
