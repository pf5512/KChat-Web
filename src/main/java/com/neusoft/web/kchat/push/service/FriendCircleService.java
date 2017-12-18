package com.neusoft.web.kchat.push.service;

import com.google.common.base.Strings;
import com.neusoft.web.kchat.push.bean.api.base.ResponseModel;
import com.neusoft.web.kchat.push.bean.api.circle.CommentModel;
import com.neusoft.web.kchat.push.bean.api.circle.ReleaseFriendCircleModel;
import com.neusoft.web.kchat.push.bean.card.CommentCard;
import com.neusoft.web.kchat.push.bean.card.FriendCircleCard;
import com.neusoft.web.kchat.push.bean.db.Comment;
import com.neusoft.web.kchat.push.bean.db.Fabulous;
import com.neusoft.web.kchat.push.bean.db.FriendCircle;
import com.neusoft.web.kchat.push.bean.db.User;
import com.neusoft.web.kchat.push.factory.FriendCircleFactory;
import com.neusoft.web.kchat.push.factory.PushFactory;
import com.neusoft.web.kchat.push.utils.Hib;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/friend")
public class FriendCircleService extends BaseService {
    // 发布一个朋友圈（这个接口类似于创建群）
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<FriendCircleCard> release(ReleaseFriendCircleModel model) {

        if (!ReleaseFriendCircleModel.check(model)) {
            return ResponseModel.buildParameterError();
        }
        //得到发布者
        User release = getSelf();
        FriendCircle friendCircle = new FriendCircle(release, model);
        //保存到数据库
        Hib.query(session ->
                session.save(friendCircle)
        );
        return ResponseModel.buildOk(new FriendCircleCard(friendCircle));
    }

    // 获取自己的朋友圈
    @GET
    @Path("/list")//127.0.0.1/api/friend/list......
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<FriendCircleCard>> list() {
        List<FriendCircle> friendCircles = FriendCircleFactory.list();
        if (friendCircles != null && friendCircles.size() > 0) {
            //这句代码是查询到所有的点赞状态然后返回到朋友圈动态数据中
            List<FriendCircleCard> friendCircleCards
                    = friendCircles.stream().map(fc ->
                    new FriendCircleCard(fc, FriendCircleFactory
                            .getFabulousState(getSelf().getId(), fc.getId())))
                    .collect(Collectors.toList());
            return ResponseModel.buildOk(friendCircleCards);
        }
        return ResponseModel.buildOk();
    }

    // 获取评论内容
    @GET
    @Path("/comments/{friendCircleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<CommentCard>> comments(@PathParam("friendCircleId") String friendCircleId) {
        if (Strings.isNullOrEmpty(friendCircleId)) {
            //如果传过来的id是空的那么什么也不返回
            return ResponseModel.buildOk();
        }
        FriendCircle friendCircle = FriendCircleFactory.findById(friendCircleId);
        if (friendCircle == null)
            return ResponseModel.buildOk();
        List<Comment> comments = FriendCircleFactory.getComments(friendCircle);
        if (comments == null || comments.size() == 0)
            return ResponseModel.buildOk();
        List<CommentCard> commentCards = comments.stream()
                .map(CommentCard::new)//转换操作
                .collect(Collectors.toList());

        return ResponseModel.buildOk(commentCards);
    }

    // 评论
    @POST
    @Path("/comment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<CommentCard> comment(CommentModel model) {
        if (!CommentModel.check(model)) {
            return ResponseModel.buildParameterError();
        }
        FriendCircle friendCircle = FriendCircleFactory.findById(model.getFriendCircleId());
        if (friendCircle == null)
            return ResponseModel.buildParameterError();
        User self = getSelf();
        //如果发布者不是评论者本人的情况下，要推送给发送者
        if (!self.getId().equalsIgnoreCase(friendCircle.getReleaseId())) {
            PushFactory.pushFriendCircle(friendCircle.getRelease());
        }
        Comment comment = new Comment(friendCircle, self, model.getContent());
        //保存到数据库
        Hib.queryOnly(session ->
                session.save(comment)
        );
        return ResponseModel.buildOk(new CommentCard(comment));
    }

    // 点赞与取消点赞
    @POST
    @Path("/fabulous/{friendCircleId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<FriendCircleCard> fabulous( @PathParam("friendCircleId") String friendCircleId) {
        if (Strings.isNullOrEmpty(friendCircleId))
            //如果传过来的id是空的那么就是参数错误
            return ResponseModel.buildParameterError();
        FriendCircle friendCircle = FriendCircleFactory.findById(friendCircleId);
        if (friendCircle == null)
            //如果找不到这个朋友圈的内容，那么就是参数错误
            return ResponseModel.buildParameterError();
        User self = getSelf();
        Fabulous fabulous;
        //如果没有点赞表的话，我们要创建一个点赞表，有的话只要改变他的点赞状态就行了
        if (FriendCircleFactory.getFabulous(self.getId(), friendCircleId) == null) {
            fabulous = new Fabulous(self, friendCircle, true);
            Hib.query(session ->
                    session.save(fabulous)
            );
        } else {
            //反向定论，如果是点赞了就设置为未点赞，没点赞就设置为点赞
            fabulous = FriendCircleFactory.getFabulous(self.getId(), friendCircleId);
            fabulous.setFabulous(!fabulous.isFabulous());
            Hib.queryOnly(session ->
                    session.saveOrUpdate(fabulous));
        }
        //设置点赞状态，然后返回
        return ResponseModel.buildOk(new FriendCircleCard(friendCircle, fabulous.isFabulous()));
    }
}
