package com.neusoft.web.kchat.push.service;

import com.google.common.base.Strings;
import com.neusoft.web.kchat.push.bean.api.base.PushModel;
import com.neusoft.web.kchat.push.bean.api.base.ResponseModel;
import com.neusoft.web.kchat.push.bean.api.user.UpdateInfoModel;
import com.neusoft.web.kchat.push.bean.card.UserCard;
import com.neusoft.web.kchat.push.bean.db.User;
import com.neusoft.web.kchat.push.factory.PushFactory;
import com.neusoft.web.kchat.push.factory.UserFactory;
import com.neusoft.web.kchat.push.utils.PushDispatcher;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息处理的Service
 *
 */
// 127.0.0.1/api/user/...
@Path("/user")
public class UserService extends BaseService {

    // 用户信息修改接口
    // 返回自己的个人信息
    @PUT
    //@Path("") //127.0.0.1/api/user 不需要写，就是当前目录
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model) {
        if (!UpdateInfoModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        // 更新用户信息
        self = model.updateToUser(self);
        self = UserFactory.update(self);
        // 构架自己的用户信息
        UserCard card = new UserCard(self, true);
        // 返回
        return ResponseModel.buildOk(card);
    }

    // 拉取联系人
    @GET
    @Path("/contact") //127.0.0.1/api/user 不需要写，就是当前目录
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> contact(){
        User self = getSelf();

        // 拿到联系人
        List<User> users = UserFactory.contacts(self);
        // 转换为UserCard
        List<UserCard> userCards = users.stream()
                .map((user) -> new UserCard(user, true))
                .collect(Collectors.toList());
        return ResponseModel.buildOk(userCards);
    }

    // 关注人
    // 简化:双方同时关注
    @PUT // 修改类使用Put
    @Path("/follow/{followId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> follow(@PathParam("followId") String followId){
        User self = getSelf();
        if (self.getId().equalsIgnoreCase(followId)
                || Strings.isNullOrEmpty(followId)){
            return ResponseModel.buildParameterError();

        }
        User followUser = UserFactory.findById(followId);

        if (followUser == null ){
            return ResponseModel.buildNotFoundUserError(null);
        }
        // 备注默认为null
        followUser = UserFactory.follow(self, followUser, null);
        if (followUser == null){
            return ResponseModel.buildServiceError();
        }
        // 通知关注的人，有人关注了
        PushFactory.pushFollow(followUser, new UserCard(self));

        // 返回关注人的信息
        return ResponseModel.buildOk(new UserCard(followUser, true));
    }

    // 获取某人的信息
    @PUT // 修改类使用Put
    @Path("/{id}") // http://127.0.0.1/api/user/{id}
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> getUser(@PathParam("id") String id){
        if (Strings.isNullOrEmpty(id)){
            // 返回参数异常
            return ResponseModel.buildNotFoundUserError(null);
        }

        User self = getSelf();
        if (self.getId().equalsIgnoreCase(id)){
            // 返回自己，不必查询数据库
            return ResponseModel.buildOk(new UserCard(self, true));
        }

        User user = UserFactory.findById(id);
        if (user == null){
            // 没找到，返回没找到用户
            return ResponseModel.buildNotFoundUserError(null);
        }

        // 如果有记录，则我已关注需要查询信息的用户
        boolean isFollow = UserFactory.getUserFollow(self, user) != null;

        return ResponseModel.buildOk(new UserCard(user, isFollow));
    }

    // 搜索人的接口实现
    // 为了简化分页，只返回20条数据
    @GET // 不涉及数据更改
    //127.0.0.1/api/user/search
    @Path("/search/{name:(.*)?}") // 名字为任意字符，可以为null
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard>> search(@DefaultValue("") @PathParam("name") String name){
        User self = getSelf();
        // 先查询数据
        List<User> searchUsers = UserFactory.search(name);
        // 查询的人封装为UserCard
        // 判断是否有已关注的人
        // 若有，则返回的关注状态中应该已经设置好状态
        // 拿出关注列表
        List<User> contacts = UserFactory.contacts(self);
        // 把User->UserCard
        List<UserCard> userCards = searchUsers.stream()
                .map(user -> {
                    // 判断查询的用户是否自己,或者在联系人列表中
                    // anyMatch 任意匹配 , 匹配ID字段
                    boolean isFollow = user.getId().equalsIgnoreCase(self.getId())
                            || contacts.stream().anyMatch(
                                    contactUser -> contactUser.getId().equalsIgnoreCase(user.getId())
                    );

                    return new UserCard(user, isFollow);
                }).collect(Collectors.toList());
        // 返回
        return ResponseModel.buildOk(userCards);
    }
}
