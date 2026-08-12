package com.dawn.ai.controller;

import com.dawn.ai.service.AiAgentService;
import com.dawn.common.core.dto.response.ResponseObject;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIAgentController {

    private final AiAgentService agent;

    @PostMapping("/chat")
    public ResponseObject<String> chat(@Valid @RequestBody ChatRequest question, HttpSession session) {
        String answer = agent.chat(session.getId(), question.getMessage());
        return ResponseObject.success(answer);
    }
}
