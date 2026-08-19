package com.timemate.gonow.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 그룹 초대 링크(카톡 등 인앱 브라우저의 intent:// 우회용)의 외부 노출 URL을 /join으로
 * 깔끔하게 유지하기 위한 포워딩. 실제 페이지는 static/join.html(정적 리소스)이 서빙한다.
 */
@Controller
public class InviteRedirectController {

    @GetMapping("/join")
    public String showJoinPage() {
        return "forward:/join.html";
    }
}
