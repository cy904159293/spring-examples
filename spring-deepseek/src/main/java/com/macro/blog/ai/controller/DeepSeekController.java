package com.macro.blog.ai.controller;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


/**
 * @auther macrozheng
 * @description 对接DeepSeek后生成回答的Controller
 * @date 2025/2/21
 * @github https://github.com/macrozheng
 */
@RestController
public class DeepSeekController {

    private final OpenAiChatModel chatModel;

    @Autowired
    public DeepSeekController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 根据消息直接输出回答
     */
    @GetMapping("/ai/chat")
    public Map chat(@RequestParam(value = "message") String message) {
        return Map.of("generation", this.chatModel.call(message));
    }

    /**
     * 根据消息采用流式输出，输出回答
     */
    @GetMapping(value = "/ai/chatFlux", produces = MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=UTF-8")
    public Flux<ChatResponse> chatFlux(@RequestParam(value = "message") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }


    @PostMapping(value = "/ai/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map chatWithImage(@RequestParam("file") MultipartFile file, @RequestParam(value = "message") String message) {
        try {
            // 将文件转换为ByteArrayResource
            ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            // 创建UserMessage，包含文本消息和图像资源
            UserMessage userMessage = new UserMessage(message);
            // 创建Media对象，设置媒体类型和资源
            MimeType mimeType = MimeTypeUtils.parseMimeType(file.getContentType());
            Media media = new Media(mimeType, imageResource);

            // 创建Prompt并发送到chatModel
            Prompt prompt = new Prompt(userMessage);
            return Map.of("generation", this.chatModel.call(prompt));
        } catch (Exception e) {
            // 返回更详细的错误信息
            return Map.of("error", e.getMessage());
        }
    }

}
