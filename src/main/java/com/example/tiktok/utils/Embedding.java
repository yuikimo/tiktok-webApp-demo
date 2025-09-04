package com.example.tiktok.utils;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.List;

public class Embedding {

    public static List<TextEmbeddingResultItem> basicCall(List<String> names) throws ApiException, NoApiKeyException {
        TextEmbeddingParam param = TextEmbeddingParam
                .builder()
                .model(TextEmbedding.Models.TEXT_EMBEDDING_V1)
                .texts(names)
                .build();
        TextEmbedding textEmbedding = new TextEmbedding();
        TextEmbeddingResult result = textEmbedding.call(param);
        return result.getOutput().getEmbeddings();
    }
}
