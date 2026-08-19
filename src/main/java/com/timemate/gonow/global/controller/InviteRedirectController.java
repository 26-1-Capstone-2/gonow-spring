package com.timemate.gonow.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 그룹 초대 링크(카톡 등 인앱 브라우저의 intent:// 우회용)의 외부 노출 URL을 /join으로
 * 깔끔하게 유지하기 위한 포워딩. 실제 페이지는 static/join.html(정적 리소스)이 서빙한다.
 * 스프링 시큐리티는 내부 forward 디스패치에도 다시 필터를 걸기 때문에, forward 대상인
 * "/join.html"도 SecurityConfig의 permitAll에 함께 등록해야 한다.
 */
@Controller
public class InviteRedirectController {

    @GetMapping("/join")
    public String showJoinPage() {
        return "forward:/join.html";
    }
}
