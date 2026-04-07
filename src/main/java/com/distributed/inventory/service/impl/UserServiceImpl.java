package com.distributed.inventory.service.impl;

import com.distributed.inventory.dto.LoginRequest;
import com.distributed.inventory.dto.LoginResponse;
import com.distributed.inventory.dto.RegisterRequest;
import com.distributed.inventory.entity.User;
import com.distributed.inventory.mapper.UserMapper;
import com.distributed.inventory.service.UserService;
import com.distributed.inventory.util.JwtUtil;
import com.distributed.inventory.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        User existUser = userMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(MD5Util.encrypt(request.getPassword()));
        user.setPhone(request.getPhone());
        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String encryptedPassword = MD5Util.encrypt(request.getPassword());
        if (!StringUtils.equals(encryptedPassword, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }
}
