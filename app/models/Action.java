package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Created by AnhQuan on 8/14/2016.
 */
@Entity
public class Action extends Model {
    @ManyToOne
    public User user;
    public String action;
    public Date time;

    public Action() {
    }

    public Action(User user, String action, Date time) {

        this.user = user;
        this.action = action;
        this.time = time;
    }
}
