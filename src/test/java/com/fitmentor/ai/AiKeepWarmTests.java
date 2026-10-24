package com.fitmentor.ai;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiKeepWarmTests {

    @Test
    void pingAsksTheAiServiceToWakeUp() {
        AiServiceClient ai = mock(AiServiceClient.class);

        new AiKeepWarm(ai).ping();

        verify(ai).warmUp();
    }
}
