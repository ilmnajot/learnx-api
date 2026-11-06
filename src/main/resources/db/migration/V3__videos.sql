-- Short videos, played only through YouTube's official embedded player. Nothing is downloaded or re-hosted.
CREATE TABLE videos (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug              text NOT NULL UNIQUE,
    youtube_id        text NOT NULL UNIQUE,
    title             text NOT NULL,
    channel           text NOT NULL,
    category          text NOT NULL CHECK (category IN ('COMEDY', 'INSPIRATION', 'SPEAKING')),
    duration_seconds  integer NOT NULL CHECK (duration_seconds > 0),
    why_watch         text NOT NULL,
    speak_prompt      text NOT NULL,
    position          integer NOT NULL UNIQUE
);

CREATE TABLE video_progress (
    user_id       uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    video_id      uuid NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    completed_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, video_id)
);

-- A phrase now belongs to either a lesson or a video, so reviews and games work for both.
ALTER TABLE phrases ALTER COLUMN lesson_id DROP NOT NULL;
ALTER TABLE phrases ADD COLUMN video_id uuid REFERENCES videos (id) ON DELETE CASCADE;
ALTER TABLE phrases ADD CONSTRAINT phrases_one_owner CHECK ((lesson_id IS NULL) <> (video_id IS NULL));
ALTER TABLE phrases ADD CONSTRAINT phrases_video_position UNIQUE (video_id, position);

INSERT INTO videos (slug, youtube_id, title, channel, category, duration_seconds, why_watch, speak_prompt, position) VALUES
    ('the-expert', 'BKorP55Aqvg', 'The Expert', 'Lauris Beinerts', 'COMEDY', 454,
     'Mijoz imkonsiz narsani soʻragan yigʻilish haqida kulgili sahna. Har bir dasturchiga tanish vaziyat.',
     'Bu yigʻilishda nima notoʻgʻri edi? Inglizcha bir-ikki gap bilan ayting.', 1),
    ('microservices', 'y8OnoxKotPQ', 'Microservices', 'KRAZAM', 'COMEDY', 189,
     'Oddiy oʻzgarish nega juda murakkablashib ketishi haqida qisqa hazil.',
     'Videoda oddiy vazifa nega qiyin boʻlib qoldi? Inglizcha ayting.', 2),
    ('conference-call', 'DYu_bGbZiiQ', 'A Conference Call in Real Life', 'Tripp and Tyler', 'COMEDY', 205,
     'Onlayn qoʻngʻiroqlar haqiqiy hayotda qanday koʻrinishi haqida. Hozir ishlatadigan iboralaringizni eshitasiz.',
     'Onlayn qoʻngʻiroqlarda sizni nima eng koʻp bezovta qiladi? Inglizcha ayting.', 3),
    ('sound-smart', '8S0FDjFBj8o', 'How to sound smart in your TEDx Talk', 'TEDx Talks', 'COMEDY', 356,
     'Nutqlar qanday qilib aqlli eshitilishi haqida kulgili chiqish. Ohang va pauzaga eʼtibor bering.',
     'Yaxshi nutq uchun qaysi usul sizga eng foydali tuyuldi? Inglizcha ayting.', 4),
    ('thirty-days', 'UNP03fDSj1U', 'Try something new for 30 days', 'Matt Cutts, TED-Ed', 'INSPIRATION', 207,
     'Kichik odatlar qanday katta oʻzgarish berishi haqida. Kunlik 5 daqiqa nega ishlashini tushunasiz.',
     'Keyingi 30 kun ichida nimani sinab koʻrmoqchisiz? Inglizcha ayting.', 5),
    ('grit', 'H14bBuluwB8', 'Grit: The Power of Passion and Perseverance', 'Angela Lee Duckworth, TED', 'INSPIRATION', 373,
     'Muvaffaqiyatda iqtidordan tashqari nima muhimligi haqida. Charchagan kunlaringiz uchun.',
     'Qiyin boʻlganda sizni nima davom ettiradi? Inglizcha ayting.', 6),
    ('speak-listen', 'eIho2S0ZahI', 'How to Speak So That People Want to Listen', 'Julian Treasure, TED', 'SPEAKING', 598,
     'Odamlar sizni tinglashi uchun ovozdan qanday foydalanish haqida.',
     'Bundan keyin gapirganda nimani oʻzgartirmoqchisiz? Inglizcha ayting.', 7);

