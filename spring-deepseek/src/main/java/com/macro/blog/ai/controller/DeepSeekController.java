package com.macro.blog.ai.controller;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;
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


    /**
     * 根据消息直接输出回答，文件直接用本地的
     * 需要有多模态能力的模型才能得到返回信息，例如：通义千问VL
     */
    @GetMapping("/ai/chatVQALocal")
    public Map chatVQALocal(@RequestParam(value = "message") String message) {
        try {
            // 使用ClassPathResource来获取资源文件
            Resource resource = new ClassPathResource("test-image.jpg");
            byte[] imageBytes = StreamUtils.copyToByteArray(resource.getInputStream());

            // 创建ByteArrayResource
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "test-image.jpg";
                }
            };

            // 创建UserMessage和Media对象
            UserMessage userMessage = new UserMessage(message);
            Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);
            userMessage.getMedia().add(media);

            // 创建包含图片的Prompt并发送到chatModel
            Prompt prompt = new Prompt(userMessage);
            return Map.of("generation", this.chatModel.call(prompt));
        } catch (IOException e) {
            return Map.of("error", "读取图片文件失败: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 根据消息直接输出回答，文件上传实现
     * 需要有多模态能力的模型才能得到返回信息，例如：通义千问VL
     */
    @PostMapping(value = "/ai/chatVQA", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map chatVQA(@RequestParam("file") MultipartFile file, @RequestParam(value = "message") String message) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Map.of("error", "不支持的文件类型，请上传图片文件");
            }
            
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
            userMessage.getMedia().add(media);

            // 创建包含图片的Prompt并发送到chatModel
            Prompt prompt = new Prompt(userMessage);
            return Map.of("generation", this.chatModel.call(prompt));
        } catch (IOException e) {
            e.printStackTrace(); // 打印详细堆栈信息
            return Map.of("error", "读取图片文件失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细堆栈信息
            return Map.of("error", e.getMessage());
        }
    }

}
