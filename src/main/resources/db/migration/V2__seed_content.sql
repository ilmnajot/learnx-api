-- First LearnX course: English for IT professionals. Translations need review by a native Uzbek speaker.
INSERT INTO topics (slug, title, position) VALUES
    ('standup', 'Daily standup', 1),
    ('code-review', 'Code review', 2),
    ('incidents', 'Bugs and incidents', 3),
    ('planning', 'Planning and clients', 4),
    ('writing', 'Written messages', 5),
    ('career', 'Career and interviews', 6);

INSERT INTO lessons (topic_id, slug, title, can_do, scenario, ai_persona, opening_line, position)
SELECT t.id, v.slug, v.title, v.can_do, v.scenario, v.persona, v.opening, v.pos
FROM (VALUES
    ('standup', 'give-update', 'Give your standup update', 'You can give a clear standup update.',
     'The daily standup. The learner shares what they did yesterday and what they will do today.',
     'A friendly team lead running the standup', 'Good morning! What did you work on yesterday, and what is the plan for today?', 1),
    ('standup', 'report-blocker', 'Report a blocker', 'You can report a blocker and ask for what you need.',
     'The learner is blocked because they are waiting on something from another team.',
     'A team lead who wants to remove blockers', 'Is anything blocking you right now?', 2),
    ('standup', 'task-late', 'Tell your manager a task is late', 'You can tell your manager a task is late.',
     'A task will miss today''s release. The learner explains why and gives a new date.',
     'An engineering manager preparing a release', 'Hey, is the payment API ready for today''s release?', 3),
    ('standup', 'ask-help', 'Ask a teammate for help', 'You can ask a teammate for help politely.',
     'The learner has been stuck on a problem for hours and asks a senior developer for help.',
     'A helpful senior developer who is a bit busy', 'Hi! You wanted to ask me something?', 4),
    ('standup', 'estimate-changed', 'Explain why an estimate changed', 'You can explain why an estimate changed.',
     'A task turned out to be bigger than planned. The learner explains what changed.',
     'A project manager tracking the sprint', 'You estimated two days for this task, but it is still open. What happened?', 5),

    ('code-review', 'describe-pr', 'Describe your pull request', 'You can describe your pull request clearly.',
     'The learner asks a colleague to review their pull request and explains what it does.',
     'A colleague about to review the code', 'Sure, I can review it. What does this pull request change?', 6),
    ('code-review', 'suggest-change', 'Suggest a change politely', 'You can suggest a code change politely.',
     'The learner reviewed a teammate''s code and wants to suggest a simpler approach.',
     'A teammate who wrote the code', 'Thanks for reviewing my code. Did you notice anything?', 7),
    ('code-review', 'disagree-comment', 'Disagree with a review comment', 'You can disagree respectfully in a code review.',
     'A reviewer asked for a change that the learner thinks is not necessary.',
     'A senior reviewer open to discussion', 'I left a comment asking you to split this class. Did you see it?', 8),
    ('code-review', 'ask-why', 'Ask why something was done', 'You can ask why code was written a certain way.',
     'The learner found confusing old code and asks the author about it, without sounding critical.',
     'The original author of the code', 'You have a question about the code I wrote last year?', 9),
    ('code-review', 'respond-criticism', 'Respond to criticism', 'You can respond calmly to critical feedback.',
     'A reviewer found a serious problem in the learner''s code. The learner accepts it and proposes next steps.',
     'A direct but fair senior developer', 'This query will be very slow in production. It scans the whole table.', 10),

    ('incidents', 'report-bug', 'Report a bug', 'You can report a bug with clear steps.',
     'The learner found a bug and reports it to the developer responsible for that feature.',
     'A developer who needs precise details', 'You found a bug? Tell me what happened and how I can reproduce it.', 11),
    ('incidents', 'root-cause', 'Explain the root cause', 'You can explain the root cause of a problem.',
     'After an outage, the learner explains to the team what caused it.',
     'A tech lead running a short post-incident review', 'The service is back now. So what was the root cause?', 12),
    ('incidents', 'outage-status', 'Give a status update during an outage', 'You can give a status update during an outage.',
     'Production is down. The learner updates the team on what is known and what comes next.',
     'An incident manager on a call', 'What is the current status? Do we know what is affected?', 13),
    ('incidents', 'fix-deployed', 'Say a fix is deployed', 'You can announce a fix and what to watch next.',
     'The learner has deployed a fix and tells the team how it is being monitored.',
     'A product manager waiting for news', 'Any news on the login problem? Customers keep asking.', 14),
    ('incidents', 'explain-nontechnical', 'Explain an issue to a non-technical person', 'You can explain a technical issue in simple words.',
     'The learner explains a technical problem to someone from the sales team, without jargon.',
     'A sales manager with no technical background', 'A customer asked me why the reports were wrong yesterday. What should I tell them?', 15),

    ('planning', 'clarify-requirement', 'Clarify a requirement', 'You can ask questions to clarify a requirement.',
     'A requirement is unclear, so the learner asks the product manager for details before starting.',
     'A product manager who is happy to explain', 'So you have seen the new export feature. Is everything clear?', 16),
    ('planning', 'push-back-deadline', 'Push back on a deadline', 'You can push back on an unrealistic deadline.',
     'The learner is asked to finish something too quickly and explains the risk politely.',
     'A manager under pressure from a client', 'The client wants this feature by Friday. Can you do it?', 17),
    ('planning', 'give-estimate', 'Give an estimate', 'You can give an estimate and explain your assumptions.',
     'The learner gives a time estimate for a new task, including what could change it.',
     'A project manager planning the next sprint', 'How long do you think the new notification service will take?', 18),
    ('planning', 'explain-tradeoff', 'Explain a trade-off', 'You can explain a technical trade-off.',
     'There are two possible solutions. The learner explains the advantages and costs of each.',
     'A tech lead choosing an approach', 'Should we build this ourselves or use a third-party service?', 19),
    ('planning', 'not-possible-option', 'Say no and offer an option', 'You can say no and offer an alternative.',
     'A client asks for something that is not possible. The learner says so and suggests another way.',
     'A polite but demanding client', 'We would like real-time reports for all our data by next week.', 20),

    ('writing', 'slack-question', 'Ask a clear question on Slack', 'You can ask a clear question in a chat message.',
     'The learner needs an answer from a colleague and describes the question they would write in chat.',
     'A colleague in another team who has little context', 'Hi, I saw you wanted to ask me something about the API?', 21),
    ('writing', 'follow-up', 'Follow up without being rude', 'You can follow up politely.',
     'The learner has not received a reply for three days and follows up politely.',
     'A busy colleague who forgot to answer', 'Sorry, I have been very busy this week. What was this about?', 22),
    ('writing', 'pr-description', 'Explain your PR description', 'You can summarise a change for your team.',
     'The learner summarises what a change does, why it is needed and how it was tested.',
     'A teammate reading the pull request', 'I am reading your pull request. Why do we need this change?', 23),
    ('writing', 'time-off', 'Ask for time off', 'You can ask for time off and plan your handover.',
     'The learner asks a manager for days off and explains who will cover their work.',
     'A supportive manager who needs to plan', 'You wanted to talk about your schedule next month?', 24),
    ('writing', 'client-delay', 'Tell a client about a delay', 'You can tell a client about a delay professionally.',
     'A delivery will be late. The learner informs the client, explains why and gives a new date.',
     'A client who is expecting a delivery', 'Hello! Are we still on track for the delivery this Friday?', 25),

    ('career', 'introduce-yourself', 'Introduce yourself', 'You can introduce yourself professionally.',
     'A job interview starts. The learner briefly introduces themselves and their experience.',
     'A friendly interviewer at a software company', 'Thanks for joining us. Could you tell me a little about yourself?', 26),
    ('career', 'past-project', 'Walk through a past project', 'You can describe a past project and your role.',
     'In an interview, the learner describes a project they worked on and their contribution.',
     'A technical interviewer', 'Tell me about a project you are proud of. What was your role?', 27),
    ('career', 'difficult-bug', 'Tell about a difficult bug', 'You can tell a story about solving a hard problem.',
     'The interviewer asks about a hard bug. The learner describes the problem, the steps and the result.',
     'A senior engineer interviewing a candidate', 'What is the most difficult bug you have ever fixed?', 28),
    ('career', 'dont-know', 'Say "I don''t know" confidently', 'You can admit you don''t know something and stay confident.',
     'The interviewer asks about a technology the learner has not used. The learner answers honestly and explains how they would learn it.',
     'A technical interviewer', 'Have you used Kafka in production? How does partitioning work?', 29),
    ('career', 'discuss-salary', 'Discuss salary', 'You can talk about salary expectations professionally.',
     'At the end of an interview, the learner discusses salary expectations.',
     'A recruiter finishing the interview process', 'We are happy with your interviews. What are your salary expectations?', 30)
) AS v (topic, slug, title, can_do, scenario, persona, opening, pos)
JOIN topics t ON t.slug = v.topic;

