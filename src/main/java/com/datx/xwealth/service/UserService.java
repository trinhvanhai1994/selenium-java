package com.datx.xwealth.service;

import com.datx.xwealth.repository.middleware.MiddleUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final MiddleUserRepository middleUserRepository;

    public UserService(MiddleUserRepository middleUserRepository) {
        this.middleUserRepository = middleUserRepository;
    }

    public boolean checkMemberIdExits(String memberId) {
        return middleUserRepository.existsById(memberId);
    }
}
