package com.distributed.inventory.service;

import com.distributed.inventory.dto.LoginRequest;
import com.distributed.inventory.dto.LoginResponse;
import com.distributed.inventory.dto.RegisterRequest;

public interface UserService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
