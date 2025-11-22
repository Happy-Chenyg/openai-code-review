package com.chenyg.middleware.sdk.infrastructure.openai;


import com.chenyg.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.chenyg.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

public interface IOpenAI {

    ChatCompletionSyncResponseDTO completions( ChatCompletionRequestDTO requestDTO) throws Exception;

}
