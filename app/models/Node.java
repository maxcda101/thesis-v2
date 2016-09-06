package models;

import play.db.jpa.JPA;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by AnhQuan on 8/10/2016.
 */
@Entity
public class Node extends Model {
    public String name;
    public String description;
    @ManyToOne
    public Root root;

    public Node(String name, String description, Root root) {
        this.name = name;
        this.description = description;
        this.root = root;
    }

    public Node() {

    }

    public static List<Node> getAllNodeByLocation(Long idLocation) {
        EntityManager em = JPA.em();
        String sql = "SELECT DISTINCT * FROM Node join Root on Node.root_id=Root.id join Location on Root.location_id= Location.id where Location.id=" + idLocation;
        Query query = em.createNativeQuery(sql, Node.class);
        List<Node> listNode = query.getResultList();
        return listNode;
    }
}
