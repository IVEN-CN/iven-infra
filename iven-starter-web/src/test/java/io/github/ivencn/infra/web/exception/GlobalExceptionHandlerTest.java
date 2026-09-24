package io.github.ivencn.infra.web.exception;

import io.github.ivencn.infra.core.exception.BizException;
import io.github.ivencn.infra.web.response.ResponseMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/biz")
        public String biz() {
            throw new BizException(HttpStatus.CONFLICT, "数据冲突", new String[] {"detail"});
        }

        @GetMapping("/boom")
        public String boom() {
            throw new RuntimeException("数据库连接爆炸");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BizException 映射为对应 HTTP 状态码、消息与附加数据")
    void handlesBizException() throws Exception {
        mockMvc.perform(get("/test/biz"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("数据冲突"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("BizException 无附加数据时 data 为 null")
    void handlesBizExceptionWithoutData() {
        BizException ex = new BizException(HttpStatus.NOT_FOUND, "不存在");
        ResponseEntity<ResponseMessage<?>> response = new GlobalExceptionHandler().handleBizException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(404);
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("未捕获异常返回 500 统一结构且不泄漏内部信息")
    void handlesUncaughtException() throws Exception {
        String body = mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).doesNotContain("数据库连接爆炸");
        assertThat(body).doesNotContain("RuntimeException");
    }
}
