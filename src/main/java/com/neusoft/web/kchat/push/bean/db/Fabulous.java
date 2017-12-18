package com.neusoft.web.kchat.push.bean.db;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_FABULOUS")
public class Fabulous {
    @Id
    @PrimaryKeyJoinColumn
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(updatable = false, nullable = false)
    private String id;

    //对应朋友圈中的id
    @JoinColumn(name = "fabulousId")
    @ManyToOne(optional = false, fetch = FetchType.EAGER , cascade = CascadeType.ALL)
    private FriendCircle fabulous;

    @Column(nullable = false, insertable = false, updatable = false)
    private String fabulousId;

    //对应用户的id
    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JoinColumn(name="uid",unique=true)
    private User user;

    //定义为更新时间戳 在创建时就已经写入
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt = LocalDateTime.now();

    //是否点赞
    @Column(nullable = false)
    private boolean isFabulous = false;



    public Fabulous() {
    }

    public Fabulous(User user, FriendCircle fabulous, boolean isFabulous) {
        this.fabulous = fabulous;
        this.user = user;
        this.isFabulous = isFabulous;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FriendCircle getFabulous() {
        return fabulous;
    }

    public void setFabulous(FriendCircle fabulous) {
        this.fabulous = fabulous;
    }

    public String getFabulousId() {
        return fabulousId;
    }

    public void setFabulousId(String fabulousId) {
        this.fabulousId = fabulousId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }

    public boolean isFabulous() {
        return isFabulous;
    }

    public void setFabulous(boolean fabulous) {
        isFabulous = fabulous;
    }
}
