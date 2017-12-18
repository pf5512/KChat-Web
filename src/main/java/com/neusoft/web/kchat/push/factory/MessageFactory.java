package com.neusoft.web.kchat.push.factory;

import com.neusoft.web.kchat.push.bean.api.message.MessageCreateModel;
import com.neusoft.web.kchat.push.bean.db.Group;
import com.neusoft.web.kchat.push.bean.db.Message;
import com.neusoft.web.kchat.push.bean.db.User;
import com.neusoft.web.kchat.push.utils.Hib;

/**
 * 消息数据存储的类
 */
public class MessageFactory {
    // 查询某一个消息
    public static Message findById(String id) {
        return Hib.query(session -> session.get(Message.class, id));
    }

    // 添加一条普通消息
    public static Message add(User sender, User receiver, MessageCreateModel model) {
        Message message = new Message(sender, receiver, model);
        return save(message);
    }

    // 添加一条群消息
    public static Message add(User sender, Group group, MessageCreateModel model) {
        Message message = new Message(sender, group, model);
        return save(message);
    }

    private static Message save(Message message) {
        return Hib.query(session -> {
            session.save(message);

            // 写入到数据库
            session.flush();

            // 紧接着从数据库中查询出来
            session.refresh(message);
            return message;
        });
    }

}
