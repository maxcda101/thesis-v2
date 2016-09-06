package models;

import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * Created by AnhQuan on 8/14/2016.
 */
@Entity
public class User extends Model {
    public String email;
    public String password;
    public String name;
    public String address;
    public String phone;
    public boolean isManager;

    public User() {
    }

    public User(String email, String password, String name, String address, String phone, boolean isManager) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.isManager = isManager;
    }
}
