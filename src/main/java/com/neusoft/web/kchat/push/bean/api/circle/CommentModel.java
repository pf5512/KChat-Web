package com.neusoft.web.kchat.push.bean.api.circle;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;

/**
 * 评论的model
 */
public class CommentModel {

    @Expose
    private String friendCircleId;//朋友圈Id

    @Expose
    private String content;//评论内容


    public String getFriendCircleId() {
        return friendCircleId;
    }

    public void setFriendCircleId(String friendCircleId) {
        this.friendCircleId = friendCircleId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 检测
     * @param model
     * @return
     */
    public static boolean check(CommentModel model) {
        return model != null &&
                !(Strings.isNullOrEmpty(model.content)
                        || Strings.isNullOrEmpty(model.friendCircleId));
    }
}