INSERT INTO phrases (lesson_id, english, translation, example, position)
SELECT l.id, v.english, v.translation, v.example, v.pos
FROM (VALUES
    ('give-update', 'Yesterday I worked on', 'Kecha men ... ustida ishladim', 'Yesterday I worked on the login page.', 1),
    ('give-update', 'Today I''m going to', 'Bugun men ... qilmoqchiman', 'Today I''m going to write the tests.', 2),
    ('give-update', 'I''m almost done with', 'Men ... ni deyarli tugatdim', 'I''m almost done with the API changes.', 3),

    ('report-blocker', 'I''m blocked by', 'Meni ... toʻxtatib turibdi', 'I''m blocked by the missing API key.', 1),
    ('report-blocker', 'I''m waiting for', 'Men ... ni kutyapman', 'I''m waiting for access to the database.', 2),
    ('report-blocker', 'Could you help me with', '... bilan yordam bera olasizmi?', 'Could you help me with the server access?', 3),

    ('task-late', 'This is taking longer than expected.', 'Bu kutilganidan koʻproq vaqt olyapti.', 'This is taking longer than expected because of the migration.', 1),
    ('task-late', 'I ran into an issue with', '... bilan muammoga duch keldim', 'I ran into an issue with the database.', 2),
    ('task-late', 'I should have it ready by', 'Buni ... gacha tayyorlashim kerak', 'I should have it ready by tomorrow.', 3),

    ('ask-help', 'Do you have a few minutes?', 'Bir necha daqiqa vaqtingiz bormi?', 'Do you have a few minutes to look at this?', 1),
    ('ask-help', 'I''ve been stuck on this for', 'Men bunda ... davomida qotib qoldim', 'I''ve been stuck on this for two hours.', 2),
    ('ask-help', 'I''ve already tried', 'Men allaqachon ... ni sinab koʻrdim', 'I''ve already tried restarting the service.', 3),

    ('estimate-changed', 'It turned out to be more complex.', 'Bu murakkabroq boʻlib chiqdi.', 'It turned out to be more complex than we thought.', 1),
    ('estimate-changed', 'We didn''t take into account', 'Biz ... ni hisobga olmadik', 'We didn''t take into account the old data.', 2),
    ('estimate-changed', 'My new estimate is', 'Mening yangi baholashim ...', 'My new estimate is three more days.', 3),

    ('describe-pr', 'This pull request adds', 'Bu pull request ... ni qoʻshadi', 'This pull request adds email validation.', 1),
    ('describe-pr', 'The main change is in', 'Asosiy oʻzgarish ... da', 'The main change is in the payment service.', 2),
    ('describe-pr', 'I tested it by', 'Men buni ... orqali sinab koʻrdim', 'I tested it by running the full checkout flow.', 3),

    ('suggest-change', 'What do you think about', '... haqida nima deb oʻylaysiz?', 'What do you think about using a map here?', 1),
    ('suggest-change', 'It might be simpler to', '... qilish osonroq boʻlishi mumkin', 'It might be simpler to move this to a helper.', 2),
    ('suggest-change', 'Have you considered', '... ni oʻylab koʻrdingizmi?', 'Have you considered adding an index?', 3),

    ('disagree-comment', 'I see your point, but', 'Fikringizni tushunaman, lekin', 'I see your point, but this class is still small.', 1),
    ('disagree-comment', 'I''m not sure that''s necessary because', 'Bu kerakligiga ishonchim komil emas, chunki', 'I''m not sure that''s necessary because we only call it once.', 2),
    ('disagree-comment', 'Could we discuss this?', 'Buni muhokama qilsak boʻladimi?', 'Could we discuss this on a quick call?', 3),

    ('ask-why', 'I''m trying to understand', 'Men ... ni tushunishga harakat qilyapman', 'I''m trying to understand this retry logic.', 1),
    ('ask-why', 'Was there a reason for', '... uchun sabab bormidi?', 'Was there a reason for using a separate table?', 2),
    ('ask-why', 'Can you walk me through', '... ni tushuntirib bera olasizmi?', 'Can you walk me through this function?', 3),

    ('respond-criticism', 'Good catch, thanks.', 'Yaxshi topdingiz, rahmat.', 'Good catch, thanks. I missed that case.', 1),
    ('respond-criticism', 'You''re right, I''ll fix that.', 'Haqsiz, buni tuzataman.', 'You''re right, I''ll fix that today.', 2),
    ('respond-criticism', 'How would you approach it?', 'Siz bunga qanday yondashgan boʻlardingiz?', 'How would you approach it in this case?', 3),

    ('report-bug', 'Steps to reproduce:', 'Takrorlash bosqichlari:', 'Steps to reproduce: open the page and click save.', 1),
    ('report-bug', 'I expected it to', 'Men uning ... qilishini kutgandim', 'I expected it to save the form.', 2),
    ('report-bug', 'Instead, it', 'Buning oʻrniga u', 'Instead, it shows an error message.', 3),

    ('root-cause', 'The root cause was', 'Asosiy sabab ... edi', 'The root cause was a missing database index.', 1),
    ('root-cause', 'This happened because', 'Bu ... sababli yuz berdi', 'This happened because the cache expired.', 2),
    ('root-cause', 'To prevent this, we will', 'Buning oldini olish uchun biz ... qilamiz', 'To prevent this, we will add an alert.', 3),

    ('outage-status', 'We''re looking into it.', 'Biz buni tekshiryapmiz.', 'We''re looking into it right now.', 1),
    ('outage-status', 'So far we know that', 'Hozircha bizga maʼlumki', 'So far we know that only logins are affected.', 2),
    ('outage-status', 'The next update will be in', 'Keyingi xabar ... dan keyin boʻladi', 'The next update will be in fifteen minutes.', 3),

    ('fix-deployed', 'The fix is live.', 'Tuzatish ishga tushirildi.', 'The fix is live in production.', 1),
    ('fix-deployed', 'We''re monitoring it closely.', 'Biz buni diqqat bilan kuzatyapmiz.', 'We''re monitoring it closely for the next hour.', 2),
    ('fix-deployed', 'Let me know if you still see', 'Agar hali ham ... ni koʻrsangiz, menga xabar bering', 'Let me know if you still see the error.', 3),

    ('explain-nontechnical', 'In simple terms,', 'Oddiy qilib aytganda,', 'In simple terms, the system used old data.', 1),
    ('explain-nontechnical', 'This means that', 'Bu shuni anglatadiki', 'This means that some numbers were wrong.', 2),
    ('explain-nontechnical', 'It won''t affect', 'Bu ... ga taʼsir qilmaydi', 'It won''t affect your account.', 3),

    ('clarify-requirement', 'Just to clarify,', 'Aniqlashtirib olsam,', 'Just to clarify, is this for admins only?', 1),
    ('clarify-requirement', 'What should happen if', 'Agar ... boʻlsa, nima boʻlishi kerak?', 'What should happen if the file is empty?', 2),
    ('clarify-requirement', 'Do I understand correctly that', 'Toʻgʻri tushundimmi,', 'Do I understand correctly that it runs daily?', 3),

    ('push-back-deadline', 'I''m concerned about the deadline.', 'Muddat meni xavotirga solyapti.', 'I''m concerned about the deadline because testing takes time.', 1),
    ('push-back-deadline', 'To do it properly, we need', 'Buni toʻgʻri qilish uchun bizga ... kerak', 'To do it properly, we need one more week.', 2),
    ('push-back-deadline', 'What we can deliver by then is', 'Shu vaqtgacha yetkaza oladiganimiz', 'What we can deliver by then is the first version.', 3),

    ('give-estimate', 'I''d estimate around', 'Taxminan ... deb baholagan boʻlardim', 'I''d estimate around five days.', 1),
    ('give-estimate', 'That depends on', 'Bu ... ga bogʻliq', 'That depends on the design.', 2),
    ('give-estimate', 'Assuming that', '... deb hisoblasak', 'Assuming that the API is ready, it will take a week.', 3),

    ('explain-tradeoff', 'The advantage is that', 'Afzalligi shundaki', 'The advantage is that it is faster to build.', 1),
    ('explain-tradeoff', 'The downside is that', 'Kamchiligi shundaki', 'The downside is that it costs more every month.', 2),
    ('explain-tradeoff', 'I would recommend', 'Men ... ni tavsiya qilgan boʻlardim', 'I would recommend starting with the simple option.', 3),

    ('not-possible-option', 'Unfortunately, that isn''t possible because', 'Afsuski, bu mumkin emas, chunki', 'Unfortunately, that isn''t possible because of the data size.', 1),
    ('not-possible-option', 'What we could do instead is', 'Buning oʻrniga ... qilishimiz mumkin', 'What we could do instead is update it every hour.', 2),
    ('not-possible-option', 'Would that work for you?', 'Bu sizga toʻgʻri keladimi?', 'Would that work for you and your team?', 3),

    ('slack-question', 'Quick question about', '... haqida qisqa savol', 'Quick question about the deployment script.', 1),
    ('slack-question', 'Here''s the context:', 'Mana vaziyat:', 'Here''s the context: the build fails on staging.', 2),
    ('slack-question', 'No rush.', 'Shoshilinch emas.', 'No rush. Any time today is fine.', 3),

    ('follow-up', 'Just following up on', '... boʻyicha eslatib qoʻymoqchiman', 'Just following up on my question from Monday.', 1),
    ('follow-up', 'Any update on', '... boʻyicha yangilik bormi?', 'Any update on the access request?', 2),
    ('follow-up', 'Is there anything I can do to help?', 'Yordam bera oladigan biror narsam bormi?', 'Is there anything I can do to help move this forward?', 3),

    ('pr-description', 'This change fixes', 'Bu oʻzgarish ... ni tuzatadi', 'This change fixes the timeout on the report page.', 1),
    ('pr-description', 'We need this because', 'Bu bizga ... uchun kerak', 'We need this because customers are seeing errors.', 2),
    ('pr-description', 'It was tested on', 'Bu ... da sinab koʻrildi', 'It was tested on staging with real data.', 3),

    ('time-off', 'I''d like to take a few days off.', 'Bir necha kun dam olmoqchiman.', 'I''d like to take a few days off next month.', 1),
    ('time-off', 'Would that be okay?', 'Bu maqulmi?', 'Would that be okay with the release plan?', 2),
    ('time-off', 'will cover for me', 'mening ishimni bajarib turadi', 'Aziz will cover for me while I''m away.', 3),

    ('client-delay', 'I wanted to let you know that', 'Sizga shuni bildirmoqchi edim:', 'I wanted to let you know that the delivery will be late.', 1),
    ('client-delay', 'We now expect to deliver by', 'Endi ... gacha yetkazishni kutyapmiz', 'We now expect to deliver by next Wednesday.', 2),
    ('client-delay', 'I apologise for the inconvenience.', 'Noqulaylik uchun uzr soʻrayman.', 'I apologise for the inconvenience this causes.', 3),

    ('introduce-yourself', 'I''m a backend developer with', 'Men ... tajribaga ega backend dasturchiman', 'I''m a backend developer with four years of experience.', 1),
    ('introduce-yourself', 'I mainly work with', 'Men asosan ... bilan ishlayman', 'I mainly work with Java and PostgreSQL.', 2),
    ('introduce-yourself', 'Most recently, I', 'Yaqinda men', 'Most recently, I built a payment system.', 3),

    ('past-project', 'I was responsible for', 'Men ... uchun javobgar edim', 'I was responsible for the search feature.', 1),
    ('past-project', 'The biggest challenge was', 'Eng katta qiyinchilik ... edi', 'The biggest challenge was the amount of data.', 2),
    ('past-project', 'As a result,', 'Natijada,', 'As a result, the page became ten times faster.', 3),

    ('difficult-bug', 'At first, I thought', 'Avvaliga men ... deb oʻyladim', 'At first, I thought it was a network problem.', 1),
    ('difficult-bug', 'I narrowed it down to', 'Men muammoni ... gacha aniqladim', 'I narrowed it down to one database query.', 2),
    ('difficult-bug', 'It turned out that', 'Maʼlum boʻlishicha,', 'It turned out that two threads shared one object.', 3),

    ('dont-know', 'I haven''t worked with that yet.', 'Men hali u bilan ishlamaganman.', 'I haven''t worked with that yet, but I use RabbitMQ.', 1),
    ('dont-know', 'I''m not sure, but I would guess', 'Aniq bilmayman, lekin taxminimcha', 'I''m not sure, but I would guess it uses a key.', 2),
    ('dont-know', 'I would start by', 'Men ... dan boshlagan boʻlardim', 'I would start by reading the documentation.', 3),

    ('discuss-salary', 'I''m looking for something around', 'Men taxminan ... atrofida kutyapman', 'I''m looking for something around this range.', 1),
    ('discuss-salary', 'Based on my experience,', 'Tajribamdan kelib chiqib,', 'Based on my experience, that seems fair.', 2),
    ('discuss-salary', 'Is there any flexibility?', 'Biror moslashuvchanlik bormi?', 'Is there any flexibility on the offer?', 3)
) AS v (lesson, english, translation, example, pos)
JOIN lessons l ON l.slug = v.lesson;

