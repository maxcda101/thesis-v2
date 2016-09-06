package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Created by AnhQuan on 9/6/2016.
 */
@Entity
public class Authentication extends Model {
    @ManyToOne
    public User userCreate;
    public Date timeCreate;
    public Date timeExpired;
    public String token;
}
