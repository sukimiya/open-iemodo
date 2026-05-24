-- Update USER_REGISTERED templates to include email verification link
UPDATE notification_templates
SET body = '<h2>Hello, {{userName}}!</h2><p>Thank you for joining iemodo. Please verify your email address by clicking the link below:</p><p><a href="{{verifyLink}}">Verify Email Address</a></p><p>Start your cross-border shopping journey today!</p>',
    update_time = NOW()
WHERE type = 'USER_REGISTERED' AND channel = 'EMAIL' AND language = 'en';

UPDATE notification_templates
SET body = '<h2>你好，{{userName}}！</h2><p>感谢您注册 iemodo。请点击下方链接验证您的邮箱地址：</p><p><a href="{{verifyLink}}">验证邮箱</a></p><p>开始您的跨境购物之旅吧！</p>',
    update_time = NOW()
WHERE type = 'USER_REGISTERED' AND channel = 'EMAIL' AND language = 'zh-CN';