INSERT INTO placement_questions (prompt, options, correct_index, weight, position) VALUES
    ('She ___ a developer.', ARRAY['are', 'is', 'be', 'am'], 1, 1, 1),
    ('We ___ a meeting every Monday.', ARRAY['has', 'is have', 'having', 'have'], 3, 1, 2),
    ('The server ___ working right now.', ARRAY['isn''t', 'not is', 'don''t', 'doesn''t'], 0, 1, 3),
    ('I ___ this bug yesterday.', ARRAY['fix', 'fixes', 'fixed', 'fixing'], 2, 1, 4),
    ('I have ___ problem with the database.', ARRAY['an', 'the some', 'a', 'any'], 2, 1, 5),
    ('The tests ___ when I arrived this morning.', ARRAY['were running', 'run', 'are run', 'runs'], 0, 2, 6),
    ('We ___ this feature since January.', ARRAY['build', 'are building', 'built', 'have been building'], 3, 2, 7),
    ('If the build fails, we ___ the release.', ARRAY['would delay', 'will delay', 'delaying', 'delayed'], 1, 2, 8),
    ('This depends ___ the client''s decision.', ARRAY['of', 'from', 'on', 'about'], 2, 2, 9),
    ('The bug ___ by the QA team last week.', ARRAY['found', 'was found', 'has find', 'finding'], 1, 2, 10),
    ('If we had added an index, the query ___ so slow.', ARRAY['wouldn''t have been', 'isn''t', 'won''t be', 'hasn''t been'], 0, 3, 11),
    ('Choose the most polite way to disagree in a code review.', ARRAY['You are wrong.', 'This is bad code.', 'I see your point, but I think this may be slower.', 'Change this.'], 2, 3, 12);