INSERT INTO phrases (video_id, english, translation, example, position)
SELECT v.id, p.english, p.translation, p.example, p.pos
FROM (VALUES
    ('the-expert', 'That isn''t technically possible.', 'Bu texnik jihatdan mumkin emas.', 'That isn''t technically possible, but we have another option.', 1),
    ('the-expert', 'Let me make sure I understand.', 'Toʻgʻri tushunganimga ishonch hosil qilay.', 'Let me make sure I understand what you need.', 2),
    ('the-expert', 'Could you clarify what you mean?', 'Nimani nazarda tutganingizni aniqlashtira olasizmi?', 'Could you clarify what you mean by real time?', 3),

    ('microservices', 'It depends on another service.', 'Bu boshqa servisga bogʻliq.', 'It depends on another service owned by a different team.', 1),
    ('microservices', 'The request goes through several services.', 'Soʻrov bir nechta servisdan oʻtadi.', 'The request goes through several services before it returns.', 2),
    ('microservices', 'This is more complicated than it looks.', 'Bu koʻringanidan murakkabroq.', 'This is more complicated than it looks, so I need more time.', 3),

    ('conference-call', 'Sorry, you''re breaking up.', 'Kechirasiz, ovozingiz uzilib qolyapti.', 'Sorry, you''re breaking up. Could you say that again?', 1),
    ('conference-call', 'Can everyone see my screen?', 'Hamma ekranimni koʻryaptimi?', 'Can everyone see my screen before I start?', 2),
    ('conference-call', 'Sorry, I was on mute.', 'Kechirasiz, mikrofonim oʻchiq ekan.', 'Sorry, I was on mute. I said the fix is ready.', 3),

    ('sound-smart', 'Let me start with a question.', 'Bir savoldan boshlay.', 'Let me start with a question about our users.', 1),
    ('sound-smart', 'The key point is', 'Asosiy fikr shundaki', 'The key point is that we need better tests.', 2),
    ('sound-smart', 'To sum up,', 'Xulosa qilib aytganda,', 'To sum up, the new version is faster.', 3),

    ('thirty-days', 'I decided to try', 'Men ... ni sinab koʻrishga qaror qildim', 'I decided to try speaking English every day.', 1),
    ('thirty-days', 'It became a habit.', 'Bu odatga aylandi.', 'After a month, it became a habit.', 2),
    ('thirty-days', 'Small changes add up.', 'Kichik oʻzgarishlar jamlanib kattalashadi.', 'Small changes add up over a year.', 3),

    ('grit', 'I won''t give up.', 'Men taslim boʻlmayman.', 'This is hard, but I won''t give up.', 1),
    ('grit', 'Practice makes a difference.', 'Mashq farq qiladi.', 'Practice makes a difference, even five minutes a day.', 2),
    ('grit', 'Keep going even when it''s hard.', 'Qiyin boʻlsa ham davom eting.', 'You have to keep going even when it''s hard.', 3),

    ('speak-listen', 'Could you speak more slowly?', 'Sekinroq gapira olasizmi?', 'Could you speak more slowly, please?', 1),
    ('speak-listen', 'Let me say that another way.', 'Buni boshqacha aytib beray.', 'Let me say that another way, with an example.', 2),
    ('speak-listen', 'I''d like to hear your opinion.', 'Fikringizni eshitmoqchiman.', 'I''d like to hear your opinion on this design.', 3)
) AS p (video, english, translation, example, pos)
JOIN videos v ON v.slug = p.video;
