package com.auction.server.model;

<<<<<<< HEAD
public interface Entity {
    String getId();
    void setId(String id);
=======
import java.io.Serializable;


public interface Entity<T> extends Serializable {// kế thừa serializable để có thể gửi đối tượng qua socket

    T getId();

    void setId(T id);
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
}