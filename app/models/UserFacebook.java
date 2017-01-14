
package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class UserFacebook extends Model {

    public long uid;
    public String access_token;

    public UserFacebook(long uid) {
        this.uid = uid;
    }

    public static UserFacebook get(long id) {
        return find("uid", id).first();
    }

    public static UserFacebook createNew() {
        long uid = (long)Math.floor(Math.random() * 10000);
        UserFacebook user = new UserFacebook(uid);
        user.create();
        return user;
    }

}