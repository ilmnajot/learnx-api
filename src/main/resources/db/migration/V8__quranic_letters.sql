-- The alphabet course teaches letters the way a Quran school does: each hint describes the letter's makhraj (where it
-- is pronounced), and each example is a word from the Quran with full vowel marks. A Quran teacher should review the
-- hints and examples before learners rely on them.
--
-- Recordings: when a qari has recorded a letter, put the file in learnx-ui/public/audio/letters/ named by the letter's
-- position (01.mp3 for alif … 28.mp3 for yāʼ) and run, for example:
--   UPDATE letters SET audio_url = '/audio/letters/02.mp3' WHERE glyph = 'ب';
-- The app then plays the recording instead of the device's voice.

CREATE TEMPORARY TABLE quranic_seed (glyph text, hint text, word text, word_latin text, word_meaning text);

INSERT INTO quranic_seed VALUES
    ('ا', 'Alif oʻzi choʻziq «ā» boʻlib, ogʻiz va boʻgʻiz boʻshligʻidan (jawf) chiqadi. Hamza (أ) esa boʻgʻizning eng chuqur qismidan.', 'أَحَدٌ', 'aḥadun', 'yagona (Ixlos, 1)'),
    ('ب', 'Ikki lab yumilib, soʻng ochilib aytiladi.', 'بِسْمِ', 'bismi', 'nomi bilan (Fotiha, 1)'),
    ('ت', 'Til uchi yuqori old tishlarning ildiziga tegadi. Ingichka.', 'تَبَّتْ', 'tabbat', 'halok boʻldi (Masad, 1)'),
    ('ث', 'Til uchi yuqori old tishlarning uchiga tegadi, havo sizib chiqadi.', 'ثُمَّ', 'thumma', 'soʻngra (Takosur, 4)'),
    ('ج', 'Tilning oʻrtasi tanglayning oʻrtasiga tegadi.', 'جَآءَ', 'jāʼa', 'keldi (Nasr, 1)'),
    ('ح', 'Boʻgʻizning oʻrtasidan, xirillamay, kuchli nafas bilan chiqadi.', 'ٱلْحَمْدُ', 'al-ḥamdu', 'hamd, maqtov (Fotiha, 2)'),
    ('خ', 'Boʻgʻizning eng yuqori qismidan (ogʻizga yaqin) chiqadi. Qalin.', 'خَلَقَ', 'khalaqa', 'yaratdi (Alaq, 1)'),
    ('د', 'Til uchi yuqori old tishlarning ildiziga tegadi (ت bilan bir joydan, lekin ovozli).', 'دِينِ', 'dīni', 'dinim (Kofirun, 6)'),
    ('ذ', 'Til uchi yuqori old tishlarning uchiga tegadi (ث bilan bir joydan, lekin ovozli).', 'ذَٰلِكَ', 'dhālika', 'ana u (Baqara, 2)'),
    ('ر', 'Til uchi yuqori tish milkiga yaqin tanglayga tegadi. Koʻpincha qalin aytiladi.', 'رَبِّ', 'rabbi', 'Rabbi (Fotiha, 2)'),
    ('ز', 'Til uchi pastki old tishlarga yaqin turadi, hushtakli va ovozli.', 'زُلْزِلَتِ', 'zulzilati', 'larzaga keltirildi (Zalzala, 1)'),
    ('س', 'Til uchi pastki old tishlarga yaqin turadi, hushtakli va ovozsiz.', 'سَلَٰمٌ', 'salāmun', 'tinchlik (Qadr, 5)'),
    ('ش', 'Tilning oʻrtasi tanglayga yaqinlashadi, havo ogʻiz boʻylab yoyiladi.', 'شَرِّ', 'sharri', 'yomonligi (Falaq, 2)'),
    ('ص', 'س bilan bir joydan, lekin til orqasi koʻtariladi. Qalin.', 'ٱلصَّمَدُ', 'aṣ-ṣamadu', 'behojat (Ixlos, 2)'),
    ('ض', 'Tilning yon cheti yuqori oziq tishlarga tegadi. Qalin; boshqa tillarda deyarli yoʻq.', 'ٱلضَّآلِّينَ', 'aḍ-ḍāllīn', 'adashganlar (Fotiha, 7)'),
    ('ط', 'ت bilan bir joydan, lekin qalin va kuchli.', 'طَيْرًا', 'ṭayran', 'qushlar (Fil, 3)'),
    ('ظ', 'ذ bilan bir joydan, lekin qalin.', 'ظُلُمَٰتٍ', 'ẓulumātin', 'zulmatlar (Baqara, 17)'),
    ('ع', 'Boʻgʻizning oʻrtasidan, boʻgʻiz siqilib chiqadi.', 'عَلَقٍ', 'ʿalaqin', 'alaqa (Alaq, 2)'),
    ('غ', 'Boʻgʻizning eng yuqori qismidan, «gʻ» kabi. Qalin.', 'غَيْرِ', 'ghayri', 'emas, boshqa (Fotiha, 7)'),
    ('ف', 'Pastki labning ichi yuqori old tishlarning uchiga tegadi.', 'فَصَلِّ', 'faṣalli', 'namoz oʻqing (Kavsar, 2)'),
    ('ق', 'Til ildizi tilchaga yaqin yumshoq tanglayga tegadi. Qalin.', 'قُلْ', 'qul', 'ayting (Ixlos, 1)'),
    ('ك', 'Til ildizi ق dan biroz oldinroq tanglayga tegadi. Ingichka.', 'كُفُوًا', 'kufuwan', 'teng (Ixlos, 4)'),
    ('ل', 'Til uchining chetlari yuqori tish milkiga tegadi.', 'لَمْ', 'lam', 'yoʻq, emas (Ixlos, 3)'),
    ('م', 'Ikki lab yumiladi, ovoz burundan ham chiqadi.', 'مَٰلِكِ', 'māliki', 'egasi (Fotiha, 4)'),
    ('ن', 'Til uchi yuqori tish milkiga tegadi, ovoz burundan ham chiqadi.', 'نَصْرُ', 'naṣru', 'yordam (Nasr, 1)'),
    ('ه', 'Boʻgʻizning eng chuqur qismidan, yengil nafas bilan.', 'هُوَ', 'huwa', 'U (Ixlos, 1)'),
    ('و', 'Ikki lab dumaloqlanadi, lekin toʻliq yumilmaydi.', 'وَٱلْعَصْرِ', 'wal-ʿaṣri', 'asrga qasam (Asr, 1)'),
    ('ي', 'Tilning oʻrtasi tanglayning oʻrtasiga yaqinlashadi.', 'يَوْمِ', 'yawmi', 'kuni (Fotiha, 4)');

UPDATE letters l
SET hint = s.hint, word = s.word, word_latin = s.word_latin, word_meaning = s.word_meaning
FROM quranic_seed s
WHERE l.glyph = s.glyph;

-- Review cards repeat the example, so they follow.
UPDATE phrases p
SET translation = l.name || ' — «' || l.latin || '»',
    example = l.word || ' (' || l.word_latin || ') — ' || l.word_meaning
FROM letters l
WHERE p.lesson_id = l.lesson_id;

UPDATE lessons l
SET can_do = '«' || a.glyph || '» harfini taniysiz va uni maxrajidan toʻgʻri aytasiz.'
FROM letters a
WHERE a.lesson_id = l.id;
