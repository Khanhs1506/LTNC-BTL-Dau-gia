package com.auction.server.model;

import java.io.Serializable;


public interface Entity<T> extends Serializable {// kế thừa serializable để có thể gửi đối tượng qua socket

    T getId();

    void setId(T id);
}