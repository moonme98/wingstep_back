package com.wingstep.service.domain.user;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data              
@NoArgsConstructor  
@AllArgsConstructor 
@Builder            
public class User {

    private String userId;		   // 사용자 ID
    private int levelId;		   // 레벨 ID
    private int avatarId;		   // 아바타 ID
    private String nickname;	   // 닉네임
    private String password;       // 비밀번호
    private LocalDate regDate; 	   // 가입일
    private int exp;			   // 경험치
    private boolean delete;		   // 회원탈퇴 여부
    private Character gender;	   // 성별
    
    private int soloWinCount;      // 개인전 승리 수
    private int teamWinCount;	   // 팀전 승리 수
}
