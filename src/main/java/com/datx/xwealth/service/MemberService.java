//package com.datx.xwealth.service;
//
//import com.datx.xwealth.entity.middleware.Members;
//import com.datx.xwealth.repository.MemberRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class MemberService {
//    private final MemberRepository memberRepository;
//
//    MemberService(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;
//    }
//
//    public void getMemberInfo() {
//        List<Members> all = memberRepository.findAll();
//        System.out.println(all);
//    }
//
//    public boolean checkMemberIdExits(String memberId) {
//        return memberRepository.existsById(memberId);
//    }
//}
